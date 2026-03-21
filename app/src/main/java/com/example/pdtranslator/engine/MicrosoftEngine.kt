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
import kotlinx.serialization.json.put

class MicrosoftEngine(
  private val client: HttpClient,
  private val apiKey: String
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "microsoft",
      nameResId = R.string.engine_microsoft,
      isExperimental = false,
      requiresApiKey = true
    )
  }

  override val config = CONFIG

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val from = mapLang(sourceLang)
      val to = mapLang(targetLang)

      val body = buildJsonArray {
        add(buildJsonObject { put("Text", text) })
      }

      val response: String = client.post("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=$from&to=$to") {
        header("Ocp-Apim-Subscription-Key", apiKey)
        contentType(ContentType.parse("application/json; charset=UTF-8"))
        setBody(body.toString())
      }.body()

      val json = Json.parseToJsonElement(response)
      val translated = EngineJsonParser.extractMicrosoftTranslatedText(json)
        ?: return Result.failure(Exception("Unexpected response format"))

      Result.success(TranslationResult(translated, "Microsoft"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun testConnection(): Result<String> {
    return translate("hello", "en", "zh-Hans").map { "OK: ${it.translatedText}" }
  }

  private fun mapLang(lang: String): String {
    return when {
      lang == "base" -> "en"
      lang.startsWith("zh") -> if (lang.contains("TW") || lang.contains("Hant")) "zh-Hant" else "zh-Hans"
      else -> lang.substringBefore("-")
    }
  }
}
