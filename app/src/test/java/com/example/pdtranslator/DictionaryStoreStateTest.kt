package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DictionaryStoreStateTest {

  @Test
  fun `serializes concurrent updates without losing dictionaries`() {
    val state = DictionaryStoreState(DictionaryStore.empty())
    val executor = Executors.newFixedThreadPool(4)
    try {
      repeat(20) { index ->
        executor.submit {
          state.update { store ->
            store.createDictionary("Dict $index")
          }
        }
      }
    } finally {
      executor.shutdown()
      executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    assertEquals(21, state.snapshot().dictionaries.size)
  }
}
