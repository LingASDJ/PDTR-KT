package com.example.pdtranslator

import java.io.Writer
import java.util.Properties

/**
 * Writes Properties to a Writer in UTF-8 without escaping non-ASCII characters
 * to \uXXXX sequences (unlike the default Properties.store()).
 */
object PropertiesWriter {

  fun write(props: Properties, writer: Writer) {
    val sortedKeys = props.keys.mapNotNull { it as? String }.sorted()
    val writtenKeys = mutableSetOf<String>()
    for (key in sortedKeys) {
      val cleanKey = key.removePrefix("\uFEFF")
      if (!writtenKeys.add(cleanKey)) continue
      val value = props.getProperty(cleanKey) ?: props.getProperty(key, "")
      writer.write(escapeKey(cleanKey))
      writer.write("=")
      writer.write(escapeValue(value))
      writer.write("\n")
    }
  }

  internal fun escapeKey(key: String): String = buildString {
    for (ch in key) {
      when (ch) {
        ' ', '=', ':', '#', '!' -> { append('\\'); append(ch) }
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> append(ch)
      }
    }
  }

  internal fun escapeValue(value: String): String = buildString {
    var leadingSpace = true
    for (ch in value) {
      when {
        ch == ' ' && leadingSpace -> append("\\ ")
        ch == '\\' -> { append("\\\\"); leadingSpace = false }
        ch == '\n' -> { append("\\n"); leadingSpace = false }
        ch == '\r' -> { append("\\r"); leadingSpace = false }
        ch == '\t' -> { append("\\t"); leadingSpace = false }
        else -> { append(ch); leadingSpace = false }
      }
    }
  }
}
