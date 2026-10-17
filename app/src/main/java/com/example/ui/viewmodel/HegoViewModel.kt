package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.model.CameraState
import com.example.data.model.ChatMessage
import com.example.data.model.HegoAction
import com.example.data.model.HegoDecision
import com.example.data.model.Sender
import android.net.Uri
import android.provider.OpenableColumns
import com.example.service.GeminiService
import com.example.service.SpeechManager
import com.example.service.TtsManager
import com.example.ui.threejs.Hego3DController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class HegoUiState(
    val apiKey: String = "",
    val selectedModel: String = PreferencesManager.DEFAULT_MODEL,
    val isApiKeyConfigured: Boolean = false,
    val isSceneReady: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isThinking: Boolean = false,
    val speechSoundLevel: Float = 0f,
    val transcription: String = "",
    val currentDecision: HegoDecision? = null,
    val cameraState: CameraState = CameraState.DESK_STAND,
    val cameraViewMode: String = "VIRTUAL_CAM", // VIRTUAL_CAM or FREE_ORBIT
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val roomFileName: String? = null,
    val characterFileName: String? = null,
    val roomFilePath: String? = null,
    val characterFilePath: String? = null,
    val isRoomLoaded: Boolean = false,
    val isCharacterLoaded: Boolean = false,
    val isUsingDefaultModels: Boolean = false
) {
    val isStartReady: Boolean
        get() = (isRoomLoaded && isCharacterLoaded) || isUsingDefaultModels
}

class HegoViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HegoViewModel"
    }

    private val prefsManager = PreferencesManager(application)
    private val geminiService = GeminiService()
    val speechManager = SpeechManager(application)
    val ttsManager = TtsManager(application)

    private var hego3DController: Hego3DController? = null

    private val _uiState = MutableStateFlow(HegoUiState())
    val uiState: StateFlow<HegoUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadSettings()
        setupSpeechManager()
        setupTtsManager()
    }

    private fun loadSettings() {
        val key = prefsManager.getGeminiApiKey()
        val model = prefsManager.getGeminiModel()
        _uiState.update {
            it.copy(
                apiKey = key,
                selectedModel = model,
                isApiKeyConfigured = key.isNotBlank()
            )
        }
    }

    fun saveSettings(key: String, model: String) {
        prefsManager.setGeminiApiKey(key)
        prefsManager.setGeminiModel(model)
        _uiState.update {
            it.copy(
                apiKey = key.trim(),
                selectedModel = model.trim(),
                isApiKeyConfigured = key.isNotBlank()
            )
        }
        viewModelScope.launch {
            _toastEvent.emit("Settings saved successfully!")
        }
    }

    fun onRoomFileSelected(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val fileName = getFileNameFromUri(uri) ?: "room.glb"
            val targetFile = File(context.filesDir, "custom_room.glb")
            
            val success = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying room file: ${e.message}", e)
                    false
                }
            }

            if (success) {
                _uiState.update {
                    it.copy(
                        roomFileName = fileName,
                        roomFilePath = targetFile.absolutePath,
                        isRoomLoaded = true,
                        isUsingDefaultModels = false
                    )
                }
                _toastEvent.emit("Xona yuklandi: $fileName")
            } else {
                _toastEvent.emit("Faylni yuklashda xatolik yuz berdi")
            }
        }
    }

    fun onCharacterFileSelected(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val fileName = getFileNameFromUri(uri) ?: "character.glb"
            val targetFile = File(context.filesDir, "custom_character.glb")
            
            val success = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying character file: ${e.message}", e)
                    false
                }
            }

            if (success) {
                _uiState.update {
                    it.copy(
                        characterFileName = fileName,
                        characterFilePath = targetFile.absolutePath,
                        isCharacterLoaded = true,
                        isUsingDefaultModels = false
                    )
                }
                _toastEvent.emit("Qahramon yuklandi: $fileName")
            } else {
                _toastEvent.emit("Faylni yuklashda xatolik yuz berdi")
            }
        }
    }

    fun useDefaultModels() {
        _uiState.update {
            it.copy(
                isUsingDefaultModels = true,
                isRoomLoaded = true,
                isCharacterLoaded = true,
                roomFileName = "Standart 3D Xona",
                characterFileName = "Hego (Standart)"
            )
        }
        viewModelScope.launch {
            _toastEvent.emit("Standart 3D modellar faollashtirildi!")
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val context = getApplication<Application>()
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get display name from cursor: ${e.message}")
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    fun set3DController(controller: Hego3DController) {
        this.hego3DController = controller
    }

    fun onSceneReady() {
        _uiState.update { it.copy(isSceneReady = true) }
        // Greet user on video call connection
        viewModelScope.launch {
            if (_uiState.value.messages.isEmpty()) {
                val initialDecision = HegoDecision(
                    thought = "Foydalanuvchini iliq kutib olish va xonaga taklif qilish",
                    speech = "Salom! Men Hego'man, ongli 3D hamrohingizman! Video qo'ng'irog'imiz ulandi, mening xonamga xush kelibsiz!",
                    action = "WAVE",
                    target = null,
                    actionForce = 5.0f,
                    mood = "happy"
                )
                onHegoResponded(initialDecision)
            }
        }
    }

    fun onCameraStateChanged(state: CameraState) {
        _uiState.update { it.copy(cameraState = state) }
    }

    fun toggleCameraViewMode() {
        val newMode = if (_uiState.value.cameraViewMode == "VIRTUAL_CAM") "FREE_ORBIT" else "VIRTUAL_CAM"
        _uiState.update { it.copy(cameraViewMode = newMode) }
        hego3DController?.setCameraMode(newMode)
    }

    private fun setupSpeechManager() {
        speechManager.initialize()
        speechManager.onFinalSpeechResult = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                sendUserMessage(recognizedText)
            }
        }

        viewModelScope.launch {
            speechManager.isListening.collect { listening ->
                _uiState.update { it.copy(isListening = listening) }
            }
        }

        viewModelScope.launch {
            speechManager.transcription.collect { text ->
                _uiState.update { it.copy(transcription = text) }
            }
        }

        viewModelScope.launch {
            speechManager.soundLevel.collect { level ->
                _uiState.update { it.copy(speechSoundLevel = level) }
            }
        }
    }

    private fun setupTtsManager() {
        ttsManager.onSpeakingStarted = {
            _uiState.update { it.copy(isSpeaking = true) }
            hego3DController?.setSpeaking(true)
        }
        ttsManager.onSpeakingFinished = {
            _uiState.update { it.copy(isSpeaking = false) }
            hego3DController?.setSpeaking(false)
        }
    }

    fun startVoiceInput() {
        if (_uiState.value.isSpeaking) {
            ttsManager.stop()
        }
        speechManager.startListening()
    }

    fun stopVoiceInput() {
        speechManager.stopListening()
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(sender = Sender.USER, text = text)
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isThinking = true,
                errorMessage = null
            )
        }

        // Set Hego to thinking animation in 3D
        hego3DController?.setMood("thinking")

        viewModelScope.launch {
            val key = _uiState.value.apiKey
            val model = _uiState.value.selectedModel

            val history = _uiState.value.messages.takeLast(6).map {
                (if (it.sender == Sender.USER) "user" else "model") to it.text
            }

            val result = geminiService.generateHegoDecision(
                apiKey = key,
                modelName = model,
                userMessage = text,
                conversationHistory = history
            )

            result.fold(
                onSuccess = { decision ->
                    _uiState.update { it.copy(isThinking = false) }
                    onHegoResponded(decision)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error from Gemini: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            isThinking = false,
                            errorMessage = error.message ?: "Failed to reach Gemini"
                        )
                    }
                    // Friendly offline fallback response
                    val fallbackDecision = HegoDecision(
                        thought = "API ulanishida nosozlik yuz berganda foydalanuvchiga yordam berish",
                        speech = "Kechirasiz, Gemini API ulanishida xatolik yuz berdi. Sozlamalardan API kalitingizni tekshiring!",
                        action = "WAVE",
                        target = null,
                        actionForce = 5.0f,
                        mood = "thinking"
                    )
                    onHegoResponded(fallbackDecision)
                }
            )
        }
    }

    private fun onHegoResponded(decision: HegoDecision) {
        _uiState.update {
            it.copy(
                currentDecision = decision,
                messages = it.messages + ChatMessage(sender = Sender.HEGO, text = decision.speech, decision = decision)
            )
        }

        // 1. Trigger 3D Action & Mood with Physics & Force
        hego3DController?.executeAction(
            action = decision.action,
            targetObject = decision.target,
            mood = decision.mood,
            actionForce = decision.actionForce,
            speech = decision.speech
        )

        // 2. Speak via TTS
        ttsManager.speak(decision.speech)
    }

    fun triggerQuickAction(actionName: String, promptText: String) {
        sendUserMessage(promptText)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
