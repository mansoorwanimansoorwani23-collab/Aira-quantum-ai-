package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING_ACTION,
    ERROR
}

class LiveVoiceManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedSpeechText = MutableStateFlow("")
    val recognizedSpeechText: StateFlow<String> = _recognizedSpeechText.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private var playbackJob: Job? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private val pcmLock = Any()
    private val recordedPcmStream = ByteArrayOutputStream()

    // Default 24kHz for Gemini Live Audio (Gemini Native Audio outputs 24kHz 16-bit Mono PCM)
    private val defaultSampleRate = 24000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _statusText = MutableStateFlow("Tap the microphone to speak with Quantum AI")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun setVoiceState(state: VoiceState, status: String? = null) {
        _voiceState.value = state
        if (status != null) {
            _statusText.value = status
        }
    }

    fun startListening(onAmplitudeUpdate: ((Float) -> Unit)? = null) {
        stopPlayback()
        _recognizedSpeechText.value = ""

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _voiceState.value = VoiceState.ERROR
            _statusText.value = "Microphone permission is required for voice interaction."
            return
        }

        // Start speech recognizer on main thread for real-time partial display if available
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {}
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    _recognizedSpeechText.value = matches[0]
                                    _statusText.value = "Heard: \"${matches[0]}\""
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    _recognizedSpeechText.value = matches[0]
                                    _statusText.value = "Hearing: \"${matches[0]}\""
                                }
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    speechRecognizer?.startListening(recognizerIntent)
                }
            } catch (_: Exception) {}
        }

        try {
            val recordSampleRate = 16000
            val minBufSize = AudioRecord.getMinBufferSize(
                recordSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                recordSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _voiceState.value = VoiceState.ERROR
                _statusText.value = "AudioRecord initialization failed."
                return
            }

            synchronized(pcmLock) {
                recordedPcmStream.reset()
            }

            audioRecord?.startRecording()
            _voiceState.value = VoiceState.LISTENING
            _statusText.value = "Listening to you in any language (Hindi, English, Hinglish)..."

            recordJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(1024)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        synchronized(pcmLock) {
                            for (i in 0 until read) {
                                sum += buffer[i] * buffer[i]
                                val sample = buffer[i].toInt()
                                recordedPcmStream.write(sample and 0xFF)
                                recordedPcmStream.write((sample shr 8) and 0xFF)
                            }
                        }
                        val rms = sqrt(sum / read)
                        val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized
                        onAmplitudeUpdate?.invoke(normalized)
                    }
                }
            }
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
            _statusText.value = "Failed to start microphone: ${e.message}"
        }
    }

    fun stopListeningAndGetPcm(): ByteArray {
        stopSpeechRecognizer()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordJob?.cancel()
        } catch (_: Exception) {}
        _audioAmplitude.value = 0f
        return synchronized(pcmLock) {
            recordedPcmStream.toByteArray()
        }
    }

    fun stopListeningAndGetWav(): ByteArray {
        val pcmBytes = stopListeningAndGetPcm()
        return if (pcmBytes.isNotEmpty()) pcmToWav(pcmBytes, 16000, 1) else ByteArray(0)
    }

    private fun stopSpeechRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }

    fun playAudioBase64(base64Audio: String, onFinished: (() -> Unit)? = null) {
        stopPlayback()
        if (base64Audio.isBlank()) {
            onFinished?.invoke()
            return
        }

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                val rawBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                _voiceState.value = VoiceState.SPEAKING
                _statusText.value = "Quantum AI is speaking..."

                var playBytes = rawBytes
                var sampleRate = defaultSampleRate

                // Check for RIFF WAV header (44 bytes)
                if (rawBytes.size > 44 &&
                    rawBytes[0] == 'R'.code.toByte() &&
                    rawBytes[1] == 'I'.code.toByte() &&
                    rawBytes[2] == 'F'.code.toByte() &&
                    rawBytes[3] == 'F'.code.toByte()
                ) {
                    try {
                        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
                        val extractedSampleRate = buffer.getInt(24)
                        if (extractedSampleRate in 8000..48000) {
                            sampleRate = extractedSampleRate
                        }
                        playBytes = rawBytes.copyOfRange(44, rawBytes.size)
                    } catch (_: Exception) {
                        playBytes = rawBytes.copyOfRange(44, rawBytes.size)
                    }
                }

                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    channelConfig,
                    audioFormat
                )
                val bufferSize = (minBufSize * 4).coerceAtLeast(8192)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val chunkSize = 2048
                var offset = 0
                while (isActive && offset < playBytes.size && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    val count = (playBytes.size - offset).coerceAtMost(chunkSize)
                    val written = audioTrack?.write(playBytes, offset, count, AudioTrack.WRITE_BLOCKING) ?: 0
                    if (written <= 0) break

                    var maxAmp = 0
                    var i = offset
                    while (i < offset + count - 1) {
                        val sample = (playBytes[i].toInt() and 0xFF) or (playBytes[i + 1].toInt() shl 8)
                        val absVal = abs(sample)
                        if (absVal > maxAmp) maxAmp = absVal
                        i += 2
                    }
                    val normalizedAmp = (maxAmp / 32768f).coerceIn(0.05f, 1f)
                    _audioAmplitude.value = normalizedAmp

                    offset += count
                }

                _audioAmplitude.value = 0f
                _voiceState.value = VoiceState.IDLE
                _statusText.value = "Tap the microphone to speak"
                onFinished?.invoke()
            } catch (e: Exception) {
                _voiceState.value = VoiceState.IDLE
                _statusText.value = "Ready"
                onFinished?.invoke()
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                    audioTrack = null
                } catch (_: Exception) {}
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
        _audioAmplitude.value = 0f
    }

    fun interrupt() {
        stopPlayback()
        stopSpeechRecognizer()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordJob?.cancel()
        } catch (_: Exception) {}
        _voiceState.value = VoiceState.IDLE
        _statusText.value = "Interrupted. Tap microphone to speak."
        _audioAmplitude.value = 0f
    }

    fun release() {
        interrupt()
    }

    private fun pcmToWav(pcmData: ByteArray, sampleRate: Int = 16000, channels: Int = 1): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavData = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, 44)
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)
        return wavData
    }
}

