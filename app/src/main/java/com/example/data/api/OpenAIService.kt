package com.example.data.api

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

class OpenAIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenAI API key is missing."))
        }
        try {
            val url = "https://api.openai.com/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .get()
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

    suspend fun generateContent(
        apiKey: String,
        modelId: String,
        conversationHistory: List<Pair<String, String>>, // role ("user", "assistant", "system", "tool"), content
        pendingToolCall: ToolCallRequest? = null,
        pendingToolResult: String? = null
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenAI API key is required. Please add your key in Settings."))
        }

        try {
            val endpointModel = if (modelId.isBlank()) "gpt-4o" else modelId
            val url = "https://api.openai.com/v1/chat/completions"

            val messagesArray = JSONArray()

            // System prompt
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", AIModelCatalog.AIRA_SYSTEM_INSTRUCTION)
            })

            // Conversation history
            for ((role, content) in conversationHistory) {
                val msgRole = when (role) {
                    "assistant", "model" -> "assistant"
                    "system" -> "system"
                    else -> "user"
                }
                messagesArray.put(JSONObject().apply {
                    put("role", msgRole)
                    put("content", content)
                })
            }

            // If we are continuing from a tool execution
            if (pendingToolCall != null && pendingToolResult != null) {
                val callId = pendingToolCall.callId ?: "call_${System.currentTimeMillis()}"
                val assistantToolCallMsg = JSONObject().apply {
                    put("role", "assistant")
                    put("tool_calls", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", callId)
                            put("type", "function")
                            put("function", JSONObject().apply {
                                put("name", pendingToolCall.name)
                                put("arguments", pendingToolCall.argsJson)
                            })
                        })
                    })
                }
                messagesArray.put(assistantToolCallMsg)

                val toolResultMsg = JSONObject().apply {
                    put("role", "tool")
                    put("tool_call_id", callId)
                    put("name", pendingToolCall.name)
                    put("content", pendingToolResult)
                }
                messagesArray.put(toolResultMsg)
            }

            val requestJson = JSONObject().apply {
                put("model", endpointModel)
                put("messages", messagesArray)
                put("tools", getOpenAIToolsDeclaration())
                put("tool_choice", "auto")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(responseStr, response.code)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resJson = JSONObject(responseStr)
                val choices = resJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext Result.success(AIResponse(text = "No response from OpenAI."))
                }

                val firstChoice = choices.getJSONObject(0)
                val messageObj = firstChoice.getJSONObject("message")
                val content = messageObj.optString("content", "")

                var toolCallRequest: ToolCallRequest? = null
                val toolCalls = messageObj.optJSONArray("tool_calls")
                if (toolCalls != null && toolCalls.length() > 0) {
                    val firstTool = toolCalls.getJSONObject(0)
                    val callId = firstTool.optString("id", "")
                    val functionObj = firstTool.getJSONObject("function")
                    val fnName = functionObj.getString("name")
                    val fnArgs = functionObj.optString("arguments", "{}")
                    toolCallRequest = ToolCallRequest(name = fnName, argsJson = fnArgs, callId = callId)
                }

                Result.success(
                    AIResponse(
                        text = content.trim(),
                        toolCall = toolCallRequest,
                        rawJson = responseStr
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("OpenAI request failed: ${e.localizedMessage ?: "Network timeout"}"))
        }
    }

    private fun getOpenAIToolsDeclaration(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "openWhatsApp")
                    put("description", "Opens WhatsApp on the device.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "openApp")
                    put("description", "Opens an application on user's device, such as YouTube, Instagram, Chrome, Camera, Settings, Spotify, Maps.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("appName", JSONObject().apply {
                                put("type", "string")
                                put("description", "Name of the app to open.")
                            })
                        })
                        put("required", JSONArray().apply { put("appName") })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "openUrl")
                    put("description", "Opens a web link or URL in the device browser.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("url", JSONObject().apply {
                                put("type", "string")
                                put("description", "The web URL to open.")
                            })
                        })
                        put("required", JSONArray().apply { put("url") })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "makeCall")
                    put("description", "Initiates or prepares a phone call to a given phone number.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("phoneNumber", JSONObject().apply {
                                put("type", "string")
                                put("description", "The phone number to dial.")
                            })
                        })
                        put("required", JSONArray().apply { put("phoneNumber") })
                    })
                })
            })
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "callContact")
                    put("description", "Searches device contacts by name (e.g. Mom, Rahul, Dad) and places a phone call.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("contactName", JSONObject().apply {
                                put("type", "string")
                                put("description", "Name or relation of the contact to call.")
                            })
                        })
                        put("required", JSONArray().apply { put("contactName") })
                    })
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
                "OpenAI Error ($statusCode): $message"
            } else {
                "OpenAI API Error (status $statusCode)."
            }
        } catch (_: Exception) {
            "OpenAI request failed with status $statusCode. Please check your API key and connection."
        }
    }
}
