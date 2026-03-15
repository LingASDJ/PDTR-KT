package com.example.pdtranslator.translators

import com.example.pdtranslator.translators.TranslationService
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GoogleTranslator : TranslationService {

    override val name: String = "Google Translate"

    private val client = HttpClient(Android)

    override suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (text.isBlank()) return ""

        val url = "https://translate.google.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=${text.encodeURL()}"

        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.get(url)
                val responseBody = response.bodyAsText(Charsets.UTF_8)
                parseGoogleTranslateResponse(responseBody)
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }

    private fun parseGoogleTranslateResponse(responseBody: String): String {
        try {
            val jsonArray = JSONArray(responseBody)
            val firstArray = jsonArray.getJSONArray(0)
            val result = StringBuilder()
            for (i in 0 until firstArray.length()) {
                val innerArray = firstArray.getJSONArray(i)
                result.append(innerArray.getString(0))
            }
            return result.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error parsing response"
        }
    }
}

private fun String.encodeURL(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}
