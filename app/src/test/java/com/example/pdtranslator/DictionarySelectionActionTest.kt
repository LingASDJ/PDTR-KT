package com.example.pdtranslator

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class DictionarySelectionActionTest {

  @Test
  fun `runs dictionary selection persistence on provided io dispatcher before refresh`() = runTest {
    val ioDispatcher = StandardTestDispatcher(testScheduler)
    val events = mutableListOf<String>()
    var observedDispatcher: CoroutineDispatcher? = null

    DictionarySelectionAction.selectPersisted(
      id = "boss_terms",
      ioDispatcher = ioDispatcher,
      persistSelection = { id ->
        events += "persist:$id"
        observedDispatcher = currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
      },
      onSelectionApplied = {
        events += "refresh"
      }
    )

    assertSame(ioDispatcher, observedDispatcher)
    assertEquals(listOf("persist:boss_terms", "refresh"), events)
  }
}
