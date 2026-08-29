package com.example.data.api

import android.util.Base64
import com.example.data.model.AIModelCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AIResponse(
    val text: String,
    val toolCall: ToolCallRequest? = null,
    val audioBase64: String? = null,
    val rawJson: String? = null
)

data class ToolCallRequest(
    val name: String,
    val argsJson: String,
    val callId: String? = null
)

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing."))
        }
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Ping"))
                        })
                    })
                })
            }
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorMsg = parseErrorMessage(responseStr, response.code)
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Failed to connect"}"))
        }
    }

    /**
     * Standard Chat & Intelligence generation using the latest selected Gemini model
     */
    suspend fun generateContent(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Pair<String, String>>, // role ("user", "model", "function"), content
        pendingToolResult: Pair<String, String>? = null // toolName, resultJson
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required. Please set your key in Settings."))
        }

        try {
            val endpointModel = if (modelId.isBlank()) "gemini-2.5-flash" else modelId
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$endpointModel:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            for ((role, content) in conversationHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "assistant" || role == "model") "model" else "user")
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", content))
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            if (pendingToolResult != null) {
                val (toolName, toolResultJson) = pendingToolResult
                val functionResponseObj = JSONObject().apply {
                    put("role", "function")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", toolName)
                                put("response", JSONObject().apply {
                                    put("name", toolName)
                                    put("content", toolResultJson)
                                })
                            })
                        })
                    })
                }
                contentsArray.put(functionResponseObj)
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", AIModelCatalog.AIRA_SYSTEM_INSTRUCTION))
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionDeclarations", getGeminiToolsDeclaration())
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(responseStr, response.code)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                parseCandidatesResponse(responseStr)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gemini request failed: ${e.localizedMessage ?: "Network timeout"}"))
        }
    }

    /**
     * Dedicated Gemini Live Voice Session Pipeline:
     * - Uses Gemini 3.5 Live-compatible voice models (with graceful multi-model fallback)
     * - Supports real-time audio input (WAV/PCM) and produces native 24kHz/PCM audio output directly from Gemini Live
     * - Preserves tool calling and real-time execution
     */
    suspend fun generateLiveVoiceContent(
        apiKey: String,
        conversationHistory: List<Pair<String, String>>,
        inputAudioBytes: ByteArray? = null,
        inputText: String? = null,
        pendingToolResult: Pair<String, String>? = null,
        voiceName: String = "Aoede"
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required."))
        }

        // Live Voice Models Priority (Gemini 3.5 Live-compatible models for voice only)
        val voiceModels = listOf(
            "gemini-3.5-flash",
            "gemini-3.5-flash-preview",
            "gemini-2.5-flash-native-audio-preview-12-2025",
            "gemini-2.0-flash-exp",
            "gemini-2.0-flash",
            "gemini-2.5-flash"
        )

        var lastException: Exception? = null

        for (modelId in voiceModels) {
            try {
                val result = executeLiveVoiceRequest(
                    apiKey = apiKey,
                    modelId = modelId,
                    conversationHistory = conversationHistory,
                    inputAudioBytes = inputAudioBytes,
                    inputText = inputText,
                    pendingToolResult = pendingToolResult,
                    voiceName = voiceName
                )
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Gemini Live Voice Session failed to respond."))
    }

    private fun executeLiveVoiceRequest(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Pair<String, String>>,
        inputAudioBytes: ByteArray?,
        inputText: String?,
        pendingToolResult: Pair<String, String>?,
        voiceName: String
    ): Result<AIResponse> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Prior turns
        for ((role, content) in conversationHistory) {
            val contentObj = JSONObject()
            contentObj.put("role", if (role == "assistant" || role == "model") "model" else "user")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", content))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        // Current turn with Audio and/or Text
        if (inputAudioBytes != null && inputAudioBytes.isNotEmpty()) {
            val userContentObj = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                if (!inputText.isNullOrBlank()) {
                    parts.put(JSONObject().put("text", inputText))
                }
                parts.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "audio/wav")
                        put("data", Base64.encodeToString(inputAudioBytes, Base64.NO_WRAP))
                    })
                })
                put("parts", parts)
            }
            contentsArray.put(userContentObj)
        } else if (!inputText.isNullOrBlank()) {
            val userContentObj = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", inputText))
                put("parts", parts)
            }
            contentsArray.put(userContentObj)
        }

        // Function response if any
        if (pendingToolResult != null) {
            val (toolName, toolResultJson) = pendingToolResult
            val functionResponseObj = JSONObject().apply {
                put("role", "function")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionResponse", JSONObject().apply {
                            put("name", toolName)
                            put("response", JSONObject().apply {
                                put("name", toolName)
                                put("content", toolResultJson)
                            })
                        })
                    })
                })
            }
            contentsArray.put(functionResponseObj)
        }

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", AIModelCatalog.ARUSHI_SYSTEM_INSTRUCTION))
                })
            })
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", getGeminiToolsDeclaration())
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("AUDIO")
                })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseStr, response.code)
                return Result.failure(Exception(errorMsg))
            }
            return parseCandidatesResponse(responseStr)
        }
    }

    private fun parseCandidatesResponse(responseStr: String): Result<AIResponse> {
        val resJson = JSONObject(responseStr)
        val candidates = resJson.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return Result.success(AIResponse(text = "I couldn't generate a response. Please try again."))
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        var responseText = ""
        var audioBase64: String? = null
        var toolCall: ToolCallRequest? = null

        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    responseText += part.getString("text")
                }
                if (part.has("inlineData")) {
                    val inline = part.getJSONObject("inlineData")
                    val mime = inline.optString("mimeType", "")
                    if (mime.startsWith("audio/")) {
                        audioBase64 = if (inline.has("data")) inline.getString("data") else null
                    }
                }
                if (part.has("functionCall")) {
                    val fc = part.getJSONObject("functionCall")
                    val name = fc.getString("name")
                    val args = fc.optJSONObject("args")?.toString() ?: "{}"
                    toolCall = ToolCallRequest(name = name, argsJson = args)
                }
            }
        }

        return Result.success(
            AIResponse(
                text = responseText.trim(),
                toolCall = toolCall,
                audioBase64 = audioBase64,
                rawJson = responseStr
            )
        )
    }

    private fun getGeminiToolsDeclaration(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("name", "openWhatsApp")
                put("description", "Opens the WhatsApp application or web interface on user's device.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })
            put(JSONObject().apply {
                put("name", "openApp")
                put("description", "Opens an application on the user's device, e.g. YouTube, Instagram, Chrome, Camera, Settings, Spotify, Maps, Calculator.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("appName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The name of the application to open, e.g. 'YouTube', 'Instagram', 'Settings', 'Chrome'.")
                        })
                    })
                    put("required", JSONArray().apply { put("appName") })
                })
            })
            put(JSONObject().apply {
                put("name", "openUrl")
                put("description", "Opens a web link or URL in the browser.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("url", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The full web URL to open, e.g. 'https://www.google.com'.")
                        })
                    })
                    put("required", JSONArray().apply { put("url") })
                })
            })
            put(JSONObject().apply {
                put("name", "makeCall")
                put("description", "Initiates or prepares a phone call to a given phone number.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("phoneNumber", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The phone number with optional country code, e.g. '+919876543210'.")
                        })
                    })
                    put("required", JSONArray().apply { put("phoneNumber") })
                })
            })
            put(JSONObject().apply {
                put("name", "callContact")
                put("description", "Finds a contact by name (e.g. Mom, Mummy, Dad, Rahul) in the user's contacts and initiates a phone call.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The name or relation of the contact to call, e.g. 'Mom', 'Mummy', 'Rahul', 'Dad'.")
                        })
                    })
                    put("required", JSONArray().apply { put("contactName") })
                })
            })
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            val message = error?.optString("message")
            if (!message.isNullOrBlank()) {
                "Gemini Error ($statusCode): $message"
            } else {
                "Gemini API Error with status $statusCode."
            }
        } catch (_: Exception) {
            "Gemini request failed (HTTP $statusCode). Please check your API key and network."
        }
    }
}
