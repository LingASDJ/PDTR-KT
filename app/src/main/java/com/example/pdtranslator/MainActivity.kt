package com.example.pdtranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val originalContent = try {
            File("scenes/scenes.properties").readText()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }

        val translatedContent = try {
            File("scenes/scenes_chk.properties").readText()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }

        viewModel.loadTranslations(originalContent, translatedContent)

        setContent {
            PDTranslatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TranslatorScreen(viewModel)
                }
            }
        }
    }
}
