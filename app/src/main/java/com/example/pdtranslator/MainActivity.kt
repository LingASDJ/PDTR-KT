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
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()

    private val openLanguageGroupLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            loadLanguageGroupFromUris(uris)
        }
    }

    private val saveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { saveModifiedContent(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            PDTranslatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        viewModel = viewModel,
                        onSelectLanguageGroup = { selectLanguageGroup() },
                        onSave = { onSave() }
                    )
                }
            }
        }
    }

    private fun selectLanguageGroup() {
        openLanguageGroupLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
    }

    private fun loadLanguageGroupFromUris(uris: List<Uri>) {
        val fileContents = uris.associate { uri ->
            val path = uri.path ?: "unknown"
            val content = contentResolver.openInputStream(uri)?.use {
                BufferedReader(InputStreamReader(it)).readText()
            } ?: ""
            path to content
        }.filterValues { it.isNotBlank() }

        if (fileContents.isNotEmpty()) {
            viewModel.loadLanguageGroup(fileContents)
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
