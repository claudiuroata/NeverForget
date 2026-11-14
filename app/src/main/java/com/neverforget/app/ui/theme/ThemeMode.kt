package com.neverforget.app.ui.theme

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme");

    fun next(): ThemeMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}
