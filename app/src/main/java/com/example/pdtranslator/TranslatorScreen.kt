package com.example.pdtranslator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pdtranslator.ui.theme.PDTranslatorTheme

@Composable
fun TranslatorScreen(
    viewModel: TranslatorViewModel, 
    onSelectOriginal: () -> Unit,
    onSelectTranslated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val translationItems = viewModel.translationItems.value
    val translationProgress = viewModel.translationProgress.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onSelectOriginal) {
                Text("选择原文文件")
            }
            Button(onClick = onSelectTranslated) {
                Text("选择译文文件")
            }
        }

        if (translationItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "翻译进度")
                Text(text = "${(translationProgress * 100).toInt()}%")
            }
            LinearProgressIndicator(
                progress = translationProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(translationItems) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(text = "键: ${item.key}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "原文: ${item.original}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "译文: ${item.translation}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorScreenPreview() {
    val viewModel = TranslatorViewModel()
    val originalContent = "key1=Hello\nkey2=World"
    val translatedContent = "key1=你好\nkey2=世界"
    viewModel.loadTranslations(originalContent, translatedContent)
    PDTranslatorTheme {
        TranslatorScreen(viewModel, onSelectOriginal = {}, onSelectTranslated = {})
    }
}
