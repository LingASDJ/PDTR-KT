package com.example.pdtranslator

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun writeTextAtomically(targetFile: File, text: String) {
  targetFile.parentFile?.mkdirs()
  val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
  tempFile.writeText(text, Charsets.UTF_8)
  replaceFileFromTemp(tempFile, targetFile)
}

fun replaceFileFromTemp(tempFile: File, targetFile: File) {
  require(tempFile.exists()) { "Temporary file does not exist: ${tempFile.absolutePath}" }
  targetFile.parentFile?.mkdirs()
  try {
    Files.move(
      tempFile.toPath(),
      targetFile.toPath(),
      StandardCopyOption.REPLACE_EXISTING,
      StandardCopyOption.ATOMIC_MOVE
    )
  } catch (_: AtomicMoveNotSupportedException) {
    try {
      Files.move(
        tempFile.toPath(),
        targetFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING
      )
    } catch (e: Exception) {
      tempFile.delete()
      throw e
    }
  }
}

object AtomicFilePersistenceOps {
  fun replaceFromTemp(tempFile: File, targetFile: File) {
    replaceFileFromTemp(tempFile, targetFile)
  }
}
