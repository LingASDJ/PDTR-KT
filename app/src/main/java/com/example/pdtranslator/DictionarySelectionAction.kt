package com.example.pdtranslator

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun selectDictionaryPersisted(
  id: String,
  ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  persistSelection: suspend (String) -> Unit,
  onSelectionApplied: () -> Unit
) {
  withContext(ioDispatcher) {
    persistSelection(id)
  }
  onSelectionApplied()
}
