package com.example.pdtranslator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryPreviewScreen(
  viewModel: TranslatorViewModel,
  onNavigateUp: () -> Unit
) {
  val themeColor by viewModel.themeColor.collectAsState()
  val isPD = themeColor == ThemeColor.PIXEL_DUNGEON

  val dictionaryName by viewModel.selectedDictionaryName.collectAsState()
  val totalCount by viewModel.dictEntryCount.collectAsState()
  val query by viewModel.dictionaryPreviewQuery.collectAsState()
  val allEntries by viewModel.dictionaryPreviewEntries.collectAsState()
  val currentTab by viewModel.dictionaryPreviewTab.collectAsState()

  // Load preview data on enter
  LaunchedEffect(Unit) {
    viewModel.showDictionaryPreview()
  }

  // Clean up on exit
  DisposableEffect(Unit) {
    onDispose {
      viewModel.hideDictionaryPreview()
    }
  }

  // Split entries by reviewed status
  val pendingEntries = remember(allEntries) { allEntries.filter { !it.reviewed } }
  val reviewedEntries = remember(allEntries) { allEntries.filter { it.reviewed } }

  val displayedEntries = if (currentTab == 0) pendingEntries else reviewedEntries

  val formatter = remember {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.dict_preview_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (query.isBlank()) "$dictionaryName · $totalCount"
                     else "$dictionaryName · ${allEntries.size} / $totalCount",
              style = MaterialTheme.typography.bodySmall,
              color = if (isPD) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
          }
        },
        colors = if (isPD) {
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        } else {
          TopAppBarDefaults.topAppBarColors()
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Spacer(Modifier.size(4.dp))

      // Search field
      OutlinedTextField(
        value = query,
        onValueChange = viewModel::setDictionaryPreviewQuery,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.dict_preview_search_label)) },
        singleLine = true
      )

      // Tab row: 待校对 | 已校对
      TabRow(selectedTabIndex = currentTab) {
        Tab(
          selected = currentTab == 0,
          onClick = { viewModel.setDictionaryPreviewTab(0) },
          text = {
            Text("${stringResource(R.string.dict_preview_tab_pending)} (${pendingEntries.size})")
          }
        )
        Tab(
          selected = currentTab == 1,
          onClick = { viewModel.setDictionaryPreviewTab(1) },
          text = {
            Text("${stringResource(R.string.dict_preview_tab_reviewed)} (${reviewedEntries.size})")
          }
        )
      }

      // Entry list
      if (displayedEntries.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = stringResource(R.string.dict_preview_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(displayedEntries, key = { it.rawKey }) { entry ->
            DictionaryPreviewEntryCard(
              entry = entry,
              formatter = formatter,
              isPD = isPD,
              currentTab = currentTab,
              onSaveEntry = viewModel::updateDictionaryPreviewEntry,
              onReview = viewModel::reviewDictionaryEntry,
              onUnreview = viewModel::unreviewDictionaryEntry
            )
          }
          item { Spacer(Modifier.size(8.dp)) }
        }
      }
    }
  }
}

@Composable
private fun DictionaryPreviewEntryCard(
  entry: DictionaryPreviewItem,
  formatter: DateFormat,
  isPD: Boolean,
  currentTab: Int,
  onSaveEntry: (String, String, String) -> Unit,
  onReview: (String) -> Unit,
  onUnreview: (String) -> Unit
) {
  val context = LocalContext.current
  var isEditing by remember(entry.rawKey) { mutableStateOf(false) }
  var editedSource by remember(entry.rawKey, entry.sourceText) { mutableStateOf(entry.sourceText.orEmpty()) }
  var editedTranslation by remember(entry.rawKey, entry.translation) { mutableStateOf(entry.translation) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Key + metadata badges
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = entry.propKey,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = if (isPD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f, fill = false)
        )
        if (entry.langPair != null) {
          DpsBadge(
            text = entry.langPair,
            containerColor = if (isPD) MaterialTheme.colorScheme.tertiaryContainer
                             else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isPD) MaterialTheme.colorScheme.onTertiaryContainer
                           else MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }

      if (entry.groupName != null) {
        Text(
          text = entry.groupName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (isEditing) {
        OutlinedTextField(
          value = editedSource,
          onValueChange = { editedSource = it },
          label = { Text(stringResource(R.string.dict_preview_source_label)) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
        OutlinedTextField(
          value = editedTranslation,
          onValueChange = { editedTranslation = it },
          label = { Text(stringResource(R.string.dict_preview_translation_label)) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = { copyTextToClipboard(context, entry.propKey, editedTranslation) },
            enabled = editedTranslation.isNotBlank()
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.common_copy))
          }
          Spacer(Modifier.width(8.dp))
          OutlinedButton(onClick = {
            editedSource = entry.sourceText.orEmpty()
            editedTranslation = entry.translation
            isEditing = false
          }) {
            Text(stringResource(R.string.common_cancel))
          }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              onSaveEntry(entry.rawKey, editedSource, editedTranslation)
              isEditing = false
            },
            enabled = editedTranslation.isNotBlank()
          ) {
            Text(stringResource(R.string.common_save))
          }
        }
      } else {
        // Source text
        if (!entry.sourceText.isNullOrBlank()) {
          Row(verticalAlignment = Alignment.Top) {
            DpsBadge(
              text = stringResource(R.string.dict_preview_source_label),
              containerColor = if (isPD) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              contentColor = if (isPD) MaterialTheme.colorScheme.onPrimaryContainer
                             else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = entry.sourceText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Translation
        Row(verticalAlignment = Alignment.Top) {
          DpsBadge(
            text = stringResource(R.string.dict_preview_translation_label),
            containerColor = if (isPD) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            contentColor = if (isPD) MaterialTheme.colorScheme.onSecondaryContainer
                           else MaterialTheme.colorScheme.tertiary
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = entry.translation,
            style = MaterialTheme.typography.bodyMedium
          )
        }

        // Action buttons row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Copy button
          OutlinedButton(onClick = { copyTextToClipboard(context, entry.propKey, entry.translation) }) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.common_copy))
          }
          Spacer(Modifier.width(8.dp))

          // Review / Unreview button depending on tab
          if (currentTab == 0) {
            OutlinedButton(onClick = { onReview(entry.rawKey) }) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(stringResource(R.string.dict_preview_review_btn))
            }
            Spacer(Modifier.width(8.dp))
          } else {
            OutlinedButton(onClick = { onUnreview(entry.rawKey) }) {
              Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(stringResource(R.string.dict_preview_unreview_btn))
            }
            Spacer(Modifier.width(8.dp))
          }

          // Edit button
          Button(onClick = { isEditing = true }) {
            Text(stringResource(R.string.common_edit))
          }
        }
      }

      // Timestamp
      Text(
        text = formatter.format(Date(entry.timestamp)),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun DpsBadge(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
  Box(
    modifier = Modifier
      .clip(MaterialTheme.shapes.extraSmall)
      .background(containerColor)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = contentColor
    )
  }
}
