package com.example.pdtranslator

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslatorViewModelPaginationStateTest {

  @Test
  fun `setSearchQuery restores previous page when clearing search`() {
    val viewModel = allocateTranslatorViewModel().apply {
      setField("searchQuery", MutableStateFlow(""))
      setField("currentPage", MutableStateFlow(5))
      setField("totalPages", MutableStateFlow(9))
      setField("_currentSearchResultIndex", MutableStateFlow(-1))
      setFieldIfPresent("_pageBeforeSearch", 1)
    }

    viewModel.setSearchQuery("fire")
    assertEquals(1, viewModel.currentPage.value)
    assertEquals(0, viewModel.getIntStateFlow("_currentSearchResultIndex").value)

    viewModel.currentPage.value = 2
    viewModel.setSearchQuery("")

    assertEquals(5, viewModel.currentPage.value)
    assertEquals(-1, viewModel.getIntStateFlow("_currentSearchResultIndex").value)
  }

  @Test
  fun `resetSearchState resets to page 1 for context switches`() {
    val viewModel = allocateTranslatorViewModel().apply {
      setField("searchQuery", MutableStateFlow("fire"))
      setField("replaceQuery", MutableStateFlow("ice"))
      setField("currentPage", MutableStateFlow(2))
      setField("totalPages", MutableStateFlow(6))
      setField("_searchResultKeys", MutableStateFlow(listOf("magic.fire")))
      setField("searchResultCount", MutableStateFlow(1))
      setField("_currentSearchResultIndex", MutableStateFlow(0))
      setField("currentSearchResultKey", MutableStateFlow("magic.fire"))
      setFieldIfPresent("_pageBeforeSearch", 4)
    }

    val method = TranslatorViewModel::class.java.getDeclaredMethod("resetSearchState")
    method.isAccessible = true
    method.invoke(viewModel)

    assertEquals("", viewModel.searchQuery.value)
    assertEquals("", viewModel.replaceQuery.value)
    assertEquals(0, viewModel.searchResultCount.value)
    assertEquals(-1, viewModel.getIntStateFlow("_currentSearchResultIndex").value)
    assertNull(viewModel.currentSearchResultKey.value)
    assertEquals(1, viewModel.currentPage.value)
  }

  @Test
  fun `goToPage clamps lower bound only`() {
    val viewModel = allocateTranslatorViewModel().apply {
      setField("currentPage", MutableStateFlow(3))
      setField("totalPages", MutableStateFlow(6))
    }

    val method = TranslatorViewModel::class.java.getDeclaredMethod("goToPage", Int::class.javaPrimitiveType)
    method.invoke(viewModel, 0)
    assertEquals(1, viewModel.currentPage.value)

    method.invoke(viewModel, 99)
    assertEquals(99, viewModel.currentPage.value)
  }

  @Suppress("UNCHECKED_CAST")
  private fun TranslatorViewModel.getIntStateFlow(fieldName: String): MutableStateFlow<Int> =
    TranslatorViewModel::class.java.getDeclaredField(fieldName).run {
      isAccessible = true
      get(this@getIntStateFlow) as MutableStateFlow<Int>
    }

  private fun TranslatorViewModel.setField(fieldName: String, value: Any?) {
    val field = TranslatorViewModel::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun TranslatorViewModel.setFieldIfPresent(fieldName: String, value: Any?) {
    try {
      setField(fieldName, value)
    } catch (_: NoSuchFieldException) {
    }
  }

  private fun allocateTranslatorViewModel(): TranslatorViewModel {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val field = unsafeClass.getDeclaredField("theUnsafe")
    field.isAccessible = true
    val unsafe = field.get(null)
    val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
    return allocateInstance.invoke(unsafe, TranslatorViewModel::class.java) as TranslatorViewModel
  }
}
