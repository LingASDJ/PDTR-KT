package com.example.pdtranslator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: TranslatorViewModel) {
    val context = LocalContext.current

    // State collected from the ViewModel for configuration
    val languageGroupNames by viewModel.languageGroupNames.collectAsState()
    val selectedGroupName by viewModel.selectedGroupName.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val sourceLangCode by viewModel.sourceLangCode.collectAsState()
    val targetLangCode by viewModel.targetLangCode.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()

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
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        // Language Group Selector
        LanguageGroupSelector(languageGroupNames, selectedGroupName) { viewModel.selectGroup(it) }

        // Language Selectors
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LanguageSelector("源语言", availableLanguages, sourceLangCode) { viewModel.selectSourceLanguage(it) }
            LanguageSelector("目标语言", availableLanguages, targetLangCode) { viewModel.selectTargetLanguage(it) }
        }
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
