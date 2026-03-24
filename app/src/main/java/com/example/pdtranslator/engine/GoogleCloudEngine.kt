package com.example.pdtranslator.engine

import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GoogleCloudEngine(
  private val client: HttpClient,
  private val apiKey: String
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "google_cloud",
      nameResId = R.string.engine_google_cloud,
      isExperimental = false,
      requiresApiKey = true
    )
  }

  override val config = CONFIG

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val body = buildJsonObject {
        put("q", text)
        put("source", mapLang(sourceLang))
        put("target", mapLang(targetLang))
        put("format", "text")
      }

      val response: String = client.post("https://translation.googleapis.com/language/translate/v2?key=$apiKey") {
        contentType(ContentType.Application.Json)
        setBody(body.toString())
      }.body()

      val json = Json.parseToJsonElement(response)
      val translated = json.jsonObject["data"]?.jsonObject?.get("translations")?.jsonArray?.get(0)
        ?.jsonObject?.get("translatedText")?.jsonPrimitive?.content
        ?: return Result.failure(Exception("Unexpected response format"))

      Result.success(TranslationResult(translated, "Google Cloud"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun testConnection(): Result<String> {
    return translate("hello", "en", "zh").map { "OK: ${it.translatedText}" }
  }

  private fun mapLang(lang: String): String {
    return when {
      lang == "base" -> "en"
      lang.startsWith("zh") -> if (lang.contains("TW") || lang.contains("Hant")) "zh-TW" else "zh-CN"
      else -> lang.substringBefore("-")
    }
  }
}
