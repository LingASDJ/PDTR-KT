package com.example.pdtranslator.engine

import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class BingWebEngine(
  private val client: HttpClient
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "bing_web",
      nameResId = R.string.engine_bing_web,
      isExperimental = true,
      requiresApiKey = false
    )
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
  }

  override val config = CONFIG

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val from = mapLang(sourceLang)
      val to = mapLang(targetLang)

      // Step 1: Fetch translator page to get IG + IID + token
      val (pageHtml, baseHost) = fetchTranslatorPage()

      val igMatch = Regex("""IG:"([a-zA-Z0-9]+)"""").find(pageHtml)
      val ig = igMatch?.groupValues?.get(1)
        ?: return Result.failure(Exception("Cannot obtain session token"))

      // Extract IID from page
      val iidMatch = Regex("""data-iid="([^"]+)"""").find(pageHtml)
      val iid = iidMatch?.groupValues?.get(1) ?: "translator.5023"

      // Extract token/key from params_AbusePreventionHelper
      val tokenMatch = Regex("""\[(\d+),"([^"]+)",(\d+)]""").find(pageHtml)
      val token = tokenMatch?.groupValues?.get(2) ?: ""
      val key = tokenMatch?.groupValues?.get(1) ?: ""

      // Step 2: Call ttranslatev3
      val formData = buildString {
        append("fromLang=$from")
        append("&text=${java.net.URLEncoder.encode(text, "UTF-8")}")
        append("&to=$to")
        if (token.isNotBlank()) append("&token=${java.net.URLEncoder.encode(token, "UTF-8")}")
        if (key.isNotBlank()) append("&key=$key")
      }

      val response: String = client.post("https://$baseHost/ttranslatev3?isVertical=1&IG=$ig&IID=$iid") {
        header("User-Agent", UA)
        header("Referer", "https://$baseHost/translator")
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(formData)
      }.body()

      val trimmed = response.trimStart()
      if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
        return Result.failure(Exception("Server returned non-JSON response"))
      }

      val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response)

      // Response can be JsonArray or JsonObject depending on endpoint version
      val translated = EngineJsonParser.extractBingWebTranslatedText(json)

      if (translated.isNullOrBlank()) {
        return Result.failure(Exception("Unexpected response format"))
      }

      Result.success(TranslationResult(translated, "Bing Web"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun fetchTranslatorPage(): Pair<String, String> {
    val hosts = listOf("cn.bing.com", "www.bing.com")
    for (host in hosts) {
      try {
        val html: String = client.get("https://$host/translator") {
          header("User-Agent", UA)
        }.body()
        if (html.contains("IG:\"") || html.contains("IG: \"")) {
          return Pair(html, host)
        }
      } catch (_: Exception) {}
    }
    throw Exception("Cannot access Bing Translator")
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
