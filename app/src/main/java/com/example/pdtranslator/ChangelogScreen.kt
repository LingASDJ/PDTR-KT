package com.example.pdtranslator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChangelogItem(val version: String, val changes: List<String>)

@Composable
fun getChangelog(): List<ChangelogItem> {
    return listOf(
        ChangelogItem(stringResource(R.string.changelog_version_1_1_0), stringResource(R.string.changelog_content_1_1_0).split("\n")),
        ChangelogItem(stringResource(R.string.changelog_version_1_0_2), stringResource(R.string.changelog_content_1_0_2).split("\n")),
        ChangelogItem(stringResource(R.string.changelog_version_1_0_1), stringResource(R.string.changelog_content_1_0_1).split("\n")),
        ChangelogItem(stringResource(R.string.changelog_version_1_0_0), listOf(stringResource(R.string.changelog_content_1_0_0))),
        ChangelogItem(stringResource(R.string.changelog_version_0_1_0), listOf(stringResource(R.string.changelog_content_0_1_0)))
    )
}

@Composable
fun ChangelogScreen() {
    val changelog = getChangelog()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.changelog_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(changelog) { item ->
                    ChangelogCard(item = item)
                }
            }
        }
    }
}

@Composable
fun ChangelogCard(item: ChangelogItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.version,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                item.changes.forEach { change ->
                    if (change.isNotBlank()) {
                        val bullet = "• "
                        Box(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                            Text(bullet, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = change.trim().removePrefix("-"),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
