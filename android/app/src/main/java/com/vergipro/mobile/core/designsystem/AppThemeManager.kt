package com.vergipro.mobile.core.designsystem

import android.content.Context

enum class AppThemePreference(val persistedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPersisted(value: String?): AppThemePreference =
            entries.firstOrNull { it.persistedValue == value } ?: SYSTEM
    }
}

object AppThemeManager {
    private const val preferencesName = "appearance_preferences"
    private const val themeKey = "app_theme"

    fun selected(context: Context): AppThemePreference = AppThemePreference.fromPersisted(
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(themeKey, null),
    )

    fun select(context: Context, theme: AppThemePreference) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(themeKey, theme.persistedValue)
            .apply()
    }
}
