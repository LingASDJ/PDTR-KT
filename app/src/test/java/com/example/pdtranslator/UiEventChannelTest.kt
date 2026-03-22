package com.example.pdtranslator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiEventChannelTest {

  @Test
  fun `send succeeds while channel is open`() {
    val channel = UiEventChannel()

    assertTrue(channel.send(UiEvent.ShowSnackbar("hello")))
  }

  @Test
  fun `send returns false instead of throwing after close`() {
    val channel = UiEventChannel()
    channel.close()

    assertFalse(channel.send(UiEvent.ShowSnackbar("hello")))
  }
}
