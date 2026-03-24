package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class AtomicFilePersistenceTest {

  @Test
  fun `replaces target contents from temp file`() {
    val dir = createTempDir(prefix = "atomic-file-test")
    try {
      val target = File(dir, "dictionary.json").apply { writeText("old", Charsets.UTF_8) }
      val temp = File(dir, "dictionary.json.tmp").apply { writeText("new", Charsets.UTF_8) }

      replaceFileFromTemp(temp, target)

      assertEquals("new", target.readText(Charsets.UTF_8))
      assertFalse(temp.exists())
    } finally {
      dir.deleteRecursively()
    }
  }
}
