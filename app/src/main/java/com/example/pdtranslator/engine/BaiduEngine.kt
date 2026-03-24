package com.example.pdtranslator.engine

import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

class BaiduEngine(
  private val client: HttpClient,
  private val appId: String,
  private val secretKey: String
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "baidu",
      nameResId = R.string.engine_baidu,
      isExperimental = false,
      requiresApiKey = true  // format: "appId|secretKey"
    )
  }

  override val config = CONFIG

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val salt = System.currentTimeMillis().toString()
      val sign = md5("$appId$text$salt$secretKey")
      val from = mapLang(sourceLang)
      val to = mapLang(targetLang)
      val q = java.net.URLEncoder.encode(text, "UTF-8")

      val response: String = client.get(
        "https://fanyi-api.baidu.com/api/trans/vip/translate?q=$q&from=$from&to=$to&appid=$appId&salt=$salt&sign=$sign"
      ).body()

      val json = Json.parseToJsonElement(response)
      val translated = json.jsonObject["trans_result"]?.jsonArray?.get(0)
        ?.jsonObject?.get("dst")?.jsonPrimitive?.content
        ?: return Result.failure(Exception(json.jsonObject["error_msg"]?.jsonPrimitive?.content ?: "Unknown error"))

      Result.success(TranslationResult(translated, "Baidu"))
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
      lang.startsWith("zh") -> if (lang.contains("TW") || lang.contains("Hant")) "cht" else "zh"
      lang == "ja" -> "jp"
      lang == "ko" -> "kor"
      else -> lang.substringBefore("-")
    }
  }

  private fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}
