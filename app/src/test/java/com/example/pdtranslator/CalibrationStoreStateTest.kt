package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CalibrationStoreStateTest {

  @Test
  fun `serializes concurrent updates without losing calibration entries`() {
    val state = CalibrationStoreState(CalibrationStore())
    val executor = Executors.newFixedThreadPool(4)
    try {
      repeat(20) { index ->
        executor.submit {
          state.update { store ->
            store.upsert(
              propKey = "key$index",
              originalText = "source$index",
              calibratedText = "fixed$index",
              timestamp = index.toLong()
            )
          }
        }
      }
    } finally {
      executor.shutdown()
      executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    assertEquals(20, state.snapshot().count)
  }
}
