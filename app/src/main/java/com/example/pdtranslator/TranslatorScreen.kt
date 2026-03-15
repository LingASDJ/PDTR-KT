package com.example.pdtranslator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel) {
    val context = LocalContext.current

    // State collected from the ViewModel
    val languageGroupNames by viewModel.languageGroupNames.collectAsState()
    val selectedGroupName by viewModel.selectedGroupName.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val sourceLangCode by viewModel.sourceLangCode.collectAsState()
    val targetLangCode by viewModel.targetLangCode.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val displayEntries by viewModel.displayEntries.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()
    val translationProgress by viewModel.translationProgress.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()

    // File pickers
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.loadFilesFromZip(context.contentResolver, it) } }
    )
    val multipleFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris -> if (uris.isNotEmpty()) viewModel.loadFilesFromUris(context.contentResolver, uris) }
    )
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri -> uri?.let { viewModel.saveChangesToZip(context.contentResolver, it) } }
    )

    var showImportSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // Top Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showImportSheet = true }) {
                Text("导入文件")
            }
            Button(
                onClick = { saveFileLauncher.launch("translation_output.zip") },
                enabled = isSaveEnabled
            ) {
                Text("保存")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Group Selector
        LanguageGroupSelector(languageGroupNames, selectedGroupName) { viewModel.selectGroup(it) }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Language Selectors
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageSelector("源语言", availableLanguages, sourceLangCode) { viewModel.selectSourceLanguage(it) }
            LanguageSelector("目标语言", availableLanguages, targetLangCode) { viewModel.selectTargetLanguage(it) }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

    // Import Bottom Sheet
    if (showImportSheet) {
        ModalBottomSheet(onDismissRequest = { showImportSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("请选择导入方式", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button({
                    showImportSheet = false
                    zipPickerLauncher.launch(arrayOf("application/zip"))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("从 ZIP 压缩包导入")
                }
                Spacer(Modifier.height(8.dp))
                Button({
                    showImportSheet = false
                    multipleFilesPickerLauncher.launch(arrayOf("text/plain", "application/octet-stream")) // Adjust mime types if needed
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("从多个 .properties 文件导入")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageGroupSelector(groups: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected ?: "请选择语言组",
            onValueChange = {},
            label = { Text("语言组") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groups.forEach { group ->
                DropdownMenuItem(text = { Text(group) }, onClick = { onSelect(group); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.LanguageSelector(label: String, languages: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.weight(1f)) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { lang ->
                DropdownMenuItem(text = { Text(lang) }, onClick = { onSelect(lang); expanded = false })
            }
        }
    }
}

@Composable
fun FilterButtons(selectedFilter: FilterState, onFilterSelected: (FilterState) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterState.values().forEach { filter ->
            val filterName = when (filter) {
                FilterState.ALL -> "总条目"
                FilterState.UNTRANSLATED -> "未翻译"
                FilterState.TRANSLATED -> "已翻译"
                FilterState.MODIFIED -> "已改动"
                FilterState.MISSING -> "缺失"
            }
            Row(
                Modifier
                    .selectable(
                        selected = (filter == selectedFilter), 
                        onClick = { onFilterSelected(filter) }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (filter == selectedFilter), 
                    onClick = { onFilterSelected(filter) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = filterName)
            }
        }
    }
}

@Composable
fun TranslationCard(entry: TranslationEntry, onValueChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.key, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(entry.sourceValue, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = entry.targetValue,
                onValueChange = onValueChange,
                label = { Text("译文") },
                modifier = Modifier.fillMaxWidth(),
                // Disable editing for missing entries until they are added
                enabled = !entry.isMissing
            )
        }
    }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onPrev, enabled = currentPage > 1) { Text("上一页") }
        Spacer(modifier = Modifier.width(16.dp))
        Text("第 $currentPage / $totalPages 页")
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onNext, enabled = currentPage < totalPages) { Text("下一页") }
    }
}
