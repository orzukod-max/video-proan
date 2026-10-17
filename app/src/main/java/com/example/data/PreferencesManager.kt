package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences = createEncryptedSharedPreferences(context)

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_FILE = "hego_secure_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_SPEECH_LANG = "speech_language"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_CAMERA_MODE = "camera_mode"

        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences failed, falling back to standard prefs: ${e.message}")
            context.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getGeminiApiKey(): String {
        val saved = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved

        // Fallback to BuildConfig if provided via .env
        return try {
            val buildConfigKey = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun getGeminiModel(): String {
        return prefs.getString(KEY_GEMINI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setGeminiModel(model: String) {
        prefs.edit().putString(KEY_GEMINI_MODEL, model).apply()
    }

    fun getSpeechLanguage(): String {
        return prefs.getString(KEY_SPEECH_LANG, "auto") ?: "auto"
    }

    fun setSpeechLanguage(lang: String) {
        prefs.edit().putString(KEY_SPEECH_LANG, lang).apply()
    }

    fun getCameraMode(): String {
        return prefs.getString(KEY_CAMERA_MODE, "VIRTUAL_CAM") ?: "VIRTUAL_CAM"
    }

    fun setCameraMode(mode: String) {
        prefs.edit().putString(KEY_CAMERA_MODE, mode).apply()
    }
}
