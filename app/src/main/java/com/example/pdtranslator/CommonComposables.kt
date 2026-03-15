package com.example.pdtranslator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp

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
