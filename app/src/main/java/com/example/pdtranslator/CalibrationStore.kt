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

    fun diff(incoming: Map<String, CalibrationEntry>): CalibrationDiffResult {
        val onlyIncoming = mutableMapOf<String, CalibrationEntry>()
        val onlyLocal = mutableMapOf<String, CalibrationEntry>()
        val conflicts = mutableMapOf<String, Pair<CalibrationEntry, CalibrationEntry>>()
        val identical = mutableSetOf<String>()

        for ((key, incomingEntry) in incoming) {
            val localEntry = entries[key]
            if (localEntry == null) {
                onlyIncoming[key] = incomingEntry
            } else if (localEntry.calibratedText == incomingEntry.calibratedText) {
                identical += key
            } else {
                conflicts[key] = localEntry to incomingEntry
            }
        }
        for (key in entries.keys) {
            if (key !in incoming) {
                onlyLocal[key] = entries[key]!!
            }
        }
        return CalibrationDiffResult(onlyIncoming, onlyLocal, conflicts, identical)
    }

    fun mergeSelected(
        incoming: Map<String, CalibrationEntry>,
        selectedKeys: Set<String>
    ): CalibrationStore {
        val toMerge = incoming.filterKeys { it in selectedKeys }
        return copy(entries = LinkedHashMap(entries).apply { putAll(toMerge) })
    }
}

data class CalibrationDiffResult(
    val onlyIncoming: Map<String, CalibrationEntry>,
    val onlyLocal: Map<String, CalibrationEntry>,
    val conflicts: Map<String, Pair<CalibrationEntry, CalibrationEntry>>,
    val identical: Set<String>
)
