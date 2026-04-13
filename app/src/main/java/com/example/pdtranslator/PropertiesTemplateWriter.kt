package com.example.pdtranslator

import java.io.StringReader
import java.util.Properties

object PropertiesTemplateWriter {

  private val invalidUnicodeRegex = Regex("""\x5Cu(?![0-9a-fA-F]{4})""")

  fun render(template: String, props: Properties): String {
    if (template.isEmpty()) {
      return buildString {
        val normalizedEntries = normalizedEntries(props)
        normalizedEntries.entries.forEachIndexed { index, entry ->
          if (index > 0) append('\n')
          append(PropertiesWriter.escapeKey(entry.key))
          append('=')
          append(PropertiesWriter.escapeValue(entry.value))
        }
        if (normalizedEntries.isNotEmpty()) append('\n')
      }
    }

    val segments = parseTemplate(template)
    val normalizedEntries = normalizedEntries(props)
    val preferredLineEnding = detectPreferredLineEnding(template)
    val occurrenceIndexesByKey = buildOccurrenceIndexesByKey(segments)
    val output = StringBuilder()

    segments.forEachIndexed { index, segment ->
      when (segment) {
        is Segment.Literal -> {
          if (shouldKeepLiteral(segment, segments, index, normalizedEntries)) {
            output.append(segment.raw)
          }
        }
        is Segment.Property -> {
          val finalValue = normalizedEntries[segment.key] ?: return@forEachIndexed
          val isEffectiveOccurrence = occurrenceIndexesByKey[segment.key]?.lastOrNull() == index
          if (!isEffectiveOccurrence) {
            output.append(segment.raw)
          } else {
            if (finalValue == segment.originalValue) {
              output.append(segment.raw)
            } else {
              output.append(segment.leadingWhitespace)
              output.append(PropertiesWriter.escapeKey(segment.key))
              output.append(segment.separator)
              output.append(PropertiesWriter.escapeValue(finalValue))
              output.append(segment.lineEnding)
            }
          }
        }
      }
    }

    normalizedEntries
      .filterKeys { it !in occurrenceIndexesByKey }
      .forEach { (key, value) ->
        ensureLineBreakBeforeAppend(output, preferredLineEnding)
        output.append(PropertiesWriter.escapeKey(key))
        output.append('=')
        output.append(PropertiesWriter.escapeValue(value))
        output.append(preferredLineEnding)
      }

    return output.toString()
  }

  private fun ensureLineBreakBeforeAppend(output: StringBuilder, lineEnding: String) {
    if (output.isEmpty()) return
    val lastChar = output.last()
    if (lastChar != '\n' && lastChar != '\r') {
      output.append(lineEnding)
    }
  }

  private fun buildOccurrenceIndexesByKey(segments: List<Segment>): Map<String, List<Int>> {
    val indexes = linkedMapOf<String, MutableList<Int>>()
    segments.forEachIndexed { index, segment ->
      if (segment is Segment.Property) {
        indexes.getOrPut(segment.key) { mutableListOf() }.add(index)
      }
    }
    return indexes
  }

  private fun shouldKeepLiteral(
    literal: Segment.Literal,
    segments: List<Segment>,
    index: Int,
    normalizedEntries: Map<String, String>
  ): Boolean {
    if (!literal.isCommentOrWhitespaceOnly()) return true
    val followingProperties = segments.subList(index + 1, segments.size).filterIsInstance<Segment.Property>()
    if (followingProperties.isEmpty()) return true

    // Keep shared section comments/spacing if any later property in that block still survives export.
    return followingProperties.any { it.key in normalizedEntries }
  }

  private fun normalizedEntries(props: Properties): LinkedHashMap<String, String> {
    val normalized = linkedMapOf<String, String>()
    props.keys
      .mapNotNull { it as? String }
      .sorted()
      .forEach { key ->
        val cleanKey = key.removePrefix("\uFEFF")
        if (normalized.containsKey(cleanKey)) return@forEach
        normalized[cleanKey] = props.getProperty(cleanKey) ?: props.getProperty(key, "")
      }
    return normalized
  }

  private fun parseTemplate(template: String): List<Segment> {
    val lines = splitPhysicalLines(template)
    if (lines.isEmpty()) return emptyList()

    val segments = mutableListOf<Segment>()
    var index = 0
    while (index < lines.size) {
      val raw = StringBuilder(lines[index])
      while (hasContinuation(lines[index]) && index + 1 < lines.size) {
        index++
        raw.append(lines[index])
      }
      segments += parseSegment(raw.toString())
      index++
    }
    return segments
  }

  private fun parseSegment(raw: String): Segment {
    val firstLine = firstPhysicalLine(raw)
    val firstLineBody = trimLineEnding(firstLine)
    val firstNonWhitespace = firstLineBody.indexOfFirst { !it.isPropertiesWhitespace() }
    if (firstNonWhitespace == -1) return Segment.Literal(raw)

    val firstChar = firstLineBody[firstNonWhitespace]
    if (firstChar == '#' || firstChar == '!') return Segment.Literal(raw)

    val parsed = loadSingleEntry(raw) ?: return Segment.Literal(raw)
    val prefix = parsePrefix(firstLineBody)

    return Segment.Property(
      key = parsed.first.removePrefix("\uFEFF"),
      originalValue = parsed.second,
      raw = raw,
      leadingWhitespace = firstLineBody.substring(0, prefix.keyStart),
      separator = firstLineBody.substring(prefix.keyEnd, prefix.valueStart),
      lineEnding = firstLine.removePrefix(firstLineBody)
    )
  }

