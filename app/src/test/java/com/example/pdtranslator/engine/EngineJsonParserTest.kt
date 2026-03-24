package com.example.pdtranslator.engine

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EngineJsonParserTest {

  @Test
  fun `google parser returns null for empty payload`() {
    assertNull(EngineJsonParser.extractGoogleWebTranslatedText(Json.parseToJsonElement("[]")))
  }

  @Test
  fun `google parser joins translated fragments`() {
    val payload = """[[["你好","hello",null,null,1],["世界","world",null,null,1]],null,"en"]"""

    assertEquals("你好世界", EngineJsonParser.extractGoogleWebTranslatedText(Json.parseToJsonElement(payload)))
  }

  @Test
  fun `microsoft parser returns null for empty arrays`() {
    assertNull(EngineJsonParser.extractMicrosoftTranslatedText(Json.parseToJsonElement("[]")))
    assertNull(EngineJsonParser.extractMicrosoftTranslatedText(Json.parseToJsonElement("""[{"translations":[]}]""")))
  }

  @Test
  fun `microsoft parser extracts first translation text`() {
    val payload = """[{"translations":[{"text":"你好"}]}]"""

    assertEquals("你好", EngineJsonParser.extractMicrosoftTranslatedText(Json.parseToJsonElement(payload)))
  }

  @Test
  fun `bing parser returns null for empty variants`() {
    assertNull(EngineJsonParser.extractBingWebTranslatedText(Json.parseToJsonElement("[]")))
    assertNull(EngineJsonParser.extractBingWebTranslatedText(Json.parseToJsonElement("""{"translations":[]}""")))
  }

  @Test
  fun `bing parser extracts translation from direct array response`() {
    val payload = """[{"translations":[{"text":"你好"}]}]"""

    assertEquals("你好", EngineJsonParser.extractBingWebTranslatedText(Json.parseToJsonElement(payload)))
  }
}
