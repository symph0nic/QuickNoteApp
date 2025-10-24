package com.jonmechan.quicknote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jonmechan.quicknote.data.VaultPreferences
import com.jonmechan.quicknote.ui.theme.QuickNoteTheme
import com.jonmechan.quicknote.ui.theme.ThemePreference
import com.jonmechan.quicknote.ui.screens.HomeScreen
import com.jonmechan.quicknote.ui.screens.SettingsScreen
import com.jonmechan.quicknote.ui.screens.AboutScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this

        setContent {
            val navController = rememberNavController()
            val themePref by VaultPreferences
                .getThemePreference(context)
                .collectAsState(initial = ThemePreference.SYSTEM)

            QuickNoteTheme(themePreference = themePref) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") { HomeScreen(navController) }
                    composable("settings") { SettingsScreen(navController) }
                    composable("about") { AboutScreen(navController) }
                }
            }
        }
    }
}
