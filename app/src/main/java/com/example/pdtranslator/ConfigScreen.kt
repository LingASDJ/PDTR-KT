package com.example.pdtranslator

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
  val draftData by viewModel.draftData.collectAsState()
  val draftValidation by viewModel.draftValidation.collectAsState()

  val importMultipleLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
    onResult = { uris: List<Uri> ->
      if (uris.isNotEmpty()) {
        viewModel.loadFilesFromUris(contentResolver, uris)
      }
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

  Column(
    modifier = Modifier
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {

    // Draft Recovery Card
    if (draftData != null) {
      DraftRecoveryCard(
        draft = draftData!!,
        draftValidation = draftValidation,
        onRestore = { viewModel.restoreDraft() },
        onDiscard = { viewModel.discardDraft() }
      )
    }

    // Import Card
    Card {
      Column(Modifier.padding(16.dp)) {
        Text(stringResource(id = R.string.config_import_files), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { importZipLauncher.launch(arrayOf("application/zip")) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(id = R.string.config_import_from_zip), maxLines = 1)
          }
          OutlinedButton(
            onClick = { importMultipleLauncher.launch(arrayOf("text/plain", "application/properties", "application/octet-stream")) },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(id = R.string.config_import_from_properties), maxLines = 1)
          }
        }
      }
    }

    // Language Selection Card
    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        LanguageGroupSelector(languageGroupNames, selectedGroupName, viewModel::selectGroup)
        Spacer(Modifier.height(12.dp))
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

    // Keyword Highlighting Card
    KeywordHighlightingCard(viewModel)

    // Export Button
    Button(
      onClick = {
        val groupName = selectedGroupName ?: "Project"
        val timestamp = System.currentTimeMillis()
        saveZipLauncher.launch("${groupName}_${timestamp}.zip")
      },
      enabled = isSaveEnabled,
      modifier = Modifier.fillMaxWidth()
    ) {
      Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(stringResource(id = R.string.config_export))
    }
  }
}

@Composable
private fun DraftRecoveryCard(
  draft: DraftData,
  draftValidation: DraftValidation,
  onRestore: () -> Unit,
  onDiscard: () -> Unit
) {
  val isMismatch = draftValidation == DraftValidation.MISMATCH
  val context = LocalContext.current

  // Compute time ago
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
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 8.dp)
          )
        }
        Text(
          text = stringResource(id = R.string.draft_found_title),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(Modifier.height(4.dp))

      // Meta info: time ago + changes count
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = timeAgo,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = stringResource(R.string.draft_changes_count, draft.stagedChanges.size),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(Modifier.height(8.dp))

      if (isMismatch) {
        Text(
          text = stringResource(id = R.string.draft_mismatch_warning),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onRestore) {
            Text(stringResource(id = R.string.draft_force_restore))
          }
          Spacer(Modifier.width(8.dp))
          Button(onClick = onDiscard) {
            Text(stringResource(id = R.string.draft_discard))
          }
        }
      } else {
        Text(
          text = stringResource(id = R.string.draft_match_info),
          style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDiscard) {
            Text(stringResource(id = R.string.draft_discard))
          }
          Spacer(Modifier.width(8.dp))
          Button(onClick = onRestore) {
            Text(stringResource(id = R.string.draft_restore))
          }
        }
      }
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
      Text(stringResource(id = R.string.config_keyword_highlighting_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(12.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = newKeyword,
          onValueChange = { newKeyword = it },
          label = { Text(stringResource(id = R.string.config_add_keyword_label)) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = {
          viewModel.addHighlightKeyword(newKeyword)
          newKeyword = ""
        }, enabled = newKeyword.isNotBlank()) {
          Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.config_add_keyword_button))
        }
      }

      if (highlightKeywords.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Divider()
        Spacer(modifier = Modifier.height(12.dp))
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
      modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(text, style = MaterialTheme.typography.labelMedium)
      IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.config_remove_keyword_desc, text), modifier = Modifier.size(16.dp))
      }
    }
  }
}
