package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LiveVoiceManager
import com.example.audio.VoiceState
import com.example.bridge.ActionBridge
import com.example.data.api.GeminiService
import com.example.data.api.OpenAIService
import com.example.data.api.ToolCallRequest
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.model.AIModelCatalog
import com.example.data.pref.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class UiState(
    val isLoading: Boolean = false,
    val isExecutingTool: Boolean = false,
    val currentToolName: String? = null,
    val errorMessage: String? = null,
    val showApiKeyDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showHistorySheet: Boolean = false,
    val isLiveVoiceOverlayOpen: Boolean = false,
    val isValidatingApiKey: Boolean = false,
    val validationFeedback: String? = null,
    val speakingMessageId: String? = null
)

class AiraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val conversationDao = db.conversationDao()
    val preferences = PreferencesManager(application)
    val actionBridge = ActionBridge(application)
    val liveVoiceManager = LiveVoiceManager(application)

    private val geminiService = GeminiService()
    private val openAiService = OpenAIService()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    val allConversations: StateFlow<List<ConversationEntity>> =
        conversationDao.getAllConversations().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private var messageCollectionJob: Job? = null

    init {
        // Check first launch
        val geminiKey = preferences.getEffectiveGeminiApiKey()
        val openAiKey = preferences.getOpenAiApiKey()
        if (preferences.isFirstLaunch() && geminiKey.isBlank() && openAiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(showApiKeyDialog = true)
        }

        // Initialize or load latest conversation
        viewModelScope.launch {
            allConversations.collect { list ->
                if (_activeConversationId.value == null && list.isNotEmpty()) {
                    selectConversation(list.first().id)
                } else if (_activeConversationId.value == null && list.isEmpty()) {
                    createNewConversation()
                }
            }
        }
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            conversationDao.getMessagesForConversation(id).collect { messages ->
                _currentMessages.value = messages
            }
        }
    }

    fun createNewConversation(
        provider: String = preferences.getSelectedProvider(),
        modelId: String = if (provider == PreferencesManager.PROVIDER_OPENAI) preferences.getSelectedOpenAiModel() else preferences.getSelectedGeminiModel()
    ) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newConv = ConversationEntity(
                id = newId,
                title = "New Chat",
                provider = provider,
                modelId = modelId
            )
            conversationDao.insertConversation(newConv)
            selectConversation(newId)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            conversationDao.renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.deleteConversation(id)
            if (_activeConversationId.value == id) {
                val remaining = allConversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first().id)
                } else {
                    createNewConversation()
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            conversationDao.clearAllConversations()
            createNewConversation()
            _uiState.value = _uiState.value.copy(showSettingsSheet = false)
        }
    }

    fun sendMessage(userText: String) {
        val cleanText = userText.trim()
        if (cleanText.isBlank()) return

        val convId = _activeConversationId.value ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Save user message
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = cleanText
            )
            conversationDao.insertMessage(userMsg)

            // Auto-generate title if this is the first user message
            val existing = conversationDao.getMessagesSnapshot(convId)
            if (existing.size <= 2) {
                val title = if (cleanText.length > 28) cleanText.take(28) + "..." else cleanText
                conversationDao.renameConversation(convId, title)
            } else {
                conversationDao.touchConversation(convId)
            }

            dispatchToAI(convId)
        }
    }

    private suspend fun dispatchToAI(
        convId: String,
        pendingToolCall: ToolCallRequest? = null,
        pendingToolResult: String? = null
    ) {
        val provider = preferences.getSelectedProvider()
        val messages = conversationDao.getMessagesSnapshot(convId)
        val history = messages.map { it.role to it.content }

        if (provider == PreferencesManager.PROVIDER_OPENAI) {
            val apiKey = preferences.getOpenAiApiKey()
            val modelId = preferences.getSelectedOpenAiModel()

            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "OpenAI API key missing. Please configure your key in Settings."
                )
                return
            }

            val result = openAiService.generateContent(
                apiKey = apiKey,
                modelId = modelId,
                conversationHistory = history,
                pendingToolCall = pendingToolCall,
                pendingToolResult = pendingToolResult
            )

            result.onSuccess { response ->
                handleAIResponse(convId, response, isGemini = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to generate response."
                )
            }
        } else {
            // Gemini Provider
            val apiKey = preferences.getEffectiveGeminiApiKey()
            val modelId = preferences.getSelectedGeminiModel()

            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gemini API key missing. Please configure your key in Settings."
                )
                return
            }

            val toolResultTuple = if (pendingToolCall != null && pendingToolResult != null) {
                pendingToolCall.name to pendingToolResult
            } else null

            val result = geminiService.generateContent(
                apiKey = apiKey,
                modelId = modelId,
                conversationHistory = history,
                pendingToolResult = toolResultTuple
            )

            result.onSuccess { response ->
                handleAIResponse(convId, response, isGemini = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to generate response."
                )
            }
        }
    }

    private suspend fun handleAIResponse(
        convId: String,
        response: com.example.data.api.AIResponse,
        isGemini: Boolean
    ) {
        val toolCall = response.toolCall

        if (toolCall != null) {
            // Execute device tool action
            _uiState.value = _uiState.value.copy(
                isExecutingTool = true,
                currentToolName = toolCall.name
            )
            liveVoiceManager.setVoiceState(VoiceState.EXECUTING_ACTION, "Executing action: ${toolCall.name}...")

            val executionResult = actionBridge.executeTool(toolCall.name, toolCall.argsJson)

            // Save Tool Execution Message
            val toolMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "assistant",
                content = if (response.text.isNotBlank()) response.text else "Action executed: ${toolCall.name}",
                toolName = toolCall.name,
                toolArgs = toolCall.argsJson,
                toolResult = executionResult.message
            )
            conversationDao.insertMessage(toolMsg)

            _uiState.value = _uiState.value.copy(
                isExecutingTool = false,
                currentToolName = null
            )

            // Continue conversation loop with tool result to get final response
            dispatchToAI(convId, pendingToolCall = toolCall, pendingToolResult = executionResult.message)
        } else {
            // Standard text response
            val assistantMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "assistant",
                content = response.text
            )
            conversationDao.insertMessage(assistantMsg)
            conversationDao.touchConversation(convId)

            _uiState.value = _uiState.value.copy(isLoading = false)

            // Handle voice playback ONLY via Gemini Live native audio pipeline
            if (!response.audioBase64.isNullOrBlank()) {
                liveVoiceManager.playAudioBase64(response.audioBase64)
            }
        }
    }

    /**
     * Sends user voice input directly to Gemini Live Session
     */
    fun sendLiveVoiceTurn(inputWavBytes: ByteArray?, recognizedText: String) {
        val convId = _activeConversationId.value ?: return

        val displayText = when {
            recognizedText.isNotBlank() -> recognizedText
            inputWavBytes != null && inputWavBytes.isNotEmpty() -> "🎙️ Voice input"
            else -> return
        }

        viewModelScope.launch {
            liveVoiceManager.setVoiceState(VoiceState.THINKING, "Arushi is thinking...")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Save user message
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = displayText
            )
            conversationDao.insertMessage(userMsg)

            // Auto-generate title if this is the first user message
            val existing = conversationDao.getMessagesSnapshot(convId)
            if (existing.size <= 2) {
                val title = if (displayText.length > 28) displayText.take(28) + "..." else displayText
                conversationDao.renameConversation(convId, title)
            } else {
                conversationDao.touchConversation(convId)
            }

            dispatchToGeminiLive(
                convId = convId,
                inputWavBytes = inputWavBytes,
                inputText = if (recognizedText.isNotBlank()) recognizedText else null
            )
        }
    }

    private suspend fun dispatchToGeminiLive(
        convId: String,
        inputWavBytes: ByteArray? = null,
        inputText: String? = null,
        pendingToolCall: ToolCallRequest? = null,
        pendingToolResult: String? = null
    ) {
        val apiKey = preferences.getEffectiveGeminiApiKey()
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Gemini API key missing. Please configure your key in Settings."
            )
            liveVoiceManager.setVoiceState(VoiceState.ERROR, "Gemini API key is required for Live Voice.")
            return
        }

        val messages = conversationDao.getMessagesSnapshot(convId)
        val history = messages.map { it.role to it.content }

        val toolResultTuple = if (pendingToolCall != null && pendingToolResult != null) {
            pendingToolCall.name to pendingToolResult
        } else null

        val result = geminiService.generateLiveVoiceContent(
            apiKey = apiKey,
            conversationHistory = history,
            inputAudioBytes = inputWavBytes,
            inputText = inputText,
            pendingToolResult = toolResultTuple
        )

        result.onSuccess { response ->
            handleGeminiLiveResponse(convId, response)
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "Failed to generate live voice response."
            )
            liveVoiceManager.setVoiceState(VoiceState.ERROR, error.message ?: "Voice connection failed")
        }
    }

    private suspend fun handleGeminiLiveResponse(
        convId: String,
        response: com.example.data.api.AIResponse
    ) {
        val toolCall = response.toolCall

        if (toolCall != null) {
            _uiState.value = _uiState.value.copy(
                isExecutingTool = true,
                currentToolName = toolCall.name
            )
            liveVoiceManager.setVoiceState(VoiceState.EXECUTING_ACTION, "Executing action: ${toolCall.name}...")

            val executionResult = actionBridge.executeTool(toolCall.name, toolCall.argsJson)

            val toolMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "assistant",
                content = if (response.text.isNotBlank()) response.text else "Action executed: ${toolCall.name}",
                toolName = toolCall.name,
                toolArgs = toolCall.argsJson,
                toolResult = executionResult.message
            )
            conversationDao.insertMessage(toolMsg)

            _uiState.value = _uiState.value.copy(
                isExecutingTool = false,
                currentToolName = null
            )

            // Continue loop with tool result to get spoken confirmation from Gemini Live
            dispatchToGeminiLive(
                convId = convId,
                pendingToolCall = toolCall,
                pendingToolResult = executionResult.message
            )
        } else {
            val assistantMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "assistant",
                content = response.text
            )
            conversationDao.insertMessage(assistantMsg)
            conversationDao.touchConversation(convId)

            _uiState.value = _uiState.value.copy(isLoading = false)

            if (!response.audioBase64.isNullOrBlank()) {
                liveVoiceManager.playAudioBase64(response.audioBase64)
            } else {
                liveVoiceManager.setVoiceState(VoiceState.IDLE, "Ready")
            }
        }
    }

    fun stopSpeaking() {
        liveVoiceManager.interrupt()
        _uiState.value = _uiState.value.copy(speakingMessageId = null)
    }

    fun openLiveVoiceOverlay() {
        _uiState.value = _uiState.value.copy(isLiveVoiceOverlayOpen = true)
        liveVoiceManager.startListening()
    }

    fun closeLiveVoiceOverlay() {
        liveVoiceManager.interrupt()
        _uiState.value = _uiState.value.copy(isLiveVoiceOverlayOpen = false)
    }

    fun toggleVoiceMic() {
        val currentState = liveVoiceManager.voiceState.value
        if (currentState == VoiceState.LISTENING) {
            val recognized = liveVoiceManager.recognizedSpeechText.value.trim()
            val wavBytes = liveVoiceManager.stopListeningAndGetWav()
            sendLiveVoiceTurn(if (wavBytes.isNotEmpty()) wavBytes else null, recognized)
        } else if (currentState == VoiceState.SPEAKING) {
            liveVoiceManager.interrupt()
            liveVoiceManager.startListening()
        } else {
            liveVoiceManager.startListening()
        }
    }

    fun interruptVoice() {
        liveVoiceManager.interrupt()
        stopSpeaking()
    }

    // Settings & Dialog controls
    fun openSettings() { _uiState.value = _uiState.value.copy(showSettingsSheet = true) }
    fun closeSettings() { _uiState.value = _uiState.value.copy(showSettingsSheet = false) }

    fun openHistory() { _uiState.value = _uiState.value.copy(showHistorySheet = true) }
    fun closeHistory() { _uiState.value = _uiState.value.copy(showHistorySheet = false) }

    fun openApiKeyDialog() { _uiState.value = _uiState.value.copy(showApiKeyDialog = true) }
    fun closeApiKeyDialog() {
        preferences.setFirstLaunchCompleted()
        _uiState.value = _uiState.value.copy(showApiKeyDialog = false)
    }

    fun saveApiKeys(geminiKey: String, openAiKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidatingApiKey = true, validationFeedback = null)
            preferences.setGeminiApiKey(geminiKey)
            preferences.setOpenAiApiKey(openAiKey)
            preferences.setFirstLaunchCompleted()
            _uiState.value = _uiState.value.copy(
                isValidatingApiKey = false,
                showApiKeyDialog = false,
                validationFeedback = "API Keys saved successfully."
            )
        }
    }

    fun validateGeminiKey(key: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidatingApiKey = true, validationFeedback = "Testing Gemini key...")
            val result = geminiService.validateApiKey(key)
            result.onSuccess {
                preferences.setGeminiApiKey(key)
                _uiState.value = _uiState.value.copy(
                    isValidatingApiKey = false,
                    validationFeedback = "✓ Gemini API Key verified successfully!"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isValidatingApiKey = false,
                    validationFeedback = "✕ Validation failed: ${error.message}"
                )
            }
        }
    }

    fun validateOpenAiKey(key: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidatingApiKey = true, validationFeedback = "Testing OpenAI key...")
            val result = openAiService.validateApiKey(key)
            result.onSuccess {
                preferences.setOpenAiApiKey(key)
                _uiState.value = _uiState.value.copy(
                    isValidatingApiKey = false,
                    validationFeedback = "✓ OpenAI API Key verified successfully!"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isValidatingApiKey = false,
                    validationFeedback = "✕ Validation failed: ${error.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveVoiceManager.release()
    }
}
