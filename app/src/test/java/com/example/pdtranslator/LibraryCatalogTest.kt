package com.example.pdtranslator

import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCatalogTest {

  @Test
  fun `license catalog includes networking and serialization dependencies in use`() {
    val resIds = LibraryCatalog.libraries.map { it.nameResId }.toSet()

    assertTrue(resIds.contains(R.string.lib_ktor_name))
    assertTrue(resIds.contains(R.string.lib_gson_name))
    assertTrue(resIds.contains(R.string.lib_kotlinx_serialization_name))
    assertTrue(resIds.contains(R.string.lib_kotlinx_coroutines_name))
    assertTrue(resIds.contains(R.string.lib_material3_name))
  }
}
