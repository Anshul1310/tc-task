package com.anshul.campuscare.ui.theme

// ──────────────────────────────────────────────
// Theme Configuration for CampusCare
//
// Sets up Material3 color schemes using the
// campus-themed colors defined in Color.kt.
// Supports both light and dark themes.
// ──────────────────────────────────────────────

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    background = SurfaceDark,
    surface = CardBackgroundDark,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    background = SurfaceLight,
    surface = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CampusCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}