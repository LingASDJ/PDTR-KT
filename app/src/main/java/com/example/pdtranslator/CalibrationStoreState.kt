package com.example.pdtranslator

class CalibrationStoreState(initialStore: CalibrationStore = CalibrationStore()) {
  private val lock = Any()
  private var currentStore: CalibrationStore = initialStore

  fun snapshot(): CalibrationStore = synchronized(lock) { currentStore }

  fun replace(newStore: CalibrationStore) {
    synchronized(lock) {
      currentStore = newStore
    }
  }

  fun update(transform: (CalibrationStore) -> CalibrationStore): CalibrationStore {
    return synchronized(lock) {
      currentStore = transform(currentStore)
      currentStore
    }
  }
}
