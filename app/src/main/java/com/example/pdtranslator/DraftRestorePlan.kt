package com.example.pdtranslator

data class DraftRestorePlan(
  val groupName: String,
  val preselectedSourceLangCode: String?,
  val preselectedTargetLangCode: String?,
  val finalSourceLangCode: String,
  val finalTargetLangCode: String
) {
  companion object {
    fun fromDraft(draft: DraftData): DraftRestorePlan {
      return DraftRestorePlan(
        groupName = draft.groupName,
        preselectedSourceLangCode = null,
        preselectedTargetLangCode = null,
        finalSourceLangCode = draft.sourceLangCode,
        finalTargetLangCode = draft.targetLangCode
      )
    }
  }
}
