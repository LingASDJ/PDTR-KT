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

class DeepLXEngine(
  private val client: HttpClient,
  private val endpoint: String = ""
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "deeplx",
      nameResId = R.string.engine_deeplx,
      isExperimental = false,
      requiresApiKey = false,
      requiresEndpoint = true
    )

    private val PATHS = listOf("/translate", "/v1/translate", "/v2/translate")
  }

  override val config = CONFIG

  private val json = Json { ignoreUnknownKeys = true }

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    val baseUrl = endpoint.trim().trimEnd('/')
    if (baseUrl.isBlank()) {
      return Result.failure(Exception("DeepLX now requires a custom endpoint"))
    }

    val body = buildJsonObject {
      put("text", text)
      put("source_lang", mapLang(sourceLang))
      put("target_lang", mapLang(targetLang))
    }.toString()

    var lastError: Throwable? = null
    for (path in PATHS) {
      val result = tryTranslate(baseUrl + path, body)
      if (result.isSuccess) return result
      lastError = result.exceptionOrNull()
    }

    return Result.failure(lastError ?: Exception("DeepLX endpoint returned no usable response"))
  }

  override suspend fun testConnection(): Result<String> {
    return translate("hello", "EN", "ZH").map { "OK: ${it.translatedText}" }
  }

  private suspend fun tryTranslate(url: String, bodyJson: String): Result<TranslationResult> {
    return try {
      val response: String = client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(bodyJson)
      }.body()

      val root = json.parseToJsonElement(response).jsonObject
      val translated = root["data"]?.jsonPrimitive?.content
        ?: root["translation"]?.jsonPrimitive?.content
        ?: root["translatedText"]?.jsonPrimitive?.content
        ?: root["result"]?.jsonPrimitive?.content
        ?: root["alternatives"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content

      if (!translated.isNullOrBlank()) {
        return Result.success(TranslationResult(translated, "DeepLX"))
      }

      val code = root["code"]?.jsonPrimitive?.content
      val message = root["message"]?.jsonPrimitive?.content
        ?: root["msg"]?.jsonPrimitive?.content
      val errorMessage = when {
        code == "401" -> "DeepLX endpoint requires authorization or is not a public instance"
        !message.isNullOrBlank() && !code.isNullOrBlank() -> "DeepLX error $code: $message"
        !message.isNullOrBlank() -> "DeepLX error: $message"
        else -> "DeepLX endpoint returned an unexpected response"
      }
      Result.failure(Exception(errorMessage))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun mapLang(lang: String): String {
    return when {
      lang == "base" -> "EN"
      lang.startsWith("zh") -> "ZH"
      else -> lang.substringBefore("-").uppercase()
    }
  }
}
