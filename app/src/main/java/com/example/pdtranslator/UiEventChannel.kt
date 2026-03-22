package com.example.pdtranslator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class UiEventChannel(
  capacity: Int = Channel.BUFFERED
) {
  private val channel = Channel<UiEvent>(capacity)

  fun send(event: UiEvent): Boolean = channel.trySend(event).isSuccess

  fun receiveAsFlow(): Flow<UiEvent> = channel.receiveAsFlow()

  fun close() {
    channel.close()
  }
}
