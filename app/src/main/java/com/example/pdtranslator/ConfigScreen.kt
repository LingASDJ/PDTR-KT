package com.example.pdtranslator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import java.util.Locale

// ─────────────── Shared card section header ───────────────

@Composable
private fun SectionHeader(
  icon: @Composable () -> Unit,
  title: String,
  subtitle: String? = null
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      icon()
    }
    Spacer(Modifier.width(12.dp))
    Column {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

// ─────────────── Config Screen ───────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: TranslatorViewModel, onNavigateToDictionaryPreview: () -> Unit = {}) {
  val context = LocalContext.current
  val contentResolver = context.contentResolver
  val themeColor by viewModel.themeColor.collectAsState()
  val isPD = themeColor == ThemeColor.PIXEL_DUNGEON

  val languageGroupNames by viewModel.languageGroupNames.collectAsState()
  val selectedGroupName by viewModel.selectedGroupName.collectAsState()
  val availableLanguages by viewModel.availableLanguages.collectAsState()
  val sourceLangCode by viewModel.sourceLangCode.collectAsState()
  val targetLangCode by viewModel.targetLangCode.collectAsState()
  val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()
  val draftData by viewModel.draftData.collectAsState()
  val draftValidation by viewModel.draftValidation.collectAsState()
  val dictEntryCount by viewModel.dictEntryCount.collectAsState()
  val dictionaries by viewModel.dictionaries.collectAsState()
  val selectedDictionaryId by viewModel.selectedDictionaryId.collectAsState()
  val selectedDictionaryName by viewModel.selectedDictionaryName.collectAsState()
  val dictionaryCount by viewModel.dictionaryCount.collectAsState()
  val canDeleteDictionary by viewModel.canDeleteDictionary.collectAsState()
  val pendingDictionaryImport by viewModel.pendingDictionaryImport.collectAsState()
  val createLanguageSuccessSerial by viewModel.createLanguageSuccessSerial.collectAsState()
  val selectedEngineId = viewModel.engineManager.getSelectedEngineId()
  val currentLocales = context.resources.configuration.locales
  val currentLocaleTag = if (currentLocales.isEmpty) Locale.getDefault().toLanguageTag() else currentLocales[0].toLanguageTag()
  val languageCodeOptions = remember(availableLanguages, currentLocaleTag) {
    LanguageCodeCatalog.buildOptions(
      LanguageCodeCatalog.suggestedCodes(availableLanguages),
      displayNameProvider = { code -> viewModel.getLanguageDisplayName(code, context) }
    )
  }

  val importMultipleLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
    onResult = { uris: List<Uri> ->
      if (uris.isNotEmpty()) viewModel.loadFilesFromUris(contentResolver, uris)
    }
  )
  val importZipLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri: Uri? -> uri?.let { viewModel.loadFilesFromUris(contentResolver, listOf(it)) } }
  )
  val saveZipLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip"),
    onResult = { uri: Uri? -> uri?.let { viewModel.saveChangesToZip(contentResolver, it) } }
  )
  val importDictionaryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri: Uri? -> uri?.let { viewModel.importDictionaryFromUri(contentResolver, it) } }
  )
  val exportCurrentDictionaryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
    onResult = { uri: Uri? -> uri?.let { viewModel.exportCurrentDictionary(contentResolver, it) } }
  )
  val exportAllDictionariesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip"),
    onResult = { uri: Uri? -> uri?.let { viewModel.exportAllDictionaries(contentResolver, it) } }
  )

  Column(
    modifier = Modifier
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Spacer(Modifier.height(4.dp))

    // ── Draft Recovery ──
    if (draftData != null) {
      DraftRecoveryCard(
        draft = draftData!!,
        draftValidation = draftValidation,
        onRestore = { viewModel.restoreDraft() },
        onDiscard = { viewModel.discardDraft() }
      )
    }

    // ── Import ──
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
          icon = {
            if (isPD) Icon(painterResource(R.drawable.ic_pd_chest), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            else Icon(Icons.Default.Upload, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          },
          title = stringResource(R.string.config_import_files)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          FilledTonalButton(
            onClick = { importZipLauncher.launch(arrayOf("application/zip")) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.FolderZip, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.config_import_from_zip), maxLines = 1)
          }
          FilledTonalButton(
            onClick = { importMultipleLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.config_import_from_properties), maxLines = 1)
          }
        }
      }
    }

    // ── Language Selection ──
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
          icon = {
            if (isPD) Icon(painterResource(R.drawable.ic_pd_key), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            else Icon(Icons.Default.SwapHoriz, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          },
          title = stringResource(R.string.common_language_group)
        )
        LanguageGroupSelector(languageGroupNames, selectedGroupName, viewModel::selectGroup)
        LanguageSelectors(
          availableLanguages = availableLanguages,
          sourceLangCode = sourceLangCode,
          targetLangCode = targetLangCode,
          onSourceSelected = viewModel::selectSourceLanguage,
          onTargetSelected = viewModel::selectTargetLanguage,
          getDisplayName = { code -> viewModel.getLanguageDisplayName(code, context) }
        )
      }
    }

    // ── Base Language Override (only when engine is configured) ──
    if (selectedGroupName != null && !AggregateLanguageGroup.isAllGroup(selectedGroupName) && selectedEngineId.isNotBlank()) {
      BaseLangOverrideCard(
        groupName = selectedGroupName!!,
        viewModel = viewModel,
        languageCodeOptions = languageCodeOptions,
        isPD = isPD
      )
    }

    // ── Export ──
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
          icon = {
            if (isPD) Icon(painterResource(R.drawable.ic_pd_torch), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            else Icon(Icons.Default.SaveAlt, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          },
          title = stringResource(R.string.config_export)
        )
        Button(
          onClick = {
            val groupName = selectedGroupName ?: "Project"
            saveZipLauncher.launch("${groupName}_${System.currentTimeMillis()}.zip")
          },
          enabled = isSaveEnabled,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.config_export))
        }
      }
    }

    // ── Dictionary ──
    DictionaryCard(
      dictionaries = dictionaries,
      selectedDictionaryId = selectedDictionaryId,
      selectedDictionaryName = selectedDictionaryName,
      dictEntryCount = dictEntryCount,
      dictionaryCount = dictionaryCount,
      hasLanguageSelected = sourceLangCode != null && targetLangCode != null,
      onSelectDictionary = viewModel::selectDictionary,
      onCreateDictionary = viewModel::createDictionary,
      onRenameDictionary = viewModel::renameCurrentDictionary,
      onDeleteDictionary = viewModel::deleteCurrentDictionary,
      onSave = { viewModel.saveToDictionary() },
      onApply = { viewModel.applyDictionary() },
      onClear = { viewModel.clearDictionary() },
      onPreview = onNavigateToDictionaryPreview,
      onImport = { importDictionaryLauncher.launch(arrayOf("*/*")) },
      onExportCurrent = { exportCurrentDictionaryLauncher.launch(viewModel.currentDictionaryExportFileName()) },
      onExportAll = { exportAllDictionariesLauncher.launch(viewModel.allDictionariesExportFileName()) },
      canDeleteDictionary = canDeleteDictionary,
      isPD = isPD
    )

    // ── Create Language ──
    CreateLanguageCard(
      hasGroupSelected = selectedGroupName != null && !AggregateLanguageGroup.isAllGroup(selectedGroupName),
      availableLanguages = availableLanguages,
      languageCodeOptions = languageCodeOptions,
      getDisplayName = { code -> viewModel.getLanguageDisplayName(code, context) },
      onCreateLanguage = { langCode, copyFrom -> viewModel.createLanguage(langCode, copyFrom) },
      createSuccessSerial = createLanguageSuccessSerial,
      isPD = isPD
    )

    // ── Keyword Highlighting ──
    KeywordHighlightingCard(viewModel, isPD)

    Spacer(Modifier.height(8.dp))
  }

  pendingDictionaryImport?.let { pendingImport ->
    DictionaryImportConflictDialog(
      importedCount = pendingImport.importedCount,
      conflictNames = pendingImport.conflictNames,
      onDismiss = viewModel::cancelPendingDictionaryImport,
      onConfirm = viewModel::confirmRenameAllDictionaryConflicts
    )
  }
}

