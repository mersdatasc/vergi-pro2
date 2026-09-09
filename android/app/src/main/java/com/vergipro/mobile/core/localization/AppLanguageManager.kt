package com.vergipro.mobile.core.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    TURKISH("tr"),
    ENGLISH("en"),
}

object AppLanguageManager {
    private const val preferencesName = "vergipro_preferences"
    private const val languageKey = "app_language"

    fun selected(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(languageKey, "")
            .orEmpty()
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
    }

    fun select(context: Context, language: AppLanguage) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(languageKey, language.tag)
            .apply()
    }

    fun localizedContext(base: Context): Context {
        val language = selected(base)
        if (language == AppLanguage.SYSTEM) return base

        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }
}
