package com.example.pdtranslator.engine

import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class DeepLEngine(
  private val client: HttpClient,
  private val apiKey: String
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "deepl",
      nameResId = R.string.engine_deepl,
      isExperimental = false,
      requiresApiKey = true
    )
  }

  override val config = CONFIG

  private val baseUrl: String
    get() = if (apiKey.endsWith(":fx")) "https://api-free.deepl.com" else "https://api.deepl.com"

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val body = buildJsonObject {
        put("text", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(text)) })
        put("source_lang", mapLang(sourceLang))
        put("target_lang", mapTargetLang(targetLang))
      }

      val response: String = client.post("$baseUrl/v2/translate") {
        header("Authorization", "DeepL-Auth-Key $apiKey")
        contentType(ContentType.Application.Json)
        setBody(body.toString())
      }.body()

      val json = Json.parseToJsonElement(response)
      val translated = json.jsonObject["translations"]?.jsonArray?.get(0)
        ?.jsonObject?.get("text")?.jsonPrimitive?.content
        ?: return Result.failure(Exception("Unexpected response format"))

      Result.success(TranslationResult(translated, "DeepL"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun testConnection(): Result<String> {
    return translate("hello", "en", "zh").map { "OK: ${it.translatedText}" }
  }

  private fun mapLang(lang: String): String {
    return when {
      lang == "base" -> "EN"
      lang.startsWith("zh") -> "ZH"
      lang.startsWith("pt") -> "PT"
      else -> lang.substringBefore("-").uppercase()
    }
  }

  private fun mapTargetLang(lang: String): String {
    return when {
      lang == "base" -> "EN-US"
      lang.startsWith("zh") -> if (lang.contains("TW") || lang.contains("Hant")) "ZH-HANT" else "ZH-HANS"
      lang == "en" -> "EN-US"
      lang.startsWith("pt") -> if (lang.contains("BR")) "PT-BR" else "PT-PT"
      else -> lang.substringBefore("-").uppercase()
    }
  }
}