  private fun loadSingleEntry(raw: String): Pair<String, String>? {
    val cleaned = raw.removePrefix("\uFEFF")
    val props = Properties()
    return try {
      props.load(StringReader(cleaned))
      firstEntry(props)
    } catch (_: IllegalArgumentException) {
      props.clear()
      props.load(StringReader(cleaned.replace(invalidUnicodeRegex, "\\\\u")))
      firstEntry(props)
    }
  }

  private fun firstEntry(props: Properties): Pair<String, String>? {
    val key = props.keys.mapNotNull { it as? String }.firstOrNull() ?: return null
    val cleanKey = key.removePrefix("\uFEFF")
    val value = props.getProperty(cleanKey) ?: props.getProperty(key, "")
    return cleanKey to value
  }

  private fun parsePrefix(firstLineBody: String): PrefixBounds {
    var keyStart = 0
    while (keyStart < firstLineBody.length && firstLineBody[keyStart].isPropertiesWhitespace()) {
      keyStart++
    }

    var index = keyStart
    var escaped = false
    while (index < firstLineBody.length) {
      val ch = firstLineBody[index]
      when {
        escaped -> escaped = false
        ch == '\\' -> escaped = true
        ch == '=' || ch == ':' -> {
          val valueStart = skipValuePrefix(firstLineBody, index + 1)
          return PrefixBounds(keyStart = keyStart, keyEnd = index, valueStart = valueStart)
        }
        ch.isPropertiesWhitespace() -> {
          var separatorEnd = index
          while (separatorEnd < firstLineBody.length && firstLineBody[separatorEnd].isPropertiesWhitespace()) {
            separatorEnd++
          }
          if (separatorEnd < firstLineBody.length && (firstLineBody[separatorEnd] == '=' || firstLineBody[separatorEnd] == ':')) {
            separatorEnd++
          }
          val valueStart = skipValuePrefix(firstLineBody, separatorEnd)
          return PrefixBounds(keyStart = keyStart, keyEnd = index, valueStart = valueStart)
        }
      }
      index++
    }

    return PrefixBounds(keyStart = keyStart, keyEnd = firstLineBody.length, valueStart = firstLineBody.length)
  }

  private fun skipValuePrefix(line: String, start: Int): Int {
    var index = start
    while (index < line.length && line[index].isPropertiesWhitespace()) {
      index++
    }
    return index
  }

  private fun detectPreferredLineEnding(template: String): String {
    val crlfIndex = template.indexOf("\r\n")
    if (crlfIndex >= 0) return "\r\n"
    if (template.contains('\n')) return "\n"
    if (template.contains('\r')) return "\r"
    return "\n"
  }

  private fun firstPhysicalLine(raw: String): String {
    val newlineIndex = raw.indexOfAny(charArrayOf('\r', '\n'))
    if (newlineIndex == -1) return raw
    if (raw[newlineIndex] == '\r' && newlineIndex + 1 < raw.length && raw[newlineIndex + 1] == '\n') {
      return raw.substring(0, newlineIndex + 2)
    }
    return raw.substring(0, newlineIndex + 1)
  }

  private fun trimLineEnding(line: String): String {
    var end = line.length
    while (end > 0 && (line[end - 1] == '\r' || line[end - 1] == '\n')) {
      end--
    }
    return line.substring(0, end)
  }

  private fun splitPhysicalLines(template: String): List<String> {
    if (template.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var start = 0
    var index = 0
    while (index < template.length) {
      when (template[index]) {
        '\r' -> {
          val end = if (index + 1 < template.length && template[index + 1] == '\n') index + 2 else index + 1
          lines += template.substring(start, end)
          start = end
          index = end
          continue
        }
        '\n' -> {
          val end = index + 1
          lines += template.substring(start, end)
          start = end
        }
      }
      index++
    }
    if (start < template.length) {
      lines += template.substring(start)
    }
    return lines
  }

  private fun hasContinuation(line: String): Boolean {
    val body = trimLineEnding(line)
    var slashCount = 0
    var index = body.length - 1
    while (index >= 0 && body[index] == '\\') {
      slashCount++
      index--
    }
    return slashCount % 2 == 1
  }

  private fun Char.isPropertiesWhitespace(): Boolean {
    return this == ' ' || this == '\t' || this == '\u000C'
  }

  private sealed interface Segment {
    data class Literal(val raw: String) : Segment {
      fun isCommentOrWhitespaceOnly(): Boolean {
        return splitLiteralLines(raw).all { line ->
          val trimmed = line.trim()
          trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")
        }
      }
    }

    data class Property(
      val key: String,
      val originalValue: String,
      val raw: String,
      val leadingWhitespace: String,
      val separator: String,
      val lineEnding: String
    ) : Segment
  }

  private data class PrefixBounds(
    val keyStart: Int,
    val keyEnd: Int,
    val valueStart: Int
  )

  private fun splitLiteralLines(raw: String): List<String> {
    val lines = splitPhysicalLines(raw)
    return if (lines.isEmpty()) listOf(raw) else lines.map(::trimLineEnding)
  }
}
