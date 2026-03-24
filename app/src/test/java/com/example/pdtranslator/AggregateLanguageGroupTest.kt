package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Properties

class AggregateLanguageGroupTest {

  @Test
  fun `prepends all option to group selector values`() {
    assertEquals(
      listOf(AggregateLanguageGroup.ALL_GROUP_NAME, "actors", "items"),
      AggregateLanguageGroup.groupOptions(listOf("actors", "items"))
    )
  }

  @Test
  fun `uses only common languages for all group availability`() {
    val groups = listOf(
      LanguageGroup(
        name = "actors",
        languages = mapOf(
          "base" to LanguageData("actors.properties", Properties()),
          "zh-CN" to LanguageData("actors_zh-CN.properties", Properties()),
          "fr" to LanguageData("actors_fr.properties", Properties())
        )
      ),
      LanguageGroup(
        name = "items",
        languages = mapOf(
          "base" to LanguageData("items.properties", Properties()),
          "zh-CN" to LanguageData("items_zh-CN.properties", Properties())
        )
      )
    )

    assertEquals(
      listOf("base", "zh-CN"),
      AggregateLanguageGroup.availableLanguages(groups, AggregateLanguageGroup.ALL_GROUP_NAME)
    )
  }
}
