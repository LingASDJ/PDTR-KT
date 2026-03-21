package com.example.pdtranslator.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.pdtranslator.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

enum class EngineVerificationState {
  UNTESTED,
  VERIFIED,
  FAILED
}

data class EngineHealthStatus(
  val state: EngineVerificationState,
  val message: String = ""
)

class TranslationEngineManager(private val context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences("translation_engines", Context.MODE_PRIVATE)

  val httpClient: HttpClient by lazy {
    HttpClient(Android) {
      install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
      }
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
          isLenient = true
        })
      }
      engine {
        connectTimeout = 15_000
        socketTimeout = 30_000
      }
    }
  }

  // All available engines: web (free, no key) first, then API engines
  val availableEngines: List<EngineConfig> = listOf(
    GoogleWebEngine.CONFIG,
    BingWebEngine.CONFIG,
    YoudaoWebEngine.CONFIG,
    DeepLXEngine.CONFIG,
    GoogleCloudEngine.CONFIG,
    DeepLEngine.CONFIG,
    BaiduEngine.CONFIG,
    YoudaoApiEngine.CONFIG,
    MicrosoftEngine.CONFIG
  )

  @Volatile
  private var currentEngine: TranslationEngine? = null

  fun getSelectedEngineId(): String {
    val selected = prefs.getString("selected_engine", "") ?: ""
    return if (selected.isBlank() || availableEngines.any { it.id == selected }) selected else ""
  }

  fun setSelectedEngine(engineId: String) {
    prefs.edit().putString("selected_engine", engineId).apply()
    currentEngine = null
  }

  fun getApiKey(engineId: String): String {
    return prefs.getString("api_key_$engineId", "") ?: ""
  }

  fun setApiKey(engineId: String, key: String) {
    prefs.edit().putString("api_key_$engineId", key).apply()
    currentEngine = null
  }

  fun getEndpoint(engineId: String): String {
    return prefs.getString("endpoint_$engineId", "") ?: ""
  }

  fun setEndpoint(engineId: String, endpoint: String) {
    prefs.edit().putString("endpoint_$engineId", endpoint).apply()
    currentEngine = null
  }

  // B-fix1: base language override per group
  fun getBaseLangOverride(groupName: String): String {
    return prefs.getString("base_lang_override_$groupName", "") ?: ""
  }

  fun setBaseLangOverride(groupName: String, langCode: String) {
    prefs.edit().putString("base_lang_override_$groupName", langCode).apply()
  }

  fun getEngine(): TranslationEngine? {
    currentEngine?.let { return it }
    val id = getSelectedEngineId()
    if (id.isBlank()) return null
    val engine = createEngine(id)
    currentEngine = engine
    return engine
  }

  private fun createEngine(id: String): TranslationEngine? {
    val apiKey = getApiKey(id)
    val endpoint = getEndpoint(id)
    return when (id) {
      "google_cloud" -> GoogleCloudEngine(httpClient, apiKey)
      "deepl" -> DeepLEngine(httpClient, apiKey)
      "deeplx" -> DeepLXEngine(httpClient, endpoint)
      "baidu" -> {
        val parts = apiKey.split("|", limit = 2)
        if (parts.size == 2) BaiduEngine(httpClient, parts[0], parts[1]) else null
      }
      "youdao_api" -> {
        val parts = apiKey.split("|", limit = 2)
        if (parts.size == 2) YoudaoApiEngine(httpClient, parts[0], parts[1]) else null
      }
      "microsoft" -> MicrosoftEngine(httpClient, apiKey)
      "google_web" -> GoogleWebEngine(httpClient)
      "youdao_web" -> YoudaoWebEngine(httpClient)
      "bing_web" -> BingWebEngine(httpClient)
      else -> null
    }
  }

  suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<TranslationResult> {
    val engine = getReadyEngineForTranslation().getOrElse { return Result.failure(it) }
    return engine.translate(text, sourceLang, targetLang)
  }

  suspend fun translateBatch(texts: List<String>, sourceLang: String, targetLang: String): Result<List<TranslationResult>> {
    val engine = getReadyEngineForTranslation().getOrElse { return Result.failure(it) }
    return engine.translateBatch(texts, sourceLang, targetLang)
  }

  suspend fun testConnection(): Result<String> {
    val selectedId = getSelectedEngineId()
    val engine = getEngine() ?: return Result.failure(IllegalStateException(context.getString(R.string.engine_not_configured)))
    val result = engine.testConnection()
    if (selectedId.isNotBlank()) {
      if (result.isSuccess) {
        recordEngineHealth(selectedId, EngineVerificationState.VERIFIED, result.getOrThrow())
      } else {
        val message = getFriendlyError(selectedId, result.exceptionOrNull())
        recordEngineHealth(selectedId, EngineVerificationState.FAILED, message)
      }
    }
    return result
  }

  fun getEngineHealthStatus(engineId: String): EngineHealthStatus {
    val state = runCatching {
      EngineVerificationState.valueOf(
        prefs.getString("engine_status_$engineId", EngineVerificationState.UNTESTED.name)
          ?: EngineVerificationState.UNTESTED.name
      )
    }.getOrDefault(EngineVerificationState.UNTESTED)
    val message = prefs.getString("engine_status_msg_$engineId", "") ?: ""
    return EngineHealthStatus(state = state, message = message)
  }

  fun getFriendlyError(engineId: String, error: Throwable?): String {
    if (error == null) return context.getString(R.string.engine_error_unknown)
    val msg = error.message ?: ""

    // Network errors
    if (error is java.net.UnknownHostException || msg.contains("Unable to resolve host")) {
      return context.getString(R.string.engine_error_network)
    }
    if (error is java.net.SocketTimeoutException || msg.contains("timeout", ignoreCase = true)) {
      return context.getString(R.string.engine_error_timeout)
    }
    if (error is java.net.ConnectException || msg.contains("Connection refused")) {
      return context.getString(R.string.engine_error_refused)
    }

    // HTTP errors (response contains HTML instead of JSON)
    if (msg.contains("<html", ignoreCase = true) || msg.contains("Unexpected JSON token") || msg.contains("Expected EOF")) {
      return context.getString(R.string.engine_error_server)
    }

    // Auth errors
    if (msg.contains("401") || msg.contains("Unauthorized") || msg.contains("Invalid API key")) {
      return context.getString(R.string.engine_error_auth)
    }
    if (msg.contains("403") || msg.contains("Forbidden")) {
      return context.getString(R.string.engine_error_forbidden)
    }

    // Engine-specific: missing config
    val config = availableEngines.find { it.id == engineId }
    if (config != null) {
      if (config.requiresApiKey && getApiKey(engineId).isBlank()) {
        return context.getString(R.string.engine_error_no_key)
      }
      if (config.requiresEndpoint && getEndpoint(engineId).isBlank()) {
        return context.getString(R.string.engine_error_no_endpoint)
      }
    }

    // Fallback: truncate raw message
    val clean = msg.take(80).replace(Regex("<[^>]*>"), "").trim()
    return if (clean.isNotBlank()) clean else context.getString(R.string.engine_error_unknown)
  }

  private fun recordEngineHealth(engineId: String, state: EngineVerificationState, message: String) {
    prefs.edit()
      .putString("engine_status_$engineId", state.name)
      .putString("engine_status_msg_$engineId", message)
      .apply()
  }

  private fun getReadyEngineForTranslation(): Result<TranslationEngine> {
    val engine = getEngine() ?: return Result.failure(IllegalStateException(context.getString(R.string.engine_not_configured)))
    val selectedId = getSelectedEngineId()
    if (selectedId.isBlank()) return Result.success(engine)

    val blockedMessage = EngineUsagePolicy.blockedTranslationMessage(
      healthStatus = getEngineHealthStatus(selectedId),
      fallbackMessage = context.getString(R.string.engine_status_failed)
    )
    return if (blockedMessage != null) {
      Result.failure(IllegalStateException(blockedMessage))
    } else {
      Result.success(engine)
    }
  }
}
