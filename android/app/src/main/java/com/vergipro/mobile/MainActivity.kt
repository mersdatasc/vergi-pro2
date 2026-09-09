package com.vergipro.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vergipro.mobile.core.designsystem.AppThemeManager
import com.vergipro.mobile.core.designsystem.AppThemePreference
import com.vergipro.mobile.core.designsystem.VergiProTheme
import com.vergipro.mobile.core.localization.AppLanguageManager
import com.vergipro.mobile.feature.identity.IdentityFlow

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedTheme = AppThemeManager.selected(this)
        setTheme(
            when (savedTheme) {
                AppThemePreference.LIGHT -> R.style.Theme_VergiPro_Splash_Light
                AppThemePreference.DARK -> R.style.Theme_VergiPro_Splash_Dark
                AppThemePreference.SYSTEM -> R.style.Theme_VergiPro_Splash
            },
        )
        // SplashScreen API — Theme.VergiPro.Splash tema üzerinden VP mark gösterilir.
        // postSplashScreenTheme'e geçiş onCreate'den önce gerçekleşir.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = when (savedTheme) {
                AppThemePreference.SYSTEM -> isSystemInDarkTheme()
                AppThemePreference.LIGHT -> false
                AppThemePreference.DARK -> true
            }
            VergiProTheme(darkTheme = darkTheme) { IdentityFlow() }
        }
    }
}
