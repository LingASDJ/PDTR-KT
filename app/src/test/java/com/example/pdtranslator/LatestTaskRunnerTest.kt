package com.example.pdtranslator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LatestTaskRunnerTest {

  @Test
  fun `cancels previous task before launching next one`() = runTest {
    val runner = LatestTaskRunner(this as CoroutineScope)
    val events = mutableListOf<String>()

    runner.launch {
      try {
        delay(100)
        events += "first"
      } finally {
        if (!isActive) events += "first_cancelled"
      }
    }
    runner.launch {
      events += "second"
    }

    advanceUntilIdle()

    assertTrue(events.contains("second"))
    assertFalse(events.contains("first"))
    assertEquals("second", events.last())
  }
}
