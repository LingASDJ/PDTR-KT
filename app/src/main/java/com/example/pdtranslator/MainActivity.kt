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
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()
    private var isSelectingOriginal = true

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            readFileContent(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PDTranslatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TranslatorScreen(
                        viewModel = viewModel,
                        onSelectOriginal = { selectFile(true) },
                        onSelectTranslated = { selectFile(false) }
                    )
                }
            }
        }
    }

    private fun selectFile(isOriginal: Boolean) {
        isSelectingOriginal = isOriginal
        openDocumentLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
    }

    private fun readFileContent(uri: Uri) {
        val content = contentResolver.openInputStream(uri)?.use {
            BufferedReader(InputStreamReader(it)).readText()
        } ?: ""

        if (isSelectingOriginal) {
            viewModel.loadTranslations(content, viewModel.translatedContent.value)
        } else {
            viewModel.loadTranslations(viewModel.originalContent.value, content)
        }
    }
}
