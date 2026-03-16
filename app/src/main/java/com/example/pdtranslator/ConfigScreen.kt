package com.example.pdtranslator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen(viewModel: TranslatorViewModel) {
    val context = LocalContext.current

    val languageGroupNames by viewModel.languageGroupNames.collectAsState()
    val selectedGroupName by viewModel.selectedGroupName.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val sourceLangCode by viewModel.sourceLangCode.collectAsState()
    val targetLangCode by viewModel.targetLangCode.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.loadFilesFromUris(context.contentResolver, listOf(it)) } }
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

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { showImportSheet = true }) {
                    Text(stringResource(id = R.string.config_import_files))
                }
                Button(
                    onClick = { saveFileLauncher.launch("translation_output.zip") },
                    enabled = isSaveEnabled
                ) {
                    Text(stringResource(id = R.string.config_export))
                }
            }
        }

        item { LanguageGroupSelector(languageGroupNames, selectedGroupName, onSelect = { viewModel.selectGroup(it) }) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LanguageSelector(stringResource(id = R.string.config_source_language), availableLanguages, sourceLangCode, onSelect = { viewModel.selectSourceLanguage(it) })
                LanguageSelector(stringResource(id = R.string.config_target_language), availableLanguages, targetLangCode, onSelect = { viewModel.selectTargetLanguage(it) })
            }
        }

        item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { KeywordHighlightSection(viewModel) }
    }

    if (showImportSheet) {
        ModalBottomSheet(onDismissRequest = { showImportSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.config_import_dialog_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button({
                    showImportSheet = false
                    zipPickerLauncher.launch(arrayOf("application/zip"))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.config_import_from_zip))
                }
                Spacer(Modifier.height(8.dp))
                Button({
                    showImportSheet = false
                    multipleFilesPickerLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.config_import_from_properties))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun KeywordHighlightSection(viewModel: TranslatorViewModel) {
    val keywords by viewModel.highlightKeywords.collectAsState()
    var newKeyword by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.config_keyword_highlighting_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(id = R.string.config_keyword_highlighting_subtitle),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it },
                label = { Text(stringResource(id = R.string.config_add_keyword_label)) },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.addHighlightKeyword(newKeyword)
                    newKeyword = ""
                },
                enabled = newKeyword.isNotBlank()
            ) {
                Text(stringResource(id = R.string.config_add_keyword_button))
            }
        }

        if (keywords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keywords.forEach { keyword ->
                    InputChip(
                        selected = false,
                        onClick = { /* Not needed */ },
                        label = { Text(keyword) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.config_remove_keyword_desc, keyword),
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable { viewModel.removeHighlightKeyword(keyword) }
                            )
                        }
                    )
                }
            }
        }
    }
}
