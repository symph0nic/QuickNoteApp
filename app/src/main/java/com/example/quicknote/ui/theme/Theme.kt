package com.example.quicknote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ───────────────────────────────────────────────
// 1️⃣ Define your color palettes
// (We can tweak these later for Obsidian vibes)
// ───────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECFFF),
    secondary = Color(0xFFB4B4B4),
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2A2A2A),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF005FAF),
    secondary = Color(0xFF4A4A4A),
    background = Color(0xFFF8F8F8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E1E1E),
    onSurface = Color(0xFF1E1E1E),
)

// ───────────────────────────────────────────────
// 2️⃣ The composable wrapper we’ll use in MainActivity
// ───────────────────────────────────────────────
@Composable
fun QuickNoteTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

