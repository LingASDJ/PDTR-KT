package com.example.pdtranslator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterButtons(selectedFilter: FilterState, onFilterSelected: (FilterState) -> Unit) {
  val scrollState = rememberScrollState()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FilterState.values().forEach { state ->
      val textId = when (state) {
        FilterState.ALL -> R.string.filter_all
        FilterState.UNTRANSLATED -> R.string.filter_untranslated
        FilterState.TRANSLATED -> R.string.filter_translated
        FilterState.MODIFIED -> R.string.filter_modified
        FilterState.MISSING -> R.string.filter_missing
        FilterState.DIFF -> R.string.filter_diff
        FilterState.DELETED -> R.string.filter_deleted
      }
      val isSelected = state == selectedFilter
      FilterChip(
        selected = isSelected,
        onClick = { onFilterSelected(state) },
        label = { Text(stringResource(id = textId)) },
        leadingIcon = if (isSelected) {
          { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageGroupSelector(
  groupNames: List<String>,
  selectedGroupName: String?,
  onGroupSelected: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
    OutlinedTextField(
      readOnly = true,
      value = selectedGroupName ?: stringResource(id = R.string.common_select_language_group),
      onValueChange = {},
      label = { Text(stringResource(id = R.string.common_language_group)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(),
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      groupNames.forEach { name ->
        DropdownMenuItem(text = { Text(name) }, onClick = { onGroupSelected(name); expanded = false })
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectors(
  availableLanguages: List<String>,
  sourceLangCode: String?,
  targetLangCode: String?,
  onSourceSelected: (String) -> Unit,
  onTargetSelected: (String) -> Unit,
  getDisplayName: (String) -> String
) {
  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(modifier = Modifier.weight(1f)) {
      LanguageSelector(availableLanguages.filter { it != targetLangCode }, sourceLangCode, stringResource(id = R.string.config_source_language), onSourceSelected, getDisplayName)
    }
    Box(modifier = Modifier.weight(1f)) {
      LanguageSelector(availableLanguages.filter { it != sourceLangCode }, targetLangCode, stringResource(id = R.string.config_target_language), onTargetSelected, getDisplayName)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
  languages: List<String>,
  selectedLanguage: String?,
  label: String,
  onLanguageSelected: (String) -> Unit,
  getDisplayName: (String) -> String
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedText = selectedLanguage?.let { getDisplayName(it) } ?: ""

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
    OutlinedTextField(
      readOnly = true,
      value = selectedText,
      onValueChange = {},
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(),
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      languages.forEach { lang ->
        DropdownMenuItem(
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(getDisplayName(lang))
              Text(
                text = lang,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = { onLanguageSelected(lang); expanded = false }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterableLanguageCodeField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  options: List<LanguageCodeOption>,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  var expanded by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val filteredOptions = remember(value, options) {
    LanguageCodeCatalog.filter(options, value).take(12)
  }

  ExposedDropdownMenuBox(
    expanded = expanded && filteredOptions.isNotEmpty(),
    onExpandedChange = { expanded = !expanded }
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = {
        onValueChange(it)
        expanded = true
      },
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filteredOptions.isNotEmpty()) },
      modifier = modifier
        .fillMaxWidth()
        .menuAnchor(),
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
      singleLine = true,
      enabled = enabled
    )
    ExposedDropdownMenu(
      expanded = expanded && filteredOptions.isNotEmpty(),
      onDismissRequest = { expanded = false }
    ) {
      filteredOptions.forEach { option ->
        DropdownMenuItem(
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(option.code)
              Text(
                text = option.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = {
            onValueChange(option.code)
            expanded = false
            focusManager.clearFocus()
          }
        )
      }
    }
  }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    IconButton(onClick = onPrevious, enabled = currentPage > 1) {
      Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(id = R.string.pagination_previous))
    }
    Text(
      text = stringResource(id = R.string.pagination_page_info, currentPage, totalPages),
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )
    IconButton(onClick = onNext, enabled = currentPage < totalPages) {
      Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.pagination_next))
    }
  }
}
