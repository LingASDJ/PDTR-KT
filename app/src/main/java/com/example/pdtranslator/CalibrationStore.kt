package com.example.pdtranslator

data class CalibrationStore(
    val entries: Map<String, CalibrationEntry> = emptyMap()
) {
    val count: Int get() = entries.size

    fun get(propKey: String): CalibrationEntry? = entries[propKey]

    fun upsert(
        propKey: String,
        originalText: String,
        calibratedText: String,
        timestamp: Long = System.currentTimeMillis()
    ): CalibrationStore {
        val preservedOriginalText = entries[propKey]?.originalText ?: originalText
        return put(
            propKey,
            CalibrationEntry(
                originalText = preservedOriginalText,
                calibratedText = calibratedText,
                timestamp = timestamp
            )
        )
    }

    fun put(propKey: String, entry: CalibrationEntry): CalibrationStore {
        return copy(entries = LinkedHashMap(entries).apply { put(propKey, entry) })
    }

    fun remove(propKey: String): CalibrationStore {
        if (propKey !in entries) return this
        return copy(entries = LinkedHashMap(entries).apply { remove(propKey) })
    }

    fun clear(): CalibrationStore = copy(entries = emptyMap())

    fun merge(incoming: Map<String, CalibrationEntry>): CalibrationStore {
        return copy(entries = LinkedHashMap(entries).apply { putAll(incoming) })
    }
}
