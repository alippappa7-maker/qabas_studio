package com.qabas.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single AI entry point for application code.
 * All provider credentials stay in the Supabase Edge Function.
 */
object GatewayAiService {

    suspend fun gemini(payload: JSONObject): JSONObject? =
        PlatformGatewayClient.proxyGemini(payload)

    suspend fun groq(payload: JSONObject): JSONObject? =
        PlatformGatewayClient.proxyGroq(payload)

    suspend fun openai(payload: JSONObject): JSONObject? =
        PlatformGatewayClient.proxyOpenAI(payload)

    suspend fun openrouter(payload: JSONObject): JSONObject? =
        PlatformGatewayClient.proxyOpenRouter(payload)

    suspend fun geminiText(prompt: String, system: String? = null): String? = withContext(Dispatchers.IO) {
        val contents = JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        })
        val payload = JSONObject().apply {
            put("contents", contents)
            if (!system.isNullOrBlank()) {
                put("systemInstruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", system))
                ))
            }
            put("generationConfig", JSONObject().put("temperature", 0.7))
        }
        val response = gemini(payload) ?: return@withContext null
        extractGeminiText(response)
    }

    suspend fun chatText(provider: String, prompt: String, system: String? = null): String? =
        withContext(Dispatchers.IO) {
            val messages = JSONArray()
            if (!system.isNullOrBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", system))
            }
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            val payload = JSONObject().apply {
                put("messages", messages)
                put("temperature", 0.7)
            }
            val response = when (provider.lowercase()) {
                "groq" -> groq(payload)
                "openai" -> openai(payload)
                "openrouter" -> openrouter(payload)
                else -> null
            } ?: return@withContext null
            extractChoiceText(response)
        }

    private fun extractGeminiText(json: JSONObject): String? {
        val candidates = json.optJSONArray("candidates") ?: return null
        val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: return null
        return parts.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
    }

    private fun extractChoiceText(json: JSONObject): String? {
        return json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.takeIf { it.isNotBlank() }
    }
}