// ─────────────── Draft Recovery Card ───────────────

@Composable
private fun DraftRecoveryCard(
  draft: DraftData,
  draftValidation: DraftValidation,
  onRestore: () -> Unit,
  onDiscard: () -> Unit
) {
  val isMismatch = draftValidation == DraftValidation.MISMATCH
  val context = LocalContext.current

  val timeAgo = remember(draft.timestamp) {
    val diff = System.currentTimeMillis() - draft.timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    when {
      days > 0 -> context.getString(R.string.draft_saved_ago, context.getString(R.string.draft_saved_days_ago, days.toInt()))
      hours > 0 -> context.getString(R.string.draft_saved_ago, context.getString(R.string.draft_saved_hours_ago, hours.toInt()))
      minutes > 0 -> context.getString(R.string.draft_saved_ago, context.getString(R.string.draft_saved_minutes_ago, minutes.toInt()))
      else -> context.getString(R.string.draft_saved_ago, context.getString(R.string.draft_saved_minutes_ago, 1))
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = if (isMismatch) {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    }
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isMismatch) {
          Icon(
            Icons.Default.Warning, null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 8.dp)
          )
        }
        Text(
          stringResource(R.string.draft_found_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(Modifier.height(6.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.draft_changes_count, draft.stagedChanges.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(Modifier.height(10.dp))

      Text(
        text = if (isMismatch) stringResource(R.string.draft_mismatch_warning) else stringResource(R.string.draft_match_info),
        style = MaterialTheme.typography.bodySmall,
        color = if (isMismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
      )
      Spacer(Modifier.height(12.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (isMismatch) {
          TextButton(onClick = onRestore) { Text(stringResource(R.string.draft_force_restore)) }
          Spacer(Modifier.width(8.dp))
          Button(onClick = onDiscard) { Text(stringResource(R.string.draft_discard)) }
        } else {
          OutlinedButton(onClick = onDiscard) { Text(stringResource(R.string.draft_discard)) }
          Spacer(Modifier.width(8.dp))
          Button(onClick = onRestore) { Text(stringResource(R.string.draft_restore)) }
        }
      }
    }
  }
}

// ─────────────── Dictionary Card ───────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryCard(
  dictionaries: List<NamedDictionary>,
  selectedDictionaryId: String?,
  selectedDictionaryName: String,
  dictEntryCount: Int,
  dictionaryCount: Int,
  hasLanguageSelected: Boolean,
  onSelectDictionary: (String) -> Unit,
  onCreateDictionary: (String) -> Unit,
  onRenameDictionary: (String) -> Unit,
  onDeleteDictionary: () -> Unit,
  onSave: () -> Unit,
  onApply: () -> Unit,
  onClear: () -> Unit,
  onPreview: () -> Unit,
  onImport: () -> Unit,
  onExportCurrent: () -> Unit,
  onExportAll: () -> Unit,
  canDeleteDictionary: Boolean,
  isPD: Boolean = false
) {
  var expanded by remember { mutableStateOf(false) }
  var dictionaryNameInput by remember(selectedDictionaryId, selectedDictionaryName) {
    mutableStateOf(selectedDictionaryName)
  }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionHeader(
        icon = {
          if (isPD) Icon(painterResource(R.drawable.ic_pd_book), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          else Icon(Icons.Default.Book, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        title = stringResource(R.string.dict_title),
        subtitle = stringResource(R.string.dict_subtitle)
      )

      if (dictionaries.isNotEmpty()) {
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = !expanded }
        ) {
          OutlinedTextField(
            readOnly = true,
            value = dictionaries.firstOrNull { it.id == selectedDictionaryId }?.name ?: selectedDictionaryName,
            onValueChange = {},
            label = { Text(stringResource(R.string.dict_select_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
          )
          ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            dictionaries.forEach { dictionary ->
              DropdownMenuItem(
                text = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(dictionary.name)
                    Text(
                      text = stringResource(R.string.dict_count_short, dictionary.entryCount),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                },
                onClick = {
                  onSelectDictionary(dictionary.id)
                  expanded = false
                }
              )
            }
          }
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            stringResource(R.string.dict_count, dictEntryCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Box(
          modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            stringResource(R.string.dict_total, dictionaryCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      OutlinedTextField(
        value = dictionaryNameInput,
        onValueChange = { dictionaryNameInput = it },
        label = { Text(stringResource(R.string.dict_name_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { onCreateDictionary(dictionaryNameInput) },
          enabled = dictionaryNameInput.isNotBlank(),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Add, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.dict_create), maxLines = 1)
        }
        OutlinedButton(
          onClick = { onRenameDictionary(dictionaryNameInput) },
          enabled = selectedDictionaryId != null &&
            dictionaryNameInput.isNotBlank() &&
            dictionaryNameInput.trim() != selectedDictionaryName,
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(R.string.dict_rename), maxLines = 1)
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onSave, enabled = hasLanguageSelected, modifier = Modifier.weight(1f)) {
          Text(stringResource(R.string.dict_save), maxLines = 1)
        }
        OutlinedButton(onClick = onApply, enabled = hasLanguageSelected && dictEntryCount > 0, modifier = Modifier.weight(1f)) {
          Text(stringResource(R.string.dict_apply), maxLines = 1)
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPreview, enabled = dictEntryCount > 0, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.RemoveRedEye, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.dict_preview), maxLines = 1)
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.Upload, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.dict_import), maxLines = 1)
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onExportCurrent, enabled = dictEntryCount > 0, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.SaveAlt, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.dict_export_current), maxLines = 1)
        }
        OutlinedButton(onClick = onExportAll, enabled = dictionaryCount > 0, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.ImportExport, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.dict_export_all), maxLines = 1)
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onDeleteDictionary,
          enabled = canDeleteDictionary,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
          Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.dict_delete), maxLines = 1)
        }
        OutlinedButton(
          onClick = onClear,
          enabled = dictEntryCount > 0,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
          Text(stringResource(R.string.dict_clear), maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun DictionaryImportConflictDialog(
  importedCount: Int,
  conflictNames: List<String>,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  val previewNames = conflictNames.take(5).joinToString(separator = "\n") { "• $it" }
  val overflowCount = (conflictNames.size - 5).coerceAtLeast(0)
  val overflowLabel = if (overflowCount > 0) {
    stringResource(R.string.dict_import_conflict_more, overflowCount)
  } else {
    ""
  }
  val details = buildString {
    if (previewNames.isNotBlank()) {
      append(previewNames)
    }
    if (overflowLabel.isNotBlank()) {
      if (isNotBlank()) append('\n')
      append(overflowLabel)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.dict_import_conflict_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.dict_import_conflict_message, importedCount, conflictNames.size))
        if (details.isNotBlank()) {
          Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    },
    confirmButton = {
      Button(onClick = onConfirm) {
        Text(stringResource(R.string.dict_import_rename_all))
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text(stringResource(R.string.dict_import_cancel))
      }
    }
  )
}

// ─────────────── Create Language Card ───────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateLanguageCard(
  hasGroupSelected: Boolean,
  availableLanguages: List<String>,
  languageCodeOptions: List<LanguageCodeOption>,
  getDisplayName: (String) -> String,
  onCreateLanguage: (String, String?) -> Unit,
  createSuccessSerial: Long,
  isPD: Boolean = false
) {
  var langCode by remember { mutableStateOf("") }
  var copyFromLang by remember { mutableStateOf<String?>(null) }
  var copyDropdownExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(createSuccessSerial) {
    if (createSuccessSerial > 0) {
      langCode = ""
      copyFromLang = null
      copyDropdownExpanded = false
    }
  }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionHeader(
        icon = {
          if (isPD) Icon(painterResource(R.drawable.ic_pd_wand), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          else Icon(Icons.Default.Language, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        title = stringResource(R.string.create_lang_title),
        subtitle = stringResource(R.string.create_lang_subtitle)
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        FilterableLanguageCodeField(
          value = langCode,
          onValueChange = { langCode = it.trim() },
          label = stringResource(R.string.create_lang_code_label),
          placeholder = stringResource(R.string.create_lang_code_placeholder),
          options = languageCodeOptions,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = {
            onCreateLanguage(langCode, copyFromLang)
          },
          enabled = hasGroupSelected && langCode.isNotBlank()
        ) {
          Icon(Icons.Default.Add, null)
        }
      }

      if (availableLanguages.isNotEmpty()) {
        ExposedDropdownMenuBox(
          expanded = copyDropdownExpanded,
          onExpandedChange = { copyDropdownExpanded = !copyDropdownExpanded }
        ) {
          OutlinedTextField(
            readOnly = true,
            value = copyFromLang?.let { "${getDisplayName(it)} ($it)" }
              ?: stringResource(R.string.create_lang_copy_none),
            onValueChange = {},
            label = { Text(stringResource(R.string.create_lang_copy_from)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = copyDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
          )
          ExposedDropdownMenu(expanded = copyDropdownExpanded, onDismissRequest = { copyDropdownExpanded = false }) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.create_lang_copy_none)) },
              onClick = { copyFromLang = null; copyDropdownExpanded = false }
            )
            availableLanguages.forEach { lang ->
              DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(getDisplayName(lang))
                    Text(lang, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                },
                onClick = { copyFromLang = lang; copyDropdownExpanded = false }
              )
            }
          }
        }
      }
    }
  }
}

