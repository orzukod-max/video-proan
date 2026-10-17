package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HegoDecision(
    @Json(name = "thought")
    val thought: String? = null,

    @Json(name = "speech")
    val speech: String,

    @Json(name = "action")
    val action: String = "IDLE", // WALK_TO, PICK_UP, THROW, SIT, JUMP, WAVE, HOLD_CAMERA, RESET_CAMERA, LOOK_AROUND, IDLE

    @Json(name = "target")
    val target: String? = null, // camera, chair, bed, ball, user, window, desk, bookshelf, null

    @Json(name = "action_force")
    val actionForce: Float = 5.0f,

    @Json(name = "mood")
    val mood: String = "happy" // happy, thinking, excited, curious, playful, surprised
)

enum class HegoAction(val actionKey: String, val displayName: String, val emoji: String) {
    WALK_TO("WALK_TO", "Walk To", "🚶"),
    PICK_UP("PICK_UP", "Pick Up", "✋"),
    THROW("THROW", "Throw Object", "⚽"),
    SIT("SIT", "Sit Down", "🪑"),
    JUMP("JUMP", "Jump", "🦘"),
    WAVE("WAVE", "Wave", "👋"),
    HOLD_CAMERA("HOLD_CAMERA", "Hold Camera", "📹"),
    RESET_CAMERA("RESET_CAMERA", "Reset Camera", "🔄"),
    LOOK_AROUND("LOOK_AROUND", "Look Around", "👀"),
    IDLE("IDLE", "Idle", "✨");

    companion object {
        fun fromKey(key: String): HegoAction {
            val normalized = key.uppercase().trim()
            return when (normalized) {
                "PICK_UP_CAMERA" -> HOLD_CAMERA
                "DROP_CAMERA", "PUT_DOWN_CAMERA" -> RESET_CAMERA
                "MOVE_TO_DESK" -> WALK_TO
                "PAN_CAMERA_ROOM" -> LOOK_AROUND
                else -> entries.firstOrNull { it.actionKey.equals(normalized, ignoreCase = true) } ?: IDLE
            }
        }
    }
}

enum class CameraState {
    DESK_STAND,     // Resting stationary on desk tripod looking at Hego & room
    HEGO_HAND_HELD, // Held in Hego's right hand moving with his body & gestures
    TRANSITIONING   // Moving between desk and hand
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val decision: HegoDecision? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Sender {
    USER,
    HEGO
}
