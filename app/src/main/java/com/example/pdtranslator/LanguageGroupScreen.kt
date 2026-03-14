package com.example.pdtranslator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageGroupScreen(
    viewModel: TranslatorViewModel,
    onGroupSelected: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val languageGroupNames by viewModel.languageGroupNames

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择语言组") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = padding
        ) {
            if (languageGroupNames.isEmpty()) {
                item {
                    Text(
                        "没有找到语言文件组。请确保文件遵循 'groupName_languageCode.properties' 命名约定。",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(languageGroupNames) { groupName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                viewModel.selectLanguageGroup(groupName)
                                onGroupSelected()
                            }
                    ) {
                        Text(
                            text = groupName,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
