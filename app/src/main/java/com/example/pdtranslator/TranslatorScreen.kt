package com.example.pdtranslator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel, onShowSnackbar: suspend (String) -> Unit) {
    val displayEntries by viewModel.displayEntries.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val infoBarText by viewModel.infoBarText.collectAsState()
    val isSearchCardVisible by viewModel.isSearchCardVisible.collectAsState()
    val missingEntriesCount by viewModel.missingEntriesCount.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest {
            when(it) {
                is UiEvent.ShowSnackbar -> onShowSnackbar(it.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp)) // Top padding

        AnimatedVisibility(visible = isSearchCardVisible) {
            SearchReplaceControls(viewModel)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = infoBarText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (isSearchCardVisible) "Collapse Search" else "Expand Search",
                modifier = Modifier.clickable { viewModel.toggleSearchCardVisibility() }
            )
        }

        FilterButtons(filterState) { viewModel.setFilter(it) }

        if (filterState == FilterState.MISSING) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.fillMissingEntries() },
                    enabled = missingEntriesCount > 0
                ) {
                    Text("补全缺失字段")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayEntries, key = { it.key }) { entry ->
                    NewTranslationCard(
                        entry = entry,
                        onSave = { newText -> viewModel.stageChange(entry.key, newText) },
                        onDiscard = { viewModel.unstageChange(entry.key) }
                    )
                }
            }
            PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
        }

        Spacer(modifier = Modifier.height(4.dp)) // Bottom padding
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchReplaceControls(viewModel: TranslatorViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val isCaseSensitive by viewModel.isCaseSensitive.collectAsState()
    val isExactMatch by viewModel.isExactMatch.collectAsState()

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("搜索") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { viewModel.setReplaceQuery(it) },
                    label = { Text("替换") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.selectable(selected = isCaseSensitive, onClick = { viewModel.setCaseSensitive(!isCaseSensitive) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isCaseSensitive, onCheckedChange = { viewModel.setCaseSensitive(it) })
                    Text("区分大小写")
                }
                Row(
                     Modifier.selectable(selected = isExactMatch, onClick = { viewModel.setExactMatch(!isExactMatch) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isExactMatch, onCheckedChange = { viewModel.setExactMatch(it) })
                    Text("完全匹配")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun NewTranslationCard(
    entry: TranslationEntry,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var currentText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }

    val cardColors = if (entry.isModified) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                if (entry.isIdentical) {
                    Text(
                        text = stringResource(id = R.string.translator_identical_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                 if (entry.isMissing) {
                    Text(
                        text = "(Missing)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.sourceValue, style = MaterialTheme.typography.bodyMedium)
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(id = R.string.common_translation)) },
                    )

                    if (entry.isModified) {
                        IconButton(onClick = onDiscard) {
                            Icon(painter = painterResource(id = R.drawable.ic_discard), contentDescription = "Discard Changes")
                        }
                    } else {
                        IconButton(onClick = { onSave(currentText) }, enabled = currentText != entry.originalTargetValue) {
                            Icon(painter = painterResource(id = R.drawable.ic_save), contentDescription = "Save Changes")
                        }
                    }
                }
            }
        }
    }
}
