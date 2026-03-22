package com.example.pdtranslator

class DictionaryStoreState(initialStore: DictionaryStore) {
  private val lock = Any()
  private var currentStore: DictionaryStore = initialStore

  fun snapshot(): DictionaryStore = synchronized(lock) { currentStore }

  fun replace(newStore: DictionaryStore) {
    synchronized(lock) {
      currentStore = newStore
    }
  }

  fun update(transform: (DictionaryStore) -> DictionaryStore): DictionaryStore {
    return synchronized(lock) {
      currentStore = transform(currentStore)
      currentStore
    }
  }
}
