package com.example.pdtranslator

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.util.Locale

data class NamedDictionary(
  val id: String,
  val name: String,
  val entries: Map<String, DictEntry> = emptyMap()
) {
  val entryCount: Int get() = entries.size
}

data class DictionaryStore(
  val selectedDictionaryId: String,
  val dictionaries: Map<String, NamedDictionary>
) {
  val selectedDictionary: NamedDictionary
    get() = dictionaries[selectedDictionaryId] ?: dictionaries.values.first()

  fun normalized(defaultName: String = DEFAULT_NAME): DictionaryStore {
    val normalizedDictionaries = if (dictionaries.isEmpty()) {
      linkedMapOf(DEFAULT_ID to NamedDictionary(DEFAULT_ID, defaultName))
    } else {
      LinkedHashMap(dictionaries)
    }
    val resolvedSelection = if (normalizedDictionaries.containsKey(selectedDictionaryId)) {
      selectedDictionaryId
    } else {
      normalizedDictionaries.keys.first()
    }
    return copy(selectedDictionaryId = resolvedSelection, dictionaries = normalizedDictionaries)
  }

  fun selectDictionary(id: String): DictionaryStore {
    return if (dictionaries.containsKey(id)) copy(selectedDictionaryId = id) else this
  }

  fun createDictionary(name: String): DictionaryStore {
    val trimmed = name.trim().ifBlank { DEFAULT_NAME }
    var suffix = 1
    var candidateId = slugify(trimmed)
    while (candidateId in dictionaries) {
      suffix += 1
      candidateId = "${slugify(trimmed)}-$suffix"
    }
    val updated = LinkedHashMap(dictionaries)
    updated[candidateId] = NamedDictionary(candidateId, trimmed)
    return copy(selectedDictionaryId = candidateId, dictionaries = updated).normalized()
  }

  fun renameDictionary(
    selectedOnly: Boolean = false,
    newName: String,
    dictionaryId: String? = null
  ): DictionaryStore {
    val targetId = when {
      selectedOnly -> selectedDictionaryId
      dictionaryId != null -> dictionaryId
      else -> selectedDictionaryId
    }
    val dictionary = dictionaries[targetId] ?: return this
    val trimmed = newName.trim()
    if (trimmed.isBlank()) return this
    val updated = LinkedHashMap(dictionaries)
    updated[targetId] = dictionary.copy(name = trimmed)
    return copy(dictionaries = updated)
  }

  fun deleteDictionary(id: String): DictionaryStore {
    if (dictionaries.size <= 1 || !dictionaries.containsKey(id)) return this
    val updated = LinkedHashMap(dictionaries)
    updated.remove(id)
    val nextSelected = if (selectedDictionaryId == id) updated.keys.first() else selectedDictionaryId
    return copy(selectedDictionaryId = nextSelected, dictionaries = updated).normalized()
  }

  fun clearDictionary(id: String = selectedDictionaryId): DictionaryStore {
    val dictionary = dictionaries[id] ?: return this
    val updated = LinkedHashMap(dictionaries)
    updated[id] = dictionary.copy(entries = emptyMap())
    return copy(dictionaries = updated)
  }

  fun putEntry(entryKey: String, value: DictEntry, dictionaryId: String = selectedDictionaryId): DictionaryStore {
    val dictionary = dictionaries[dictionaryId] ?: return this
    val updatedEntries = LinkedHashMap(dictionary.entries)
    updatedEntries[entryKey] = value
    val updatedDictionaries = LinkedHashMap(dictionaries)
    updatedDictionaries[dictionaryId] = dictionary.copy(entries = updatedEntries)
    return copy(dictionaries = updatedDictionaries)
  }

  companion object {
    const val DEFAULT_ID = "default"
    const val DEFAULT_NAME = "Default Dictionary"

    fun empty(defaultName: String = DEFAULT_NAME): DictionaryStore {
      return DictionaryStore(
        selectedDictionaryId = DEFAULT_ID,
        dictionaries = linkedMapOf(DEFAULT_ID to NamedDictionary(DEFAULT_ID, defaultName))
      )
    }

    private fun slugify(name: String): String {
      val base = name
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
      return if (base.isBlank()) "dictionary" else base
    }
  }
}

object DictionaryStoreSerializer {
  private val gson = Gson()

  fun fromJson(json: String, defaultName: String = DictionaryStore.DEFAULT_NAME): DictionaryStore {
    if (json.isBlank()) return DictionaryStore.empty(defaultName)
    val root = gson.fromJson(json, JsonObject::class.java) ?: return DictionaryStore.empty(defaultName)
    return if (root.has("dictionaries")) {
      val type = object : TypeToken<DictionaryStore>() {}.type
      (gson.fromJson<DictionaryStore>(json, type) ?: DictionaryStore.empty(defaultName)).normalized(defaultName)
    } else {
      migrateLegacy(json, defaultName)
    }
  }

  fun toJson(store: DictionaryStore): String {
    return gson.toJson(store.normalized())
  }

  private fun migrateLegacy(json: String, defaultName: String): DictionaryStore {
    val type = object : TypeToken<Map<String, DictEntry>>() {}.type
    val legacyEntries = gson.fromJson<Map<String, DictEntry>>(json, type).orEmpty()
    val base = DictionaryStore.empty(defaultName)
    val updatedDictionary = base.selectedDictionary.copy(entries = LinkedHashMap(legacyEntries))
    return base.copy(dictionaries = linkedMapOf(updatedDictionary.id to updatedDictionary)).normalized(defaultName)
  }
}
