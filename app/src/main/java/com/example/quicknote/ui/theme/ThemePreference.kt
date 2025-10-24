package com.example.quicknote.ui.theme

enum class ThemePreference(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(value: String?): ThemePreference =
            when (value?.uppercase()) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
    }
}
