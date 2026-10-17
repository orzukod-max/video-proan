package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CameraState
import com.example.ui.theme.HegoAmber
import com.example.ui.theme.HegoBlue
import com.example.ui.theme.HegoCyan
import com.example.ui.theme.HegoGreen
import com.example.ui.theme.HegoRed
import com.example.ui.threejs.Hego3DController
import com.example.ui.threejs.Hego3DWebView
import com.example.ui.viewmodel.HegoUiState

@Composable
fun ExperienceScreen(
    uiState: HegoUiState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onControllerReady: (Hego3DController) -> Unit,
    onSceneReady: () -> Unit,
    onCameraStateChanged: (CameraState) -> Unit,
    onObjectClicked: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSendMessage: (String) -> Unit,
    onQuickAction: (String, String) -> Unit,
    onToggleCameraMode: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var textInputText by remember { mutableStateOf("") }
    var isKeyboardOpen by remember { mutableStateOf(false) }

    // Audio recording permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartVoice()
        }
    }

    BackHandler {
        if (isKeyboardOpen) {
            isKeyboardOpen = false
        } else {
            onBackClick()
        }
    }

    // Blinking recording indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    // Pulsing audio wave scale when listening or speaking
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. FULLSCREEN 3D WEBGL WEBVIEW ---
        Hego3DWebView(
            modifier = Modifier.fillMaxSize(),
            onControllerReady = onControllerReady,
            onSceneReady = onSceneReady,
            onCameraStateChanged = onCameraStateChanged,
            onObjectClicked = onObjectClicked
        )

        // --- 2. TOP HUD: VIDEO CALL BAR ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("experience_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Call status badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = HegoRed.copy(alpha = recAlpha),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HEGO CAM",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1080p",
                        style = MaterialTheme.typography.labelSmall,
                        color = HegoCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Camera Perspective Switcher
                    IconButton(
                        onClick = onToggleCameraMode,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("toggle_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera Mode",
                            tint = if (uiState.cameraViewMode == "VIRTUAL_CAM") HegoCyan else Color.White
                        )
                    }

                    // Settings Shortcut
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("experience_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Camera Position Indicator (In Hand vs On Desk)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val isHandheld = uiState.cameraState == CameraState.HEGO_HAND_HELD
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isHandheld) HegoBlue.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isHandheld) Icons.Default.PanTool else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isHandheld) Color.White else HegoCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHandheld) "Held in Hego's Hand (Selfie Call)" else "Desk Tripod Stand",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- 3. MIDDLE: SUBTITLE & HEGO DIALOGUE BUBBLE ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp, start = 16.dp, end = 16.dp)
        ) {
            val latestHegoMsg = uiState.messages.lastOrNull { it.sender == com.example.data.model.Sender.HEGO }
            if (latestHegoMsg != null || uiState.isThinking || uiState.isListening) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialogue_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCC0F172A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isSpeaking) HegoGreen else HegoCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HEGO",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = HegoCyan
                                )
                                if (uiState.currentDecision != null) {
                                    val actionEnum = com.example.data.model.HegoAction.fromKey(uiState.currentDecision.action)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x330284C7)
                                    ) {
                                        Text(
                                            text = "${actionEnum.emoji} ${uiState.currentDecision.action}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = HegoCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Speech Audio Waves
                            if (uiState.isSpeaking) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    repeat(4) { idx ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height((8 + (idx * 4) % 14).dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(HegoGreen)
                                        )
                                    }
                                }
                            }
                        }

                        // Internal conscious reasoning / thought display
                        if (uiState.currentDecision?.thought != null && uiState.currentDecision.thought.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x2238BDF8),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x4438BDF8))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🧠 O'y: ",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = HegoAmber
                                    )
                                    Text(
                                        text = uiState.currentDecision.thought,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFBAE6FD),
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (uiState.isThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = HegoCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Hego o'ylamoqda va qaror qabul qilmoqda...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        } else if (uiState.isListening && uiState.transcription.isNotBlank()) {
                            Text(
                                text = "Sizni eshitmoqda: \"${uiState.transcription}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = HegoCyan
                            )
                        } else if (latestHegoMsg != null) {
                            Text(
                                text = latestHegoMsg.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 4. BOTTOM CONTROLS & FLOATING DOCK ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // Quick Action Prompt Chips (Autonomous Agent Commands)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    label = "🦘 Sakra!",
                    onClick = { onQuickAction("JUMP", "Hego, baland sakrab ko'rsat!") }
                )
                QuickActionChip(
                    label = "⚽ Koptokni ot!",
                    onClick = { onQuickAction("THROW", "Hego, stol ustidagi koptokni olib xona o'rtasiga qattiq ot!") }
                )
                QuickActionChip(
                    label = "🪑 Stulga o'tir",
                    onClick = { onQuickAction("SIT", "Hego, stulga borib qulay o'tir.") }
                )
                QuickActionChip(
                    label = "📹 Kamerani ol",
                    onClick = { onQuickAction("HOLD_CAMERA", "Hego, kamerani qo'lingga olib o'zingni ko'rsat!") }
                )
                QuickActionChip(
                    label = "🔄 Stolga qo'y",
                    onClick = { onQuickAction("RESET_CAMERA", "Hego, kamerani stolga qaytarib qo'y.") }
                )
                QuickActionChip(
                    label = "👀 Xonani ko'rsat",
                    onClick = { onQuickAction("LOOK_AROUND", "Hego, xonaning atrofini ko'rsat!") }
                )
                QuickActionChip(
                    label = "👋 Salom ber",
                    onClick = { onQuickAction("WAVE", "Hego, qo'lingni silkitib salom ber!") }
                )
                QuickActionChip(
                    label = "🛌 Karavotga o'tir",
                    onClick = { onQuickAction("SIT", "Hego, karavotga borib o'tir.") }
                )
                QuickActionChip(
                    label = "✨ O'zing haqingda ayt",
                    onClick = { onQuickAction("IDLE", "Hego, o'zing va bu 3D xona haqingda so'zlab ber!") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Text Input Bar (if open)
            AnimatedVisibility(
                visible = isKeyboardOpen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInputText,
                        onValueChange = { textInputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        placeholder = { Text("Ask or command Hego...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInputText.isNotBlank()) {
                                    onSendMessage(textInputText)
                                    textInputText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xEE0F172A),
                            unfocusedContainerColor = Color(0xCC0F172A),
                            focusedBorderColor = HegoCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInputText.isNotBlank()) {
                                onSendMessage(textInputText)
                                textInputText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(HegoBlue)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }

            // Main Dock Controls: Mic Button & Keyboard Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keyboard Toggle Button
                IconButton(
                    onClick = { isKeyboardOpen = !isKeyboardOpen },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("toggle_keyboard_button")
                ) {
                    Icon(
                        imageVector = if (isKeyboardOpen) Icons.Default.Close else Icons.Default.Keyboard,
                        contentDescription = "Keyboard Input",
                        tint = Color.White
                    )
                }

                // Main Microphone Call Button
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing audio ring when listening
                    if (uiState.isListening) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(HegoCyan.copy(alpha = 0.3f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (uiState.isListening) {
                                        listOf(HegoRed, Color(0xFFDC2626))
                                    } else {
                                        listOf(HegoCyan, HegoBlue)
                                    }
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (uiState.isListening) {
                                    onStopVoice()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        onStartVoice()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                            .testTag("mic_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Camera Action Shortcut (Quick Pick up / Put down)
                val isHandheld = uiState.cameraState == CameraState.HEGO_HAND_HELD
                IconButton(
                    onClick = {
                        if (isHandheld) {
                            onQuickAction("PUT_DOWN_CAMERA", "Hego, put down the camera")
                        } else {
                            onQuickAction("PICK_UP_CAMERA", "Hego, pick up the camera")
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("quick_camera_action_button")
                ) {
                    Icon(
                        imageVector = if (isHandheld) Icons.Default.Videocam else Icons.Default.PanTool,
                        contentDescription = "Camera Toggle",
                        tint = HegoCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC1E293B))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}
