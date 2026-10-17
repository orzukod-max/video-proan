package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ExperienceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HegoTheme
import com.example.ui.viewmodel.HegoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HegoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HegoApp()
                }
            }
        }
    }
}

@Composable
fun HegoApp(viewModel: HegoViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(navController.context, message, Toast.LENGTH_SHORT).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                uiState = uiState,
                onStartClick = { navController.navigate("experience") },
                onSettingsClick = { navController.navigate("settings") },
                onRoomSelected = { uri -> viewModel.onRoomFileSelected(uri) },
                onCharacterSelected = { uri -> viewModel.onCharacterFileSelected(uri) },
                onUseDefaultModels = { viewModel.useDefaultModels() }
            )
        }

        composable("settings") {
            SettingsScreen(
                currentApiKey = uiState.apiKey,
                currentModel = uiState.selectedModel,
                ttsManager = viewModel.ttsManager,
                onSave = { key, model ->
                    viewModel.saveSettings(key, model)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("experience") {
            ExperienceScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                onControllerReady = { controller ->
                    viewModel.set3DController(controller)
                },
                onSceneReady = {
                    viewModel.onSceneReady()
                },
                onCameraStateChanged = { state ->
                    viewModel.onCameraStateChanged(state)
                },
                onObjectClicked = { objectName ->
                    if (objectName == "hego") {
                        viewModel.sendUserMessage("Hego, I just tapped on you! How are you doing?")
                    }
                },
                onStartVoice = {
                    viewModel.startVoiceInput()
                },
                onStopVoice = {
                    viewModel.stopVoiceInput()
                },
                onSendMessage = { text ->
                    viewModel.sendUserMessage(text)
                },
                onQuickAction = { action, prompt ->
                    viewModel.triggerQuickAction(action, prompt)
                },
                onToggleCameraMode = {
                    viewModel.toggleCameraViewMode()
                }
            )
        }
    }
}
