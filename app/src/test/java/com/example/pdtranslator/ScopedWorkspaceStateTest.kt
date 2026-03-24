package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test

class ScopedWorkspaceStateTest {

  @Test
  fun `stores staged changes independently per edit scope`() {
    val actorsScope = EditScope(groupName = "actors", sourceLangCode = "base", targetLangCode = "zh-CN")
    val itemsScope = EditScope(groupName = "items", sourceLangCode = "base", targetLangCode = "zh-CN")

    val state = ScopedWorkspaceState()
      .withStagedChanges(actorsScope, mapOf("actor.hero" to "英雄"))
      .withStagedChanges(itemsScope, mapOf("item.wand" to "法杖"))

    assertEquals(mapOf("actor.hero" to "英雄"), state.stagedChanges(actorsScope))
    assertEquals(mapOf("item.wand" to "法杖"), state.stagedChanges(itemsScope))
  }

  @Test
  fun `stores created languages independently per group`() {
    val state = ScopedWorkspaceState()
      .withCreatedLanguages("actors", setOf("fr"))
      .withCreatedLanguages("items", setOf("ja"))

    assertEquals(setOf("fr"), state.createdLanguages("actors"))
    assertEquals(setOf("ja"), state.createdLanguages("items"))
    assertEquals(emptySet<String>(), state.createdLanguages("mobs"))
  }
}
