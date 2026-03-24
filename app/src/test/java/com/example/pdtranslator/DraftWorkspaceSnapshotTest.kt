package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Properties

class DraftWorkspaceSnapshotTest {

  @Test
  fun `captures all scoped workspace data and created language payloads`() {
    val actorsScope = EditScope(groupName = "actors", sourceLangCode = "base", targetLangCode = "zh-CN")
    val itemsScope = EditScope(groupName = "items", sourceLangCode = "base", targetLangCode = "ja")
    val workspace = ScopedWorkspaceState()
      .withStagedChanges(actorsScope, mapOf("actor.hero" to "英雄"))
      .withStagedDeletions(actorsScope, setOf("actor.villain"))
      .withStagedChanges(itemsScope, mapOf("item.wand" to "魔杖"))
      .withCreatedLanguages("actors", setOf("fr"))
      .withCreatedLanguages("items", setOf("ko"))

    val actorsBase = Properties().apply { setProperty("actor.hero", "Hero") }
    val actorsZh = Properties().apply { setProperty("actor.hero", "英雄") }
    val actorsFr = Properties().apply { setProperty("actor.hero", "Heros") }
    val itemsBase = Properties().apply { setProperty("item.wand", "Wand") }
    val itemsJa = Properties().apply { setProperty("item.wand", "杖") }
    val itemsKo = Properties().apply { setProperty("item.wand", "지팡이") }

    val groups = listOf(
      LanguageGroup(
        name = "actors",
        languages = mapOf(
          "base" to LanguageData("actors.properties", actorsBase),
          "zh-CN" to LanguageData("actors_zh-CN.properties", actorsZh),
          "fr" to LanguageData("actors_fr.properties", actorsFr)
        )
      ),
      LanguageGroup(
        name = "items",
        languages = mapOf(
          "base" to LanguageData("items.properties", itemsBase),
          "ja" to LanguageData("items_ja.properties", itemsJa),
          "ko" to LanguageData("items_ko.properties", itemsKo)
        )
      )
    )

    val snapshot = DraftWorkspaceSnapshot.capture(workspace, groups)

    assertEquals(2, snapshot.scopes.size)
    assertEquals(mapOf("actor.hero" to "英雄"), snapshot.scope(actorsScope)?.stagedChanges)
    assertEquals(setOf("actor.villain"), snapshot.scope(actorsScope)?.stagedDeletions)
    assertEquals(mapOf("item.wand" to "魔杖"), snapshot.scope(itemsScope)?.stagedChanges)

    val actorsCreated = snapshot.createdLanguages("actors")
    val itemsCreated = snapshot.createdLanguages("items")
    assertEquals(setOf("fr"), actorsCreated.keys)
    assertEquals("Heros", actorsCreated["fr"]?.get("actor.hero"))
    assertEquals(setOf("ko"), itemsCreated.keys)
    assertEquals("지팡이", itemsCreated["ko"]?.get("item.wand"))
  }

  @Test
  fun `restores scoped workspace state from draft snapshot`() {
    val actorsScope = EditScope(groupName = "actors", sourceLangCode = "base", targetLangCode = "zh-CN")
    val itemsScope = EditScope(groupName = "items", sourceLangCode = "base", targetLangCode = "ja")
    val snapshot = DraftWorkspaceSnapshot(
      scopes = listOf(
        DraftScopeSnapshot(
          groupName = "actors",
          sourceLangCode = "base",
          targetLangCode = "zh-CN",
          stagedChanges = mapOf("actor.hero" to "英雄"),
          stagedDeletions = setOf("actor.villain")
        ),
        DraftScopeSnapshot(
          groupName = "items",
          sourceLangCode = "base",
          targetLangCode = "ja",
          stagedChanges = mapOf("item.wand" to "魔杖"),
          stagedDeletions = emptySet()
        )
      ),
      createdLanguagesByGroup = listOf(
        DraftCreatedLanguageSnapshot(
          groupName = "actors",
          languages = mapOf("fr" to mapOf("actor.hero" to "Heros"))
        )
      )
    )

    val workspace = snapshot.toScopedWorkspaceState()

    assertEquals(mapOf("actor.hero" to "英雄"), workspace.stagedChanges(actorsScope))
    assertEquals(setOf("actor.villain"), workspace.stagedDeletions(actorsScope))
    assertEquals(mapOf("item.wand" to "魔杖"), workspace.stagedChanges(itemsScope))
    assertEquals(setOf("fr"), workspace.createdLanguages("actors"))
    assertEquals(emptySet<String>(), workspace.createdLanguages("items"))

    val restoredActors = snapshot.createdLanguages("actors")
    assertNotNull(restoredActors["fr"])
    assertEquals("Heros", restoredActors["fr"]?.get("actor.hero"))
  }

  @Test
  fun `converts legacy draft payload into workspace snapshot`() {
    val legacyDraft = DraftData(
      groupName = "actors",
      sourceLangCode = "base",
      targetLangCode = "zh-CN",
      stagedChanges = mapOf("actor.hero" to "英雄"),
      highlightKeywords = setOf("hero"),
      entryCount = 1,
      keysDigest = "digest",
      timestamp = 123L,
      stagedDeletions = setOf("actor.villain"),
      createdLanguages = mapOf("fr" to mapOf("actor.hero" to "Heros"))
    )

    val snapshot = DraftWorkspaceSnapshot.fromDraft(legacyDraft)
    val scope = EditScope(groupName = "actors", sourceLangCode = "base", targetLangCode = "zh-CN")

    assertEquals(mapOf("actor.hero" to "英雄"), snapshot.scope(scope)?.stagedChanges)
    assertEquals(setOf("actor.villain"), snapshot.scope(scope)?.stagedDeletions)
    assertEquals("Heros", snapshot.createdLanguages("actors")["fr"]?.get("actor.hero"))
  }
}
