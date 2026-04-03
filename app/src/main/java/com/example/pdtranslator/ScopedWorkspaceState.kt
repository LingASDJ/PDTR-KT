package com.example.pdtranslator

data class EditScope(
  val groupName: String,
  val sourceLangCode: String,
  val targetLangCode: String
)

data class ScopedWorkspaceState(
  val stagedChangesByScope: Map<EditScope, Map<String, String>> = emptyMap(),
  val stagedDeletionsByScope: Map<EditScope, Set<String>> = emptyMap(),
  val createdLanguagesByGroup: Map<String, Set<String>> = emptyMap(),
  val noTranslationNeededByScope: Map<EditScope, Set<String>> = emptyMap()
) {

  fun stagedChanges(scope: EditScope?): Map<String, String> {
    return scope?.let { stagedChangesByScope[it] }.orEmpty()
  }

  fun stagedDeletions(scope: EditScope?): Set<String> {
    return scope?.let { stagedDeletionsByScope[it] }.orEmpty()
  }

  fun createdLanguages(groupName: String?): Set<String> {
    return groupName?.let { createdLanguagesByGroup[it] }.orEmpty()
  }

  fun withStagedChanges(scope: EditScope?, changes: Map<String, String>): ScopedWorkspaceState {
    if (scope == null) return this
    val updated = stagedChangesByScope.toMutableMap()
    if (changes.isEmpty()) {
      updated.remove(scope)
    } else {
      updated[scope] = LinkedHashMap(changes)
    }
    return copy(stagedChangesByScope = updated)
  }

  fun withStagedDeletions(scope: EditScope?, deletions: Set<String>): ScopedWorkspaceState {
    if (scope == null) return this
    val updated = stagedDeletionsByScope.toMutableMap()
    if (deletions.isEmpty()) {
      updated.remove(scope)
    } else {
      updated[scope] = LinkedHashSet(deletions)
    }
    return copy(stagedDeletionsByScope = updated)
  }

  fun withCreatedLanguages(groupName: String?, languages: Set<String>): ScopedWorkspaceState {
    if (groupName == null) return this
    val updated = createdLanguagesByGroup.toMutableMap()
    if (languages.isEmpty()) {
      updated.remove(groupName)
    } else {
      updated[groupName] = LinkedHashSet(languages)
    }
    return copy(createdLanguagesByGroup = updated)
  }

  fun noTranslationNeeded(scope: EditScope?): Set<String> {
    return scope?.let { noTranslationNeededByScope[it] }.orEmpty()
  }

  fun withNoTranslationNeeded(scope: EditScope?, keys: Set<String>): ScopedWorkspaceState {
    if (scope == null) return this
    val updated = noTranslationNeededByScope.toMutableMap()
    if (keys.isEmpty()) {
      updated.remove(scope)
    } else {
      updated[scope] = LinkedHashSet(keys)
    }
    return copy(noTranslationNeededByScope = updated)
  }
}
