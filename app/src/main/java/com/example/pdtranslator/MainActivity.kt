package com.example.pdtranslator

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()

    private val saveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { saveModifiedContent(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            PDTranslatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    val openLanguageFilesLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                        if (uris.isNotEmpty()) {
                            viewModel.loadLanguageFiles(contentResolver, uris)
                            navController.navigate("languageGroupSelector")
                        }
                    }

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onSelectLanguageGroup = {
                                    // Let user select multiple .properties files
                                    openLanguageFilesLauncher.launch(arrayOf("*/*"))
                                },
                                onSave = { onSave() }
                            )
                        }
                        composable("languageGroupSelector") {
                            LanguageGroupScreen(
                                viewModel = viewModel,
                                onGroupSelected = { navController.popBackStack() },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onSave() {
        val targetLanguage = viewModel.targetLanguage.value ?: return
        val fileName = targetLanguage.fileName
        saveLauncher.launch(fileName)
    }

    private fun saveModifiedContent(uri: Uri) {
        val content = viewModel.getModifiedContentForTarget() ?: return
        try {
            contentResolver.openOutputStream(uri)?.use {
                OutputStreamWriter(it).use {
                    writer -> writer.write(content)
                }
            }
        } catch (e: Exception) {
            // Handle exceptions
        }
    }
}
