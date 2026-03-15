
package com.example.pdtranslator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Dependency(
    val name: String,
    val version: String,
    val url: String
)

val dependencies = listOf(
    Dependency("CustomActivityOnCrash", "2.4.0", "https://github.com/Ereza/CustomActivityOnCrash"),
    Dependency("Ktor Client", "2.3.8", "https://github.com/ktorio/ktor"),
    Dependency("AndroidX Navigation Compose", "2.7.7", "https://developer.android.com/jetpack/androidx/releases/navigation"),
    Dependency("AndroidX Core KTX", "1.12.0", "https://developer.android.com/jetpack/androidx/releases/core"),
    Dependency("AndroidX Core Splashscreen", "1.0.1", "https://developer.android.com/jetpack/androidx/releases/core"),
    Dependency("AndroidX Lifecycle", "2.6.2", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    Dependency("AndroidX Activity Compose", "1.8.2", "https://developer.android.com/jetpack/androidx/releases/activity"),
    Dependency("Jetpack Compose", "2023.08.00", "https://developer.android.com/jetpack/compose"),
    Dependency("Accompanist", "0.28.0", "https://github.com/google/accompanist")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependencyScreen(onNavigateUp: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("程序依赖库使用") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(dependencies) { dependency ->
                DependencyItem(dependency = dependency, onClick = { uriHandler.openUri(dependency.url) })
            }
        }
    }
}

@Composable
fun DependencyItem(dependency: Dependency, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = "Open source icon",
            modifier = Modifier.padding(end = 16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dependency.name, fontWeight = FontWeight.Bold)
            Text(text = "版本: ${'$'}{dependency.version}", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text("访问", color = MaterialTheme.colorScheme.primary)
    }
}
