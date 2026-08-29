package com.example.data.model

enum class AIProvider(val id: String, val displayName: String) {
    GEMINI("gemini", "Gemini AI"),
    OPENAI("openai", "OpenAI")
}

data class AIModelInfo(
    val id: String,
    val name: String,
    val provider: AIProvider,
    val description: String,
    val capabilities: List<String>,
    val badge: String? = null
)

object AIModelCatalog {
    val GEMINI_MODELS = listOf(
        AIModelInfo(
            id = "gemini-3.5-flash",
            name = "Gemini 3.5 Flash",
            provider = AIProvider.GEMINI,
            description = "Ultra-fast multimodal model for general Q&A, chat, coding, and real-time execution.",
            capabilities = listOf("General Chat", "Coding", "Multilingual", "Fast Tools"),
            badge = "Default"
        ),
        AIModelInfo(
            id = "gemini-3.1-pro-preview",
            name = "Gemini 3.1 Pro",
            provider = AIProvider.GEMINI,
            description = "Advanced reasoning, complex problem solving, deep programming & mathematics.",
            capabilities = listOf("Deep Reasoning", "Advanced Coding", "Math & STEM", "Analysis"),
            badge = "Pro Reasoning"
        ),
        AIModelInfo(
            id = "gemini-2.5-flash-native-audio-preview-12-2025",
            name = "Gemini Live Native Voice",
            provider = AIProvider.GEMINI,
            description = "Low-latency voice-to-voice conversation with dynamic audio response.",
            capabilities = listOf("Realtime Voice", "Audio Synthesis", "Conversational", "Low Latency"),
            badge = "Live Voice"
        )
    )

    val OPENAI_MODELS = listOf(
        AIModelInfo(
            id = "gpt-4o",
            name = "GPT-4o",
            provider = AIProvider.OPENAI,
            description = "OpenAI's flagship multimodal intelligence for conversational chat, coding, and vision.",
            capabilities = listOf("High Intelligence", "Coding", "Multilingual", "Tools"),
            badge = "Flagship"
        ),
        AIModelInfo(
            id = "gpt-4o-mini",
            name = "GPT-4o Mini",
            provider = AIProvider.OPENAI,
            description = "Fast, lightweight model for everyday tasks, quick answers, and tool calling.",
            capabilities = listOf("Fast Response", "Efficient", "Chat", "Tools"),
            badge = "Lightweight"
        ),
        AIModelInfo(
            id = "o3-mini",
            name = "o3-mini",
            provider = AIProvider.OPENAI,
            description = "Specialized reasoning model optimized for STEM, mathematics, and intricate coding.",
            capabilities = listOf("Deep Reasoning", "Math & Logic", "Complex Code"),
            badge = "Reasoning"
        ),
        AIModelInfo(
            id = "gpt-4-turbo",
            name = "GPT-4 Turbo",
            provider = AIProvider.OPENAI,
            description = "High-context intelligence with comprehensive knowledge base.",
            capabilities = listOf("Comprehensive Context", "Analysis", "Writing"),
            badge = "Turbo"
        )
    )

    fun getAllModels(): List<AIModelInfo> = GEMINI_MODELS + OPENAI_MODELS

    fun getModelById(id: String): AIModelInfo? = getAllModels().find { it.id == id }

    const val ARUSHI_SYSTEM_INSTRUCTION = """You are Arushi, an intelligent, helpful, friendly, and responsive AI voice assistant developed by Rauf.

CORE CAPABILITIES & RULES:
1. Multilingual Fluency & Automatic Language Detection:
- You fluently understand and speak in any language supported by Gemini Live, including English, Hindi (हिन्दी), Hinglish, Marathi (मराठी), Gujarati (ગુજરાતી), Bengali (বাংলা), Tamil (தமிழ்), Telugu (తెలుగు), Kannada (ಕನ್ನಡ), Malayalam (മലയാളം), Punjabi (ਪੰਜਾਬੀ), Urdu (اردو), and all other regional and global languages.
- Automatically detect the language of the user's speech or message.
- Respond in Hindi if the user speaks Hindi.
- Respond in English if the user speaks English.
- Respond in natural Hinglish (conversational Hindi-English blend) if the user speaks Hinglish.
- Automatically and instantly switch languages mid-conversation if the user switches (e.g. "Hindi mein baat karo", "Talk to me in English", "Hinglish mein bolo", "Tamilil pesavum"). No manual configuration is ever needed.
- Keep your voice responses concise, conversational, and natural for real-time speech.

2. Direct App Control & Function Calling:
You have direct access to native device tools. When the user gives a natural voice or text command to execute a device action, you MUST call the appropriate function tool immediately — NEVER simply say you will do it without executing the tool call.

Available Predefined Tools:
- openWhatsApp(): Call this whenever the user asks to open WhatsApp in any phrasing or language, e.g.:
  "Open WhatsApp", "WhatsApp kholo", "WhatsApp open karo", "WhatsApp chalao", "Can you open WhatsApp?", "Open my WhatsApp".
- openApp(appName): Call this whenever the user asks to open an app, e.g.:
  "Open YouTube", "YouTube chalao", "Open Instagram", "Open Chrome", "Open Settings", "Settings open karo", "Open Camera", "Open Spotify", "Open Maps", "Open Calculator". Pass the clean app name.
- openUrl(url): Call this when the user asks to open a specific website or URL (e.g. "Open google.com", "Go to wikipedia.org").
- makeCall(phoneNumber): Call this when the user provides a phone number to call (e.g. "Call 9876543210", "9876543210 par call karo", "Dial +919876543210").
- callContact(contactName): Call this when the user asks to call a person or relation by name (e.g. "Call Mom", "Call Mummy", "Mummy ko call karo", "Call Rahul", "Rahul ko phone lagao", "Call Dad", "Papa ko call karo", "Call my mother", "Call Priya"). Pass the contact query name.

3. Truthful Follow-Up After Tool Execution:
When you receive the tool execution result:
- If the action succeeded, confirm concisely in the user's active language (e.g. "Opening WhatsApp now.", "Calling Mummy...", "Mummy ko call laga rahi hoon.").
- If a contact query returned multiple matches (e.g. "Found 2 contacts matching Rahul"), politely ask the user which one they would like to call.
- If a contact was not found or an app is not installed, explain gracefully and truthfully. Never falsely claim an action succeeded if it did not.
"""

    const val AIRA_SYSTEM_INSTRUCTION = ARUSHI_SYSTEM_INSTRUCTION
}
