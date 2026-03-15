package com.example.pdtranslator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel) {
    // State collected from the ViewModel for translation
    val searchText by viewModel.searchText.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val displayEntries by viewModel.displayEntries.collectAsState()
    val translationProgress by viewModel.translationProgress.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // Search Field
        OutlinedTextField(
            value = searchText,
            onValueChange = { viewModel.setSearchText(it) },
            label = { Text(stringResource(id = R.string.translator_search_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Radio Buttons
        FilterButtons(filterState) { viewModel.setFilter(it) }

        Spacer(modifier = Modifier.height(4.dp))

        // "Complete Missing" Button
        if (filterState == FilterState.MISSING) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.completeMissingEntries() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.translator_complete_missing))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Indicator
        Column {
            Text(stringResource(id = R.string.translator_progress))
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(progress = translationProgress, modifier = Modifier.fillMaxWidth())
            Text("${(translationProgress * 100).toInt()}%", modifier = Modifier.align(Alignment.End))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Translation Entries List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayEntries, key = { it.key }) { entry ->
                TranslationCard(
                    entry = entry,
                    onValueChange = { newText -> viewModel.updateEntry(entry.key, newText) },
                    onAutoTranslate = { viewModel.autoTranslateEntry(entry) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pagination
        PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
    }
}

@Composable
fun TranslationCard(
    entry: TranslationEntry,
    onValueChange: (String) -> Unit,
    onAutoTranslate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.key, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (entry.isIdentical) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.translator_identical_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(entry.sourceValue, style = MaterialTheme.typography.bodyLarge)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            OutlinedTextField(
                value = entry.targetValue,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.common_translation)) },
                trailingIcon = {
                    IconButton(onClick = onAutoTranslate) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = stringResource(id = R.string.translator_auto_translate_button)
                        )
                    }
                }
            )
        }
    }
}