// ─────────────── Keyword Highlighting Card ───────────────

@Composable
private fun KeywordHighlightingCard(viewModel: TranslatorViewModel, isPD: Boolean = false) {
  val highlightKeywords by viewModel.highlightKeywords.collectAsState()
  var newKeyword by remember { mutableStateOf("") }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionHeader(
        icon = {
          if (isPD) Icon(painterResource(R.drawable.ic_pd_amulet), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          else Icon(Icons.Default.Highlight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        title = stringResource(R.string.config_keyword_highlighting_title),
        subtitle = stringResource(R.string.config_keyword_highlighting_subtitle)
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = newKeyword,
          onValueChange = { newKeyword = it },
          label = { Text(stringResource(R.string.config_add_keyword_label)) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = { viewModel.addHighlightKeyword(newKeyword); newKeyword = "" },
          enabled = newKeyword.isNotBlank()
        ) {
          Icon(Icons.Default.Add, stringResource(R.string.config_add_keyword_button))
        }
      }

      if (highlightKeywords.isNotEmpty()) {
        Divider(Modifier.padding(vertical = 2.dp))
        FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
          for (keyword in highlightKeywords) {
            Chip(keyword) { viewModel.removeHighlightKeyword(keyword) }
          }
        }
      }
    }
  }
}

