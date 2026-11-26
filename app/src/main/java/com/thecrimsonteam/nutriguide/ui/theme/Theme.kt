package com.thecrimsonteam.nutriguide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101010)
private val Paper = Color(0xFFF4F4F4)
private val LightSurface = Color(0xFFFFFFFF)
private val DarkSurface = Color(0xFF171717)
private val LightMuted = Color(0xFF737373)
private val DarkMuted = Color(0xFFB2B2B2)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF4A4A4A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAEAEA),
    onSecondaryContainer = Color(0xFF202020),
    tertiary = Color(0xFF5D5D5D),
    background = Paper,
    onBackground = Ink,
    surface = LightSurface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAEAEA),
    onSurfaceVariant = LightMuted,
    outline = Color(0xFFD5D5D5),
    outlineVariant = Color(0xFFE4E4E4),
    error = Color(0xFF4A4A4A),
    onError = Color.White,
    errorContainer = Color(0xFFE4E4E4),
    onErrorContainer = Color(0xFF202020)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF101010),
    primaryContainer = Color(0xFF292929),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFD0D0D0),
    onSecondary = Color(0xFF101010),
    secondaryContainer = Color(0xFF272727),
    onSecondaryContainer = Color(0xFFEAEAEA),
    tertiary = Color(0xFFBDBDBD),
    background = Color(0xFF090909),
    onBackground = Color(0xFFF5F5F5),
    surface = DarkSurface,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFE0E0E0),
    onError = Color(0xFF101010),
    errorContainer = Color(0xFF303030),
    onErrorContainer = Color(0xFFF5F5F5)
)

private val NutriGuideShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val NutriGuideTypography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.5).sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.7.sp
    )
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
        typography = NutriGuideTypography,
        shapes = NutriGuideShapes,
        content = content
    )
}
