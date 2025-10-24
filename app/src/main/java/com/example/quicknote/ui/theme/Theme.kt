package com.example.quicknote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.content.ContextWrapper


import android.content.Context



// ───────────────────────────────────────────────
// 1️⃣ Obsidian-inspired color palettes
// ───────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary        = Color(0xFF8AB4F8),   // link blue
    secondary      = Color(0xFF9AA0A6),   // muted grey-blue
    tertiary       = Color(0xFFBB86FC),   // accent purple (for icons/buttons)
    background     = Color(0xFF1E1E1E),   // main Obsidian dark background
    surface        = Color(0xFF2A2A2A),   // note surface / cards
    onPrimary      = Color.Black,
    onSecondary    = Color(0xFFECECEC),
    onTertiary     = Color.Black,
    onBackground   = Color(0xFFE0E0E0),
    onSurface      = Color(0xFFE0E0E0),
)

private val LightColors = lightColorScheme(
    primary        = Color(0xFF005FAF),   // Obsidian blue
    secondary      = Color(0xFF444B53),   // muted charcoal
    tertiary       = Color(0xFF4E6EF2),   // accent
    background     = Color(0xFFF9F9F9),   // paper tone
    surface        = Color(0xFFFFFFFF),
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onTertiary     = Color.White,
    onBackground   = Color(0xFF1C1C1C),
    onSurface      = Color(0xFF1C1C1C),
)




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

    Crossfade(targetState = darkTheme, label = "themeTransition") { isDark ->
        val colors = if (isDark) DarkColors else LightColors
        val view = LocalView.current

        SideEffect {
            // Obtain the Activity window cleanly
            val window = view.context.findActivity()?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.surface.toArgb()

                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }

        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Helper to find the current Activity from a LocalView context.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

