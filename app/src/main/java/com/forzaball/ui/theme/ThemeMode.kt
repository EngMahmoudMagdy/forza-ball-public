package com.forzaball.ui.theme

enum class ThemeMode(val storageKey: String, val label: String) {
    Dark("dark", "Dark"),
    Light("light", "Light"),
    System("system", "Match system"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: Dark
    }
}
