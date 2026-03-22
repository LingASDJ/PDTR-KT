package com.example.pdtranslator.engine

import org.junit.Assert.assertFalse
import org.junit.Test

class EngineConfigTest {

  @Test
  fun `all shipped engines are treated as non experimental`() {
    val configs = listOf(
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

    configs.forEach { config ->
      assertFalse("${config.id} should not be experimental", config.isExperimental)
    }
  }
}
