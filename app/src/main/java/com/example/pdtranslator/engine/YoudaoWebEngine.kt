package com.example.pdtranslator.engine

import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.util.UUID

class YoudaoWebEngine(
  private val client: HttpClient
) : TranslationEngine {

  companion object {
    val CONFIG = EngineConfig(
      id = "youdao_web",
      nameResId = R.string.engine_youdao_web,
      isExperimental = false,
      requiresApiKey = false
    )

    private const val UA =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
    private const val REFERER = "https://fanyi.youdao.com/"
    private const val ORIGIN = "https://fanyi.youdao.com"
    private const val KEY_ENDPOINT = "https://dict.youdao.com/webtranslate/key"
    private const val TRANSLATE_ENDPOINT = "https://dict.youdao.com/webtranslate"
    private const val DICT_ENDPOINT = "https://dict.youdao.com/jsonapi_s?doctype=json&jsonversion=4"

    private const val KEY_FETCH_ID = "webfanyi-key-getter-2025"
    private const val KEY_FETCH_SECRET = "yU5nT5dK3eZ1pI4j"
    private const val WEBTRANSLATE_KEY_ID = "webfanyi"

    private const val DICT_CLIENT = "webmain"
    private const val DICT_KEYFROM = "webfanyi.webmain"
    private const val DICT_SECRET = "t2he2k4m2g6QKRigK0KAmSpXKgAezywG"

    private const val SESSION_TTL_MS = 5 * 60 * 1000L
  }

  override val config = CONFIG

  private val json = Json { ignoreUnknownKeys = true }

  @Volatile
  private var cachedSession: KeySession? = null

  override suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    return try {
      val normalizedText = text.trim()
      if (normalizedText.isBlank()) {
        return Result.failure(IllegalArgumentException("Text is blank"))
      }

      val attempts = YoudaoWebLanguagePolicy.buildAttemptPairs(sourceLang, targetLang)
      val primaryPair = attempts.firstOrNull() ?: LangPair(
        YoudaoWebLanguagePolicy.mapLang(sourceLang),
        YoudaoWebLanguagePolicy.mapLang(targetLang)
      )
      var lastFailureMessage: String? = null
      var sawUnsupportedPair = false

      for (pair in attempts) {
        val primary = translateViaWebtranslate(normalizedText, pair.from, pair.to, forceRefresh = false)
        val pairAttempts = if (primary.translation.isBlank()) {
          listOf(
            primary,
            translateViaWebtranslate(normalizedText, pair.from, pair.to, forceRefresh = true)
          )
        } else {
          listOf(primary)
        }

        val success = pairAttempts.firstOrNull { it.translation.isNotBlank() }
        if (success != null) {
          return Result.success(TranslationResult(success.translation, "Youdao Web"))
        }

        if (pairAttempts.any { YoudaoWebLanguagePolicy.isErrorCode50(it.message) }) {
          sawUnsupportedPair = true
        }
        lastFailureMessage = pairAttempts.lastOrNull { !it.message.isNullOrBlank() }?.message ?: lastFailureMessage
      }

      if (shouldUseDictionaryFallback(normalizedText, primaryPair.from, primaryPair.to)) {
        val dictionaryTranslation = translateViaDictionary(normalizedText)
        if (dictionaryTranslation.isSuccess) {
          return dictionaryTranslation
        }
      }

      val message = if (sawUnsupportedPair) {
        YoudaoWebLanguagePolicy.buildUnsupportedMessage(sourceLang, targetLang)
      } else {
        lastFailureMessage ?: "Youdao webtranslate returned no translation"
      }
      Result.failure(Exception(message))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun testConnection(): Result<String> {
    return translate("hello", "en", "zh-CHS").map { "OK: ${it.translatedText}" }
  }

  private suspend fun translateViaWebtranslate(
    text: String,
    from: String,
    to: String,
    forceRefresh: Boolean
  ): TranslationAttempt {
    return try {
      val session = getSession(forceRefresh)
      val requestParams = LinkedHashMap<String, String>().apply {
        put("i", text)
        put("from", from)
        put("to", to)
        put("dictResult", "true")
        put("keyid", WEBTRANSLATE_KEY_ID)
        put("noCheckPrivate", "false")
        putAll(
          YoudaoWebCodec.buildSignedParams(
            secret = session.secretKey,
            keyId = WEBTRANSLATE_KEY_ID,
            mysticTime = System.currentTimeMillis(),
            yduuid = session.yduuid
          )
        )
      }

      val encryptedResponse: String = client.post(TRANSLATE_ENDPOINT) {
        header("User-Agent", UA)
        header("Referer", REFERER)
        header("Origin", ORIGIN)
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(FormDataContent(parametersOf(requestParams)))
      }.body()

      val decrypted = YoudaoWebCodec.decryptPayload(
        payload = encryptedResponse.trim(),
        aesKeySeed = session.aesKey,
        aesIvSeed = session.aesIv
      )
      val code = YoudaoWebCodec.extractResponseCode(decrypted)
      val translation = if (code == null || code == 0) {
        YoudaoWebCodec.extractTranslation(decrypted)
      } else {
        ""
      }

      TranslationAttempt(
        translation = translation,
        message = when {
          translation.isNotBlank() -> null
          code != null -> "Youdao webtranslate error code: $code"
          else -> "Youdao webtranslate returned an empty payload"
        }
      )
    } catch (e: Exception) {
      TranslationAttempt(message = e.message ?: "Youdao webtranslate request failed")
    }
  }

  private suspend fun translateViaDictionary(text: String): Result<TranslationResult> {
    return try {
      val timestamp = System.currentTimeMillis().toString()
      val suffix = ((text + DICT_KEYFROM).length % 10).toString()
      val t = timestamp + suffix
      val qHash = md5Hex(text + DICT_KEYFROM)
      val sign = md5Hex(DICT_CLIENT + text + t + DICT_SECRET + qHash)

      val response: String = client.post(DICT_ENDPOINT) {
        header("User-Agent", UA)
        header("Referer", REFERER)
        header("Origin", ORIGIN)
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(
          FormDataContent(
            Parameters.build {
              append("q", text)
              append("t", t)
              append("client", DICT_CLIENT)
              append("sign", sign)
              append("keyfrom", DICT_KEYFROM)
            }
          )
        )
      }.body()

      val translation = YoudaoWebCodec.extractDictionaryTranslation(response)
      if (translation.isBlank()) {
        Result.failure(Exception("Youdao dictionary returned no translation"))
      } else {
        Result.success(TranslationResult(translation, "Youdao Web"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun getSession(forceRefresh: Boolean): KeySession {
    val cached = cachedSession
    val now = System.currentTimeMillis()
    if (!forceRefresh && cached != null && cached.expiresAt > now) {
      return cached
    }

    val yduuid = UUID.randomUUID().toString().replace("-", "")
    val params = YoudaoWebCodec.buildSignedParams(
      secret = KEY_FETCH_SECRET,
      keyId = KEY_FETCH_ID,
      mysticTime = now,
      yduuid = yduuid
    )

    val response: String = client.get(KEY_ENDPOINT) {
      header("User-Agent", UA)
      header("Referer", REFERER)
      header("Origin", ORIGIN)
      url {
        params.forEach { (key, value) -> parameters.append(key, value) }
      }
    }.body()

    val root = json.parseToJsonElement(response).jsonObject
    val code = root["code"]?.jsonPrimitive?.content?.toIntOrNull()
    if (code != 0) {
      throw Exception("Youdao key fetch error: ${root["msg"]?.jsonPrimitive?.content ?: code}")
    }

    val data = root["data"]?.jsonObject ?: throw Exception("Youdao key fetch returned no key data")
    val session = KeySession(
      secretKey = data["secretKey"]?.jsonPrimitive?.content.orEmpty(),
      aesKey = data["aesKey"]?.jsonPrimitive?.content.orEmpty(),
      aesIv = data["aesIv"]?.jsonPrimitive?.content.orEmpty(),
      yduuid = yduuid,
      expiresAt = now + SESSION_TTL_MS
    )

    if (session.secretKey.isBlank() || session.aesKey.isBlank() || session.aesIv.isBlank()) {
      throw Exception("Youdao key fetch returned incomplete credentials")
    }

    cachedSession = session
    return session
  }

  private fun shouldUseDictionaryFallback(text: String, from: String, to: String): Boolean {
    return from == "en" &&
      to.startsWith("zh") &&
      text.matches(Regex("^[A-Za-z][A-Za-z'\\-]*$"))
  }

  private fun md5Hex(input: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun parametersOf(values: Map<String, String>): Parameters =
    Parameters.build { values.forEach { (key, value) -> append(key, value) } }

  private data class KeySession(
    val secretKey: String,
    val aesKey: String,
    val aesIv: String,
    val yduuid: String,
    val expiresAt: Long
  )

  private data class TranslationAttempt(
    val translation: String = "",
    val message: String? = null
  )
}
