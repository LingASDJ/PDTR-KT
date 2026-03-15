package com.example.pdtranslator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            label = { Text("搜索 (Key或原文)") },
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
                Text("补全缺失字段")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Indicator
        Column {
            Text("翻译进度")
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
                TranslationCard(entry) { newText -> viewModel.updateEntry(entry.key, newText) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pagination
        PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
    }
}
