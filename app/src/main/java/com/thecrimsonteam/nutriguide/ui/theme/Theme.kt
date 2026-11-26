package com.thecrimsonteam.nutriguide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF496B55),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7EEE8),
    onPrimaryContainer = Color(0xFF1B3323),
    secondary = Color(0xFF65776A),
    background = Color(0xFFF7F8F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF1EC),
    onBackground = Color(0xFF17231B),
    onSurface = Color(0xFF17231B),
    onSurfaceVariant = Color(0xFF68766C),
    outline = Color(0xFFE3E8E3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC6A7),
    onPrimary = Color(0xFF173321),
    primaryContainer = Color(0xFF29392E),
    onPrimaryContainer = Color(0xFFD6E8D9),
    secondary = Color(0xFFB4C7B8),
    background = Color(0xFF111713),
    surface = Color(0xFF1B241E),
    surfaceVariant = Color(0xFF29342C),
    onBackground = Color(0xFFF0F4F0),
    onSurface = Color(0xFFF0F4F0),
    onSurfaceVariant = Color(0xFFA5B1A8),
    outline = Color(0xFF303C33)
)

@Composable
fun NutriGuideTheme(
    mode: String,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
