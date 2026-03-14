package com.example.pdtranslator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ChangelogItem(val version: String, val changes: List<String>)

val changelog = listOf(
    ChangelogItem("v0.1.0", listOf("初版发布"))
)

@Composable
fun ChangelogScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "更新记录", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(changelog) { item ->
                Text(text = item.version, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                item.changes.forEach { change ->
                    Text(text = "- $change", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
