package com.example.pdtranslator.translators

import com.example.pdtranslator.TranslationService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GoogleTranslator : TranslationService {

    private val client = HttpClient(CIO)

    override suspend fun translate(text: String, from: String, to: String): String {
        if (text.isBlank()) return ""

        val url = "https://translate.google.com/translate_a/single?client=gtx&sl=$from&tl=$to&dt=t&q=${text.encodeURL()}}”

        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.get(url) {
                    header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    header("Accept-Encoding", "gzip, deflate")
                    header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7,zh-TW;q=0.6,ko;q=0.5,ja;q=0.4")
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                }
                val responseBody = response.bodyAsText()
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
