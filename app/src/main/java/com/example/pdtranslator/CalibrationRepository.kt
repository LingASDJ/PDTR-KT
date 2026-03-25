package com.example.pdtranslator

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CalibrationRepository(private val baseDir: File) {
    private val gson = Gson()
    private val file: File get() = File(baseDir, "calibrations.json")

    fun load(): CalibrationStore {
        if (!file.exists()) return CalibrationStore()
        return try {
            val json = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<Map<String, CalibrationEntry>>() {}.type
            val map: Map<String, CalibrationEntry> = gson.fromJson(json, type) ?: emptyMap()
            CalibrationStore(LinkedHashMap(map))
        } catch (_: Exception) {
            CalibrationStore()
        }
    }

    fun save(store: CalibrationStore) {
        baseDir.mkdirs()
        file.writeText(gson.toJson(store.entries), Charsets.UTF_8)
    }

    fun exportJson(store: CalibrationStore): ByteArray {
        return gson.toJson(store.entries).toByteArray(Charsets.UTF_8)
    }

    fun importContent(content: String): Map<String, CalibrationEntry> {
        val normalized = content.trimStart('\uFEFF').trim()
        if (normalized.isBlank()) return emptyMap()
        return if (normalized.startsWith("{")) importJson(normalized) else importCsv(normalized)
    }

    fun importJson(json: String): Map<String, CalibrationEntry> {
        val type = object : TypeToken<Map<String, CalibrationEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    private fun importCsv(csv: String): Map<String, CalibrationEntry> {
        val rows = parseCsvRows(csv)
        if (rows.isEmpty()) return emptyMap()

        val schema = detectHeader(rows.first())
        val dataRows = if (schema != null) rows.drop(1) else rows
        return buildMap {
            for (row in dataRows) {
                if (row.isEmpty() || row.all { it.isBlank() }) continue
                val parsed = if (schema != null) parseHeaderRow(row, schema) else parsePositionalRow(row)
                put(parsed.first, parsed.second)
            }
        }
    }

    private fun detectHeader(row: List<String>): CsvSchema? {
        val normalized = row.map { it.trim().lowercase() }
        val propKeyIndex = normalized.indexOfFirst { it in PROP_KEY_HEADERS }
        val calibratedTextIndex = normalized.indexOfFirst { it in CALIBRATED_TEXT_HEADERS }
        if (propKeyIndex < 0 || calibratedTextIndex < 0) return null
        return CsvSchema(
            propKeyIndex = propKeyIndex,
            originalTextIndex = normalized.indexOfFirst { it in ORIGINAL_TEXT_HEADERS }.takeIf { it >= 0 },
            calibratedTextIndex = calibratedTextIndex,
            timestampIndex = normalized.indexOfFirst { it in TIMESTAMP_HEADERS }.takeIf { it >= 0 }
        )
    }

    private fun parseHeaderRow(row: List<String>, schema: CsvSchema): Pair<String, CalibrationEntry> {
        val propKey = row.valueAt(schema.propKeyIndex).trim()
        val calibratedText = row.valueAt(schema.calibratedTextIndex).trim()
        require(propKey.isNotBlank()) { "Calibration import row missing propKey" }
        require(calibratedText.isNotBlank()) { "Calibration import row missing calibrated text" }
        val originalText = schema.originalTextIndex?.let { index -> row.valueAt(index) }.orEmpty()
        val timestamp = schema.timestampIndex?.let { index -> row.valueAt(index) }?.trim()?.toLongOrNull()
            ?: System.currentTimeMillis()
        return propKey to CalibrationEntry(
            originalText = originalText,
            calibratedText = calibratedText,
            timestamp = timestamp
        )
    }

    private fun parsePositionalRow(row: List<String>): Pair<String, CalibrationEntry> {
        require(row.size in 2..4) { "Calibration CSV rows must have 2 to 4 columns" }
        val propKey = row[0].trim()
        val calibratedText = row[if (row.size == 2) 1 else 2].trim()
        require(propKey.isNotBlank()) { "Calibration import row missing propKey" }
        require(calibratedText.isNotBlank()) { "Calibration import row missing calibrated text" }
        val originalText = if (row.size == 2) "" else row[1]
        val timestamp = if (row.size >= 4) row[3].trim().toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
        return propKey to CalibrationEntry(
            originalText = originalText,
            calibratedText = calibratedText,
            timestamp = timestamp
        )
    }

    private fun parseCsvRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < content.length) {
            when (val ch = content[index]) {
                '"' -> {
                    if (inQuotes && index + 1 < content.length && content[index + 1] == '"') {
                        currentCell.append('"')
                        index += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ',' -> {
                    if (inQuotes) {
                        currentCell.append(ch)
                    } else {
                        currentRow += currentCell.toString()
                        currentCell.clear()
                    }
                }

                '\n', '\r' -> {
                    if (inQuotes) {
                        currentCell.append(ch)
                    } else {
                        if (ch == '\r' && index + 1 < content.length && content[index + 1] == '\n') {
                            index += 1
                        }
                        currentRow += currentCell.toString()
                        currentCell.clear()
                        rows += currentRow.toList()
                        currentRow.clear()
                    }
                }

                else -> currentCell.append(ch)
            }
            index += 1
        }

        if (inQuotes) {
            throw IllegalArgumentException("Unterminated quoted CSV field")
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += currentCell.toString()
            rows += currentRow.toList()
        }

        return rows.filterNot { row -> row.all { it.isBlank() } }
    }

    private fun List<String>.valueAt(index: Int): String {
        return getOrElse(index) { "" }
    }

    private data class CsvSchema(
        val propKeyIndex: Int,
        val originalTextIndex: Int?,
        val calibratedTextIndex: Int,
        val timestampIndex: Int?
    )

    private companion object {
        val PROP_KEY_HEADERS = setOf("propkey", "prop_key", "key")
        val ORIGINAL_TEXT_HEADERS = setOf("originaltext", "original_text", "original", "sourcetext", "source_text", "source")
        val CALIBRATED_TEXT_HEADERS = setOf("calibratedtext", "calibrated_text", "calibrated", "correctedtext", "corrected_text", "corrected")
        val TIMESTAMP_HEADERS = setOf("timestamp", "time")
    }
}
