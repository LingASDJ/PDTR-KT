
package com.example.pdtranslator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            val themeColor by viewModel.themeColor.collectAsState()
            PDTranslatorTheme(themeColor = themeColor) {
                AppNavigator(
                    viewModel = viewModel,
                    onLanguageSelected = { lang -> setLocale(lang) }
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val lang = sharedPreferences.getString("language", null) ?: newBase.resources.configuration.locales[0].language
        val locale = Locale(lang)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    fun setLocale(lang: String) {
        val sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("language", lang)
            apply()
        }
        recreate()
    }
}
