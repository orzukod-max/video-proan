package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingFinished: (() -> Unit)? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            // Attempt with Google TTS engine first, fallback to default
            tts = TextToSpeech(context, this, GOOGLE_TTS_PACKAGE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize with Google TTS package, falling back to default: ${e.message}")
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.apply {
                // Cheerful youthful boy voice tuning for Hego
                setPitch(1.12f)
                setSpeechRate(1.02f)

                // Try system locale or Uzbek / English
                val locale = Locale.getDefault()
                val langResult = setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    setLanguage(Locale.US)
                }

                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        onSpeakingStarted?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeakingFinished?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeakingFinished?.invoke()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        onSpeakingFinished?.invoke()
                        Log.e(TAG, "TTS Utterance error code: $errorCode")
                    }
                })
            }
            Log.i(TAG, "TTS initialized successfully")
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech (status: $status)")
            isInitialized = false
        }
    }

    fun speak(text: String, utteranceId: String = "hego_speech_${System.currentTimeMillis()}") {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        try {
            // Detect language from text characters (e.g. Cyrillic vs Latin vs Uzbek apostrophes)
            val detectedLocale = detectLanguage(text)
            tts?.setLanguage(detectedLocale)

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        } finally {
            _isSpeaking.value = false
            onSpeakingFinished?.invoke()
        }
    }

    private fun detectLanguage(text: String): Locale {
        // Quick heuristics: Cyrillic letters -> Russian
        val hasCyrillic = text.any { it in '\u0400'..'\u04FF' }
        if (hasCyrillic) return Locale("ru", "RU")

        // Uzbek common words/characters (o', g', sh, ch, menga, qani, nima, qanaqa)
        val lower = text.lowercase()
        if (lower.contains("o'") || lower.contains("g'") || lower.contains("salom") ||
            lower.contains("xona") || lower.contains("qani") || lower.contains("menga") || lower.contains("rahmat")
        ) {
            val uzbek = Locale("uz", "UZ")
            if (tts?.isLanguageAvailable(uzbek) ?: -1 >= TextToSpeech.LANG_AVAILABLE) {
                return uzbek
            }
        }

        // Default to device locale or English
        val defaultLocale = Locale.getDefault()
        return if (tts?.isLanguageAvailable(defaultLocale) ?: -1 >= TextToSpeech.LANG_AVAILABLE) {
            defaultLocale
        } else {
            Locale.US
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
