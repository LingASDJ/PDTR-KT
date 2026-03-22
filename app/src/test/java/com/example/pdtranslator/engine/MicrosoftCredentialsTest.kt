package com.example.pdtranslator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MicrosoftCredentialsTest {

  @Test
  fun `parses plain subscription key`() {
    val credentials = MicrosoftCredentials.parse("secret-key")

    assertEquals("secret-key", credentials.apiKey)
    assertNull(credentials.region)
  }

  @Test
  fun `parses subscription key with region`() {
    val credentials = MicrosoftCredentials.parse("secret-key|eastasia")

    assertEquals("secret-key", credentials.apiKey)
    assertEquals("eastasia", credentials.region)
  }
}
