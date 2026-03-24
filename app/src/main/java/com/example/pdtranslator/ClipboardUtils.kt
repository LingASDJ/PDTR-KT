package com.example.pdtranslator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyTextToClipboard(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
  clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
  Toast.makeText(context, context.getString(R.string.common_copied), Toast.LENGTH_SHORT).show()
}
