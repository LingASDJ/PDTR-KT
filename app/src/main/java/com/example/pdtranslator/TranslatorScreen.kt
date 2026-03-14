package com.example.pdtranslator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pdtranslator.ui.theme.PDTranslatorTheme

@Composable
fun TranslatorScreen(
    viewModel: TranslatorViewModel,
    onSelectLanguageGroup: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val translationProgress by viewModel.translationProgress
    val searchQuery by viewModel.searchQuery
    val filterOption by viewModel.filterOption

    // --- Pagination --- 
    val paginatedItems by viewModel.paginatedItems
    val currentPage by viewModel.currentPage
    val totalPages by viewModel.totalPages

    val sourceLanguage by viewModel.sourceLanguage
    val targetLanguage by viewModel.targetLanguage
    val availableSourceLanguages by viewModel.availableSourceLanguages
    val availableTargetLanguages by viewModel.availableTargetLanguages


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // --- Header: Language Selection & Save --- 
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onSelectLanguageGroup) { Text("导入语言组") }
            if (targetLanguage != null) {
                Button(onClick = onSave, enabled = targetLanguage?.isModified == true) { Text("保存") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (availableSourceLanguages.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LanguageSelector(
                    label = "源语言",
                    selectedLanguage = sourceLanguage,
                    languages = availableSourceLanguages,
                    onLanguageSelected = { viewModel.setSourceLanguage(it.langCode) },
                    modifier = Modifier.weight(1f)
                )
                LanguageSelector(
                    label = "目标语言",
                    selectedLanguage = targetLanguage,
                    languages = availableTargetLanguages,
                    onLanguageSelected = { viewModel.setTargetLanguage(it.langCode) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Search and Filter --- 
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            label = { Text("搜索 (Key或原文)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(text = "所有", selected = filterOption == FilterOption.ALL) { viewModel.onFilterChange(FilterOption.ALL) }
            FilterChip(text = "未翻译", selected = filterOption == FilterOption.UNTRANSLATED) { viewModel.onFilterChange(FilterOption.UNTRANSLATED) }
            FilterChip(text = "已改动", selected = filterOption == FilterOption.MODIFIED) { viewModel.onFilterChange(FilterOption.MODIFIED) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Progress Bar --- 
        if (paginatedItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "翻译进度", style = MaterialTheme.typography.labelMedium)
                Text(text = "${(translationProgress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(
                progress = translationProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // --- Translation Items List (Paginated) --- 
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            if (maxWidth > 600.dp) { // Large screen: Side-by-side card layout
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(paginatedItems, key = { _, item -> item.key }) { _, item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.key, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = item.original, style = MaterialTheme.typography.bodyMedium)
                                }
                                OutlinedTextField(
                                    value = item.translation,
                                    onValueChange = { viewModel.updateTranslation(item.key, it) },
                                    label = { Text("译文") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else { // Small screen: Vertical card layout
                LazyColumn(verticalArrangements = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(paginatedItems, key = { _, item -> item.key }) { _, item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.key, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "原文: ${item.original}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = item.translation,
                                    onValueChange = { newTranslation ->
                                        viewModel.updateTranslation(item.key, newTranslation)
                                    },
                                    label = { Text("译文") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // --- Pagination Controls --- 
        if (totalPages > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.previousPage() }, enabled = currentPage > 1) {
                    Text("上一页")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "第 $currentPage / $totalPages 页")
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.nextPage() }, enabled = currentPage < totalPages) {
                    Text("下一页")
                }
            }
        }
    }
}

@Composable
fun LanguageSelector(
    label: String,
    selectedLanguage: LanguageFile?,
    languages: List<LanguageFile>,
    onLanguageSelected: (LanguageFile) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        Box(modifier = Modifier.clickable { expanded = true }) {
            OutlinedTextField(
                value = selectedLanguage?.displayName ?: "请选择",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Dropdown") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // Make it look like a button
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClick)) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, widthDp = 800)
@Composable
fun TranslatorScreenPreviewLarge() {
    val viewModel = TranslatorViewModel()
    // This is a preview, so we don't need real file loading logic.
    // We can simulate the state after a language group is selected.
    PDTranslatorTheme {
        TranslatorScreen(viewModel, onSelectLanguageGroup = {}, onSave = {})
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun TranslatorScreenPreviewSmall() {
    val viewModel = TranslatorViewModel()
    // This is a preview, so we don't need real file loading logic.
    // We can simulate the state after a language group is selected.
    PDTranslatorTheme {
        TranslatorScreen(viewModel, onSelectLanguageGroup = {}, onSave = {})
    }
}
