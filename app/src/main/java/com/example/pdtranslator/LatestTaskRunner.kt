package com.example.pdtranslator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LatestTaskRunner(private val scope: CoroutineScope) {
  private var currentJob: Job? = null

  fun launch(block: suspend CoroutineScope.() -> Unit): Job {
    currentJob?.cancel()
    return scope.launch(block = block).also { currentJob = it }
  }

  fun cancel() {
    currentJob?.cancel()
    currentJob = null
  }
}
