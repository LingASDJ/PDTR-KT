package com.example.pdtranslator

object AggregateLanguageGroup {
  const val ALL_GROUP_NAME = "all"

  fun isAllGroup(groupName: String?): Boolean = groupName == ALL_GROUP_NAME

  fun groupOptions(groupNames: List<String>): List<String> {
    return if (groupNames.isEmpty()) {
      emptyList()
    } else {
      listOf(ALL_GROUP_NAME) + groupNames
    }
  }

  fun availableLanguages(groups: List<LanguageGroup>, selectedGroupName: String?): List<String> {
    return when {
      selectedGroupName == null -> emptyList()
      isAllGroup(selectedGroupName) -> {
        if (groups.isEmpty()) return emptyList()
        groups
          .map { it.languages.keys.toSet() }
          .reduce(Set<String>::intersect)
          .toList()
          .sorted()
      }

      else -> groups.firstOrNull { it.name == selectedGroupName }?.languages?.keys?.sorted().orEmpty()
    }
  }
}
