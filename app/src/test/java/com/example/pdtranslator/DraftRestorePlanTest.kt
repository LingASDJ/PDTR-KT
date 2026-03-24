package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftRestorePlanTest {

  @Test
  fun `clears active language selection before restoring draft group`() {
    val draft = DraftData(
      groupName = "actors",
      sourceLangCode = "base",
      targetLangCode = "zh-CN",
      stagedChanges = mapOf("actor.hero" to "英雄"),
      highlightKeywords = emptySet(),
      entryCount = 1,
      keysDigest = "digest",
      timestamp = 1L
    )

    val plan = DraftRestorePlan.fromDraft(draft)

    assertEquals("actors", plan.groupName)
    assertNull(plan.preselectedSourceLangCode)
    assertNull(plan.preselectedTargetLangCode)
    assertEquals("base", plan.finalSourceLangCode)
    assertEquals("zh-CN", plan.finalTargetLangCode)
  }
}
