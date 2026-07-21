package com.example.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(
        apiKey: String,
        systemPrompt: String,
        chatHistory: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            
            val jsonBody = JSONObject()
            
            // System instruction
            if (systemPrompt.isNotEmpty()) {
                val sysInstruction = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().put("text", systemPrompt))
                sysInstruction.put("parts", parts)
                jsonBody.put("systemInstruction", sysInstruction)
            }
            
            // Contents
            val contentsArray = JSONArray()
            for (turn in chatHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (turn.first == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", turn.second))
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }
            jsonBody.put("contents", contentsArray)
            
            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            jsonBody.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: Code ${response.code}, Body: $errBody")
                    throw Exception("API Error: ${response.code}")
                }
                
                val respStr = response.body?.string() ?: throw Exception("Empty response body")
                val respJson = JSONObject(respStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "")
                        }
                    }
                }
                throw Exception("Could not parse response content")
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateContent error", e)
            "Error: ${e.message}"
        }
    }
}
