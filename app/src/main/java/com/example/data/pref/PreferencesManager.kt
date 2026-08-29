package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aira_ai_prefs", Context.MODE_PRIVATE)

    private val _geminiApiKeyFlow = MutableStateFlow(getEffectiveGeminiApiKey())
    val geminiApiKeyFlow: StateFlow<String> = _geminiApiKeyFlow.asStateFlow()

    private val _openAiApiKeyFlow = MutableStateFlow(getOpenAiApiKey())
    val openAiApiKeyFlow: StateFlow<String> = _openAiApiKeyFlow.asStateFlow()

    private val _selectedProviderFlow = MutableStateFlow(getSelectedProvider())
    val selectedProviderFlow: StateFlow<String> = _selectedProviderFlow.asStateFlow()

    private val _selectedGeminiModelFlow = MutableStateFlow(getSelectedGeminiModel())
    val selectedGeminiModelFlow: StateFlow<String> = _selectedGeminiModelFlow.asStateFlow()

    private val _selectedOpenAiModelFlow = MutableStateFlow(getSelectedOpenAiModel())
    val selectedOpenAiModelFlow: StateFlow<String> = _selectedOpenAiModelFlow.asStateFlow()

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getStoredGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun getEffectiveGeminiApiKey(): String {
        val userKey = getStoredGeminiApiKey()
        if (userKey.isNotBlank()) return userKey
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
        _geminiApiKeyFlow.value = getEffectiveGeminiApiKey()
    }

    fun getOpenAiApiKey(): String {
        return prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
    }

    fun setOpenAiApiKey(key: String) {
        prefs.edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply()
        _openAiApiKeyFlow.value = key.trim()
    }

    fun getSelectedProvider(): String {
        return prefs.getString(KEY_SELECTED_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
    }

    fun setSelectedProvider(provider: String) {
        prefs.edit().putString(KEY_SELECTED_PROVIDER, provider).apply()
        _selectedProviderFlow.value = provider
    }

    fun getSelectedGeminiModel(): String {
        return prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
    }

    fun setSelectedGeminiModel(model: String) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model).apply()
        _selectedGeminiModelFlow.value = model
    }

    fun getSelectedOpenAiModel(): String {
        return prefs.getString(KEY_OPENAI_MODEL, DEFAULT_OPENAI_MODEL) ?: DEFAULT_OPENAI_MODEL
    }

    fun setSelectedOpenAiModel(model: String) {
        prefs.edit().putString(KEY_OPENAI_MODEL, model).apply()
        _selectedOpenAiModelFlow.value = model
    }

    fun isAutoVoiceSpeakEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_VOICE_SPEAK, true)
    }

    fun setAutoVoiceSpeakEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_VOICE_SPEAK, enabled).apply()
    }

    companion object {
        const val KEY_FIRST_LAUNCH = "first_launch_aira"
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
        const val KEY_OPENAI_API_KEY = "openai_api_key"
        const val KEY_SELECTED_PROVIDER = "selected_provider"
        const val KEY_GEMINI_MODEL = "selected_gemini_model"
        const val KEY_OPENAI_MODEL = "selected_openai_model"
        const val KEY_AUTO_VOICE_SPEAK = "auto_voice_speak"

        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OPENAI = "openai"

        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o"
    }
}
