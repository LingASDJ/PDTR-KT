package com.example.pdtranslator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

class MainActivity : ComponentActivity() {

  private val viewModel: TranslatorViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    installSplashScreen()

    setContent {
      val themeColor by viewModel.themeColor.collectAsState()
      val snackbarHostState = remember { SnackbarHostState() }

      PDTranslatorTheme(themeColor = themeColor) {
        // Global uiEvents listener — works on all screens
        LaunchedEffect(Unit) {
          viewModel.uiEvents.collectLatest {
            when (it) {
              is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(it.message)
            }
          }
        }

        AppNavigator(
          viewModel = viewModel,
          snackbarHostState = snackbarHostState,
          onLanguageSelected = { lang -> setLocale(lang) }
        )
      }
    }
  }

  override fun attachBaseContext(newBase: Context) {
    val sharedPreferences = newBase.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val locales = newBase.resources.configuration.locales
    val lang = sharedPreferences.getString("language", null)
      ?: (if (locales.isEmpty) Locale.getDefault().language else locales[0].language)
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
