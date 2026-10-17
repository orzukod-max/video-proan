package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HegoDarkColorScheme = darkColorScheme(
    primary = HegoCyan,
    onPrimary = Color(0xFF003258),
    primaryContainer = HegoDarkBlue,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = HegoLightBlue,
    onSecondary = Color(0xFF003355),
    secondaryContainer = Color(0xFF004B75),
    onSecondaryContainer = Color(0xFFCBE6FF),
    tertiary = HegoAmber,
    onTertiary = Color(0xFF452B00),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = HegoRed
)

private val HegoLightColorScheme = lightColorScheme(
    primary = HegoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = HegoCyan,
    onSecondary = Color.White,
    tertiary = HegoAmber,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun HegoTheme(
    darkTheme: Boolean = true, // Default to immersive dark mode for 3D room
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HegoDarkColorScheme else HegoLightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
