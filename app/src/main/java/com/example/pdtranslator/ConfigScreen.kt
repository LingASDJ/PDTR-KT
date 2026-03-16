package com.example.pdtranslator

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: TranslatorViewModel) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val languageGroupNames by viewModel.languageGroupNames.collectAsState()
    val selectedGroupName by viewModel.selectedGroupName.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val sourceLangCode by viewModel.sourceLangCode.collectAsState()
    val targetLangCode by viewModel.targetLangCode.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()

    // Launcher for single file import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { viewModel.loadFilesFromUris(contentResolver, listOf(it)) } }
    )

    // Launcher for ZIP file import
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { viewModel.loadFilesFromUris(contentResolver, listOf(it)) } }
    )

    // Launcher for saving the ZIP file
    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? -> uri?.let { viewModel.saveChangesToZip(contentResolver, it) } }
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Card {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.config_import_files), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(onClick = { importZipLauncher.launch(arrayOf("application/zip")) }) {
                        Text(stringResource(id = R.string.config_import_from_zip))
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/plain", "application/properties")) }) {
                        Text(stringResource(id = R.string.config_import_from_properties))
                    }
                }
            }
        }

        // Language Selection Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                LanguageGroupSelector(languageGroupNames, selectedGroupName, viewModel::selectGroup)
                Spacer(Modifier.height(16.dp))
                LanguageSelectors(availableLanguages, sourceLangCode, targetLangCode, viewModel::selectSourceLanguage, viewModel::selectTargetLanguage)
            }
        }

        // Keyword Highlighting Card
        KeywordHighlightingCard(viewModel)

        Button(
            onClick = {
                val groupName = selectedGroupName ?: "Project"
                val timestamp = System.currentTimeMillis()
                saveZipLauncher.launch("${groupName}_${timestamp}.zip")
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.config_export))
        }
    }
}

@Composable
private fun KeywordHighlightingCard(viewModel: TranslatorViewModel) {
    val highlightKeywords by viewModel.highlightKeywords.collectAsState()
    var newKeyword by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(id = R.string.config_keyword_highlighting_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(id = R.string.config_keyword_highlighting_subtitle), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKeyword,
                    onValueChange = { newKeyword = it },
                    label = { Text(stringResource(id = R.string.config_add_keyword_label)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.addHighlightKeyword(newKeyword)
                    newKeyword = ""
                }, enabled = newKeyword.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.config_add_keyword_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (highlightKeywords.isNotEmpty()) {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    for (keyword in highlightKeywords) {
                        Chip(keyword) { viewModel.removeHighlightKeyword(keyword) }
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, onClose: () -> Unit) {
    Card(shape = MaterialTheme.shapes.extraLarge) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text, style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = onClose, modifier = Modifier.height(18.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.config_remove_keyword_desc, text), modifier = Modifier.height(18.dp))
            }
        }
    }
}
