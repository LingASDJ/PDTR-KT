package com.example.pdtranslator.engine

import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

class YoudaoApiEngine(
  private val client: HttpClient,
  private val appKey: String,
  private val appSecret: String
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "youdao_api",
      nameResId = R.string.engine_youdao_api,
      isExperimental = false,
      requiresApiKey = true  // format: "appKey|appSecret"
    )
  }

  override val config = CONFIG

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val salt = System.currentTimeMillis().toString()
      val curtime = (System.currentTimeMillis() / 1000).toString()
      val input = if (text.length <= 20) text else "${text.substring(0, 10)}${text.length}${text.substring(text.length - 10)}"
      val signStr = "$appKey$input$salt$curtime$appSecret"
      val sign = sha256(signStr)

      val from = mapLang(sourceLang)
      val to = mapLang(targetLang)

      val formData = "q=${java.net.URLEncoder.encode(text, "UTF-8")}&from=$from&to=$to&appKey=$appKey&salt=$salt&sign=$sign&signType=v3&curtime=$curtime"

      val response: String = client.post("https://openapi.youdao.com/api") {
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(formData)
      }.body()

      val json = Json.parseToJsonElement(response)
      val errorCode = json.jsonObject["errorCode"]?.jsonPrimitive?.content
      if (errorCode != "0") {
        return Result.failure(Exception("Youdao error: $errorCode"))
      }

      val translated = json.jsonObject["translation"]?.jsonArray?.get(0)?.jsonPrimitive?.content
        ?: return Result.failure(Exception("Unexpected response format"))

      Result.success(TranslationResult(translated, "Youdao"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun testConnection(): Result<String> {
    return translate("hello", "en", "zh-CHS").map { "OK: ${it.translatedText}" }
  }

  private fun mapLang(lang: String): String {
    return when {
      lang == "base" -> "en"
      lang.startsWith("zh") -> if (lang.contains("TW") || lang.contains("Hant")) "zh-CHT" else "zh-CHS"
      else -> lang.substringBefore("-")
    }
  }

  private fun sha256(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}
