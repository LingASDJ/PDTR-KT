package com.example.pdtranslator

data class DraftScopeSnapshot(
  val groupName: String,
  val sourceLangCode: String,
  val targetLangCode: String,
  val stagedChanges: Map<String, String>,
  val stagedDeletions: Set<String> = emptySet()
) {
  fun toEditScope(): EditScope {
    return EditScope(
      groupName = groupName,
      sourceLangCode = sourceLangCode,
      targetLangCode = targetLangCode
    )
  }
}

data class DraftCreatedLanguageSnapshot(
  val groupName: String,
  val languages: Map<String, Map<String, String>>,
  val originalContents: Map<String, String>? = null
)

data class DraftWorkspaceSnapshot(
  val scopes: List<DraftScopeSnapshot> = emptyList(),
  val createdLanguagesByGroup: List<DraftCreatedLanguageSnapshot> = emptyList()
) {
  fun scope(scope: EditScope): DraftScopeSnapshot? {
    return scopes.firstOrNull {
      it.groupName == scope.groupName &&
        it.sourceLangCode == scope.sourceLangCode &&
        it.targetLangCode == scope.targetLangCode
    }
  }

  fun createdLanguages(groupName: String): Map<String, Map<String, String>> {
    return createdLanguagesByGroup.firstOrNull { it.groupName == groupName }?.languages.orEmpty()
  }

  fun createdLanguageOriginalContent(groupName: String, langCode: String): String? {
    return createdLanguagesByGroup
      .firstOrNull { it.groupName == groupName }
      ?.originalContents
      ?.get(langCode)
  }

  fun toScopedWorkspaceState(): ScopedWorkspaceState {
    var workspace = ScopedWorkspaceState()
    scopes.forEach { snapshot ->
      val scope = snapshot.toEditScope()
      workspace = workspace
        .withStagedChanges(scope, snapshot.stagedChanges)
        .withStagedDeletions(scope, snapshot.stagedDeletions)
    }
    createdLanguagesByGroup.forEach { snapshot ->
      workspace = workspace.withCreatedLanguages(snapshot.groupName, snapshot.languages.keys)
    }
    return workspace
  }

  companion object {
    fun capture(workspace: ScopedWorkspaceState, groups: List<LanguageGroup>): DraftWorkspaceSnapshot {
      val groupsByName = groups.associateBy { it.name }
      val scopes = linkedSetOf<EditScope>().apply {
        addAll(workspace.stagedChangesByScope.keys)
        addAll(workspace.stagedDeletionsByScope.keys)
      }
        .toList()
        .sortedWith(compareBy(EditScope::groupName, EditScope::sourceLangCode, EditScope::targetLangCode))
        .map { scope ->
          DraftScopeSnapshot(
            groupName = scope.groupName,
            sourceLangCode = scope.sourceLangCode,
            targetLangCode = scope.targetLangCode,
            stagedChanges = LinkedHashMap(workspace.stagedChanges(scope)),
            stagedDeletions = LinkedHashSet(workspace.stagedDeletions(scope))
          )
        }

      val createdLanguagesByGroup = workspace.createdLanguagesByGroup.entries
        .filter { it.value.isNotEmpty() }
        .sortedBy { it.key }
        .map { (groupName, codes) ->
          val group = groupsByName[groupName]
          val languages = linkedMapOf<String, Map<String, String>>()
          val originalContents = linkedMapOf<String, String>()
          codes.toList().sorted().forEach { langCode ->
            val languageData = group?.languages?.get(langCode)
            val props = languageData?.properties
            val values = linkedMapOf<String, String>()
            props?.stringPropertyNames()?.sorted()?.forEach { key ->
              values[key] = props.getProperty(key, "")
            }
            languages[langCode] = values
            languageData?.originalContent?.let { originalContents[langCode] = it }
          }
          DraftCreatedLanguageSnapshot(
            groupName = groupName,
            languages = languages,
            originalContents = originalContents.takeIf { it.isNotEmpty() }
          )
        }

      return DraftWorkspaceSnapshot(scopes = scopes, createdLanguagesByGroup = createdLanguagesByGroup)
    }

    fun fromDraft(draft: DraftData): DraftWorkspaceSnapshot {
      draft.workspaceSnapshot?.let { return it }

      val legacyScope = DraftScopeSnapshot(
        groupName = draft.groupName,
        sourceLangCode = draft.sourceLangCode,
        targetLangCode = draft.targetLangCode,
        stagedChanges = draft.stagedChanges,
        stagedDeletions = draft.stagedDeletions.orEmpty()
      )
      val scopes = if (legacyScope.stagedChanges.isEmpty() && legacyScope.stagedDeletions.isEmpty()) {
        emptyList()
      } else {
        listOf(legacyScope)
      }
      val createdLanguagesByGroup = draft.createdLanguages
        ?.takeIf { it.isNotEmpty() }
        ?.let { listOf(DraftCreatedLanguageSnapshot(groupName = draft.groupName, languages = LinkedHashMap(it))) }
        .orEmpty()

      return DraftWorkspaceSnapshot(scopes = scopes, createdLanguagesByGroup = createdLanguagesByGroup)
    }
  }
}
