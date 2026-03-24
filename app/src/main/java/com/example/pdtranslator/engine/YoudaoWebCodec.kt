package com.example.pdtranslator.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object YoudaoWebCodec {
  private val json = Json { ignoreUnknownKeys = true }

  fun buildSignedParams(
    secret: String,
    keyId: String,
    mysticTime: Long,
    yduuid: String
  ): Map<String, String> {
    val time = mysticTime.toString()
    return linkedMapOf(
      "client" to "fanyideskweb",
      "product" to "webfanyi",
      "appVersion" to "12.0.0",
      "vendor" to "web",
      "pointParam" to "client,mysticTime,product",
      "mysticTime" to time,
      "keyfrom" to "fanyi.web",
      "mid" to "1",
      "screen" to "1",
      "model" to "1",
      "network" to "wifi",
      "abtest" to "0",
      "yduuid" to yduuid,
      "keyid" to keyId,
      "sign" to md5Hex("client=fanyideskweb&mysticTime=$time&product=webfanyi&key=$secret")
    )
  }

  fun decryptPayload(payload: String, aesKeySeed: String, aesIvSeed: String): String {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = SecretKeySpec(md5Bytes(aesKeySeed).copyOf(16), "AES")
    val iv = IvParameterSpec(md5Bytes(aesIvSeed).copyOf(16))
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    return cipher.doFinal(decodeBase64Url(payload)).toString(Charsets.UTF_8)
  }

  fun extractTranslation(jsonText: String): String {
    val root = json.parseToJsonElement(jsonText).jsonObject
    val translateResult = root["translateResult"] as? JsonArray ?: return ""
    return buildString {
      translateResult.forEach { paragraph ->
        (paragraph as? JsonArray)?.forEach { sentence ->
          val text = sentence.jsonObject["tgt"]?.jsonPrimitive?.content.orEmpty()
          append(text)
        }
      }
    }.trim()
  }

  fun extractResponseCode(jsonText: String): Int? {
    val root = json.parseToJsonElement(jsonText).jsonObject
    return root["code"]?.jsonPrimitive?.content?.toIntOrNull()
  }

  fun extractDictionaryTranslation(jsonText: String): String {
    val root = json.parseToJsonElement(jsonText).jsonObject
    val preferred = extractEcTranslation(root)
      ?: extractIndividualTranslation(root)
      ?: return ""
    return normalizeDictionaryGloss(preferred)
  }

  fun normalizeDictionaryGloss(value: String): String {
    val normalized = value.trim()
      .substringBefore('；')
      .substringBefore(';')
      .substringBefore('，')
      .substringBefore(',')
      .trim()
    return normalized.ifBlank { value.trim() }
  }

  private fun extractEcTranslation(root: JsonObject): String? {
    val ec = root["ec"] as? JsonObject ?: return null
    val word = ec["word"] ?: return null
    return when (word) {
      is JsonObject -> firstTran(word["trs"])
      is JsonArray -> word.firstOrNull()?.jsonObject?.get("trs")?.let(::firstTran)
      else -> null
    }
  }

  private fun extractIndividualTranslation(root: JsonObject): String? {
    val individual = root["individual"] as? JsonObject ?: return null
    return firstTran(individual["trs"])
  }

  private fun firstTran(element: JsonElement?): String? {
    val first = (element as? JsonArray)?.firstOrNull() ?: return null
    return first.jsonObject["tran"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
  }

  private fun md5Hex(input: String): String =
    md5Bytes(input).joinToString("") { "%02x".format(it) }

  private fun md5Bytes(input: String): ByteArray =
    MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))

  private fun decodeBase64Url(value: String): ByteArray {
    val normalized = buildString {
      append(value.replace('-', '+').replace('_', '/'))
      while (length % 4 != 0) append('=')
    }
    return try {
      java.util.Base64.getDecoder().decode(normalized)
    } catch (_: Throwable) {
      android.util.Base64.decode(normalized, android.util.Base64.DEFAULT)
    }
  }
}
