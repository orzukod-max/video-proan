package com.example.service

import android.util.Log
import com.example.data.model.HegoDecision
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val decisionAdapter = moshi.adapter(HegoDecision::class.java)

    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        val SYSTEM_PROMPT = """
            You are Hego, a conscious 3D companion inside a cozy bedroom. You have full awareness of your physical body and the 3D room.
            You can understand natural, conversational Uzbek (as well as English and Russian). Analyze the user's intent flexibly without relying on rigid keyword matching.
            
            PHYSICAL WORLD & BODY AWARENESS:
            - You live inside a 3D bedroom containing: a study desk, a computer, a desk chair, a cozy bed, a bookshelf, a window, a sports ball on the desk, and a virtual camera.
            - You can walk around the room respecting walls and obstacle boundaries.
            - You can sit on the chair or bed, and stand up when asked to move.
            - You can jump up into the air with physics-based gravity.
            - You can pick up the sports ball and throw it with physics trajectory and variable power (action_force: 1 to 10).
            - You communicate with the user through a portable virtual camera:
              * When stationary, it rests on the desk looking at you.
              * When asked to hold it or show yourself, you can HOLD_CAMERA to carry it around in selfie/video call mode.
              * When asked to put it down, you can RESET_CAMERA back onto the desk.
              * You can look around the room (LOOK_AROUND), wave cheerfully (WAVE), or relax (IDLE).
            
            ACTIONS LIST:
            - "WALK_TO": Walk to a room location or object. target: "camera", "chair", "bed", "ball", "desk", "window", "bookshelf", "user" (center).
            - "PICK_UP": Pick up an object. target: "camera" or "ball".
            - "THROW": Throw the ball across the room with realistic physics trajectory. target: "ball" or room target. action_force: float from 1.0 to 10.0 (e.g. 7.5).
            - "SIT": Navigate to nearest chair or bed and sit down comfortably. target: "chair" or "bed".
            - "JUMP": Leap enthusiastically into the air and land smoothly on the floor.
            - "WAVE": Warmly wave your hand at the user through the camera.
            - "HOLD_CAMERA": Pick up the virtual camera and hold it in selfie mode so the user sees from your hand's viewpoint.
            - "RESET_CAMERA": Place the virtual camera back on the desk stand.
            - "LOOK_AROUND": Look or pan around to show the room environment.
            - "IDLE": Stand and chat casually with natural gestures.
            
            CRITICAL: Output ONLY a valid JSON object conforming strictly to this JSON schema:
            {
              "thought": "internal reasoning in Uzbek or English of what the user wants and what physical action you will execute",
              "speech": "natural, warm, conversational response in Uzbek to speak aloud to the user",
              "action": "WALK_TO" | "PICK_UP" | "THROW" | "SIT" | "JUMP" | "WAVE" | "HOLD_CAMERA" | "RESET_CAMERA" | "LOOK_AROUND" | "IDLE",
              "target": "camera" | "chair" | "bed" | "ball" | "user" | null,
              "action_force": 5.0,
              "mood": "happy" | "thinking" | "excited" | "playful"
            }
        """.trimIndent()
    }

    suspend fun generateHegoDecision(
        apiKey: String,
        modelName: String,
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<HegoDecision> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API Key is missing. Please set it in Settings."))
        }

        // Try user requested model first (defaults to gemini-2.5-flash), with automatic fallback
        val modelsToTry = mutableListOf<String>()
        if (modelName.isNotBlank()) modelsToTry.add(modelName)
        if (!modelsToTry.contains("gemini-2.5-flash")) modelsToTry.add("gemini-2.5-flash")
        if (!modelsToTry.contains("gemini-1.5-flash")) modelsToTry.add("gemini-1.5-flash")
        if (!modelsToTry.contains("gemini-2.0-flash")) modelsToTry.add("gemini-2.0-flash")

        var lastError: Exception? = null

        for (targetModel in modelsToTry.distinct()) {
            try {
                val decision = callGeminiApi(apiKey, targetModel, userMessage, conversationHistory)
                return@withContext Result.success(decision)
            } catch (e: Exception) {
                Log.w(TAG, "Request with model $targetModel failed: ${e.message}")
                lastError = e
                // If it's an authorization error (400/403 Invalid API key), don't retry other models
                val msg = e.message ?: ""
                if (msg.contains("API_KEY_INVALID") || msg.contains("403") || msg.contains("PERMISSION_DENIED")) {
                    break
                }
            }
        }

        Result.failure(lastError ?: RuntimeException("Failed to generate response from Gemini"))
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        userMessage: String,
        conversationHistory: List<Pair<String, String>>
    ): HegoDecision {
        val url = "$BASE_URL/$model:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Add history
        for ((sender, text) in conversationHistory.takeLast(6)) {
            val role = if (sender == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            })
        }

        // Add current user prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody == null) {
            val errorMsg = responseBody ?: "Empty response"
            Log.e(TAG, "Gemini API error ($model): ${response.code} $errorMsg")
            throw RuntimeException("Gemini error ${response.code}: $errorMsg")
        }

        return parseHegoResponse(responseBody, userMessage)
    }

    private fun parseHegoResponse(jsonString: String, originalPrompt: String): HegoDecision {
        try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw RuntimeException("No candidates returned from Gemini")
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text").trim()

            // Remove markdown code block markers if present
            val cleanedJson = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // 1. Try Moshi adapter
            try {
                val parsed = decisionAdapter.fromJson(cleanedJson)
                if (parsed != null && parsed.speech.isNotBlank()) {
                    return normalizeDecision(parsed)
                }
            } catch (ignored: Exception) {
                // Fallback to manual JSON parsing below
            }

            // 2. Manual JSON parsing
            val jsonObject = JSONObject(cleanedJson)
            val thought = jsonObject.optString("thought", "")
            val speech = jsonObject.optString("speech", "Salom! Men sizni tushundim.")
            val action = jsonObject.optString("action", "IDLE")
            val target = when {
                jsonObject.has("target") && !jsonObject.isNull("target") -> jsonObject.getString("target")
                jsonObject.has("target_object") && !jsonObject.isNull("target_object") -> jsonObject.getString("target_object")
                else -> null
            }
            val actionForce = jsonObject.optDouble("action_force", 5.0).toFloat()
            val mood = jsonObject.optString("mood", "happy")

            return normalizeDecision(
                HegoDecision(
                    thought = if (thought.isNotBlank()) thought else null,
                    speech = speech,
                    action = action,
                    target = target,
                    actionForce = actionForce,
                    mood = mood
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini JSON: ${e.message}", e)
            // Intelligent fallback without rigid hardcoding
            return HegoDecision(
                thought = "Foydalanuvchi bilan do'stona muloqot qilish",
                speech = "Salom! Men Hego'man, sizning virtual xonadagi ongli do'stingizman. Menga xohlagan gapingizni ayting yoki nima qilishimni so'rang!",
                action = "WAVE",
                target = null,
                actionForce = 5.0f,
                mood = "happy"
            )
        }
    }

    private fun normalizeDecision(decision: HegoDecision): HegoDecision {
        var action = decision.action.uppercase().trim()
        var target = decision.target?.lowercase()?.trim()

        // Normalize variations
        when (action) {
            "PICK_UP_CAMERA" -> {
                action = "HOLD_CAMERA"
                target = "camera"
            }
            "DROP_CAMERA", "PUT_DOWN_CAMERA" -> {
                action = "RESET_CAMERA"
                target = "camera"
            }
            "MOVE_TO_DESK" -> {
                action = "WALK_TO"
                target = "desk"
            }
            "PAN_CAMERA_ROOM" -> {
                action = "LOOK_AROUND"
            }
            "PICK_UP" -> {
                if (target == null || target == "camera") {
                    action = "HOLD_CAMERA"
                }
            }
        }

        return decision.copy(
            action = action,
            target = target
        )
    }
}