// ─────────────── Base Language Override Card ───────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaseLangOverrideCard(
  groupName: String,
  viewModel: TranslatorViewModel,
  languageCodeOptions: List<LanguageCodeOption>,
  isPD: Boolean = false
) {
  var overrideLang by remember(groupName) {
    mutableStateOf(viewModel.engineManager.getBaseLangOverride(groupName))
  }

  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SectionHeader(
        icon = {
          if (isPD) Icon(painterResource(R.drawable.ic_pd_wand), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
          else Icon(Icons.Default.Translate, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        title = stringResource(R.string.engine_base_lang_override),
        subtitle = stringResource(R.string.engine_base_lang_override_hint)
      )
      FilterableLanguageCodeField(
        value = overrideLang,
        onValueChange = {
          overrideLang = it
          viewModel.engineManager.setBaseLangOverride(groupName, it.trim())
        },
        label = stringResource(R.string.engine_base_lang_override),
        placeholder = stringResource(R.string.engine_base_lang_override_placeholder),
        options = languageCodeOptions,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun EngineFailedWarningCard(
  message: String,
  isPD: Boolean = false
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SectionHeader(
        icon = {
          if (isPD) Icon(painterResource(R.drawable.ic_pd_skull), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
          else Icon(Icons.Default.Warning, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
        },
        title = stringResource(R.string.engine_status_failed),
        subtitle = if (message.isBlank()) null else message
      )
    }
  }
}

// ─────────────── Chip ───────────────

@Composable
fun Chip(text: String, onClose: () -> Unit) {
  Card(shape = MaterialTheme.shapes.extraLarge) {
    Row(
      modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(text, style = MaterialTheme.typography.labelMedium)
      IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
        Icon(Icons.Default.Close, stringResource(R.string.config_remove_keyword_desc, text), Modifier.size(16.dp))
      }
    }
  }
}
