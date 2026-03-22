package com.example.pdtranslator

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Library(
  val name: String,
  val description: String,
  val license: String,
  val version: String,
  val url: String
)

@Composable
fun getLibraries(): List<Library> {
  return LibraryCatalog.libraries.map { spec ->
    Library(
      name = stringResource(spec.nameResId),
      description = stringResource(spec.descriptionResId),
      license = spec.license,
      version = spec.version,
      url = spec.url
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependencyScreen(onNavigateUp: () -> Unit) {
  val libraries = getLibraries()
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.source_code_license_title)) },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(id = R.string.back_button_description)
            )
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      items(libraries) { library ->
        DependencyItem(library = library)
      }
    }
  }
}

@Composable
fun DependencyItem(library: Library) {
  val context = LocalContext.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(library.url))
        context.startActivity(intent)
      }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(library.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
      Text(
        library.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        "${library.license} - v${library.version}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    IconButton(onClick = {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(library.url))
      context.startActivity(intent)
    }) {
      Icon(Icons.Default.Link, contentDescription = stringResource(id = R.string.visit_link_description))
    }
  }
}
