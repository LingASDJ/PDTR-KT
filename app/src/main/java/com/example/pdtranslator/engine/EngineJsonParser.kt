package com.example.pdtranslator.engine

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object EngineJsonParser {

  fun extractGoogleWebTranslatedText(json: JsonElement): String? {
    val sentences = json.asArrayOrNull()?.getOrNull(0).asArrayOrNull() ?: return null
    val translated = sentences.mapNotNull { sentence ->
      sentence.asArrayOrNull()?.getOrNull(0).asStringOrNull()
    }.joinToString("")
    return translated.ifBlank { null }
  }

  fun extractMicrosoftTranslatedText(json: JsonElement): String? {
    return json.asArrayOrNull()
      ?.getOrNull(0)
      .asObjectOrNull()
      ?.get("translations")
      .asArrayOrNull()
      ?.getOrNull(0)
      .asObjectOrNull()
      ?.get("text")
      .asStringOrNull()
  }

  fun extractBingWebTranslatedText(json: JsonElement): String? {
    return when (json) {
      is JsonArray -> extractBingArrayTranslation(json)
      is JsonObject -> extractBingObjectTranslation(json)
      else -> null
    }
  }

  private fun extractBingArrayTranslation(array: JsonArray): String? {
    return array.getOrNull(0)
      .asObjectOrNull()
      ?.get("translations")
      .asArrayOrNull()
      ?.getOrNull(0)
      .asObjectOrNull()
      ?.get("text")
      .asStringOrNull()
  }

  private fun extractBingObjectTranslation(obj: JsonObject): String? {
    return obj["translations"]
      .asArrayOrNull()
      ?.getOrNull(0)
      .asObjectOrNull()
      ?.get("text")
      .asStringOrNull()
      ?: obj.values.asSequence()
        .mapNotNull { value -> value.asArrayOrNull()?.let(::extractBingArrayTranslation) }
        .firstOrNull()
  }

  private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

  private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

  private fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.content
}
