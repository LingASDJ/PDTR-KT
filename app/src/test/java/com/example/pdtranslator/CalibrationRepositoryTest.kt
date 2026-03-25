package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalibrationRepositoryTest {

  @Test
  fun `imports calibration csv with header and quoted fields`() {
    val dir = createTempDir(prefix = "calibration-repo-csv")
    try {
      val repository = CalibrationRepository(dir)
      val imported = repository.importContent(
        """
        propKey,originalText,calibratedText,timestamp
        item.magic,Magic Wand,Magic Staff,123
        boss.king,"Old, King",King,456
        """.trimIndent()
      )

      assertEquals(2, imported.size)
      assertEquals("Magic Wand", imported["item.magic"]?.originalText)
      assertEquals("Magic Staff", imported["item.magic"]?.calibratedText)
      assertEquals(123L, imported["item.magic"]?.timestamp)
      assertEquals("Old, King", imported["boss.king"]?.originalText)
      assertEquals("King", imported["boss.king"]?.calibratedText)
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun `rejects calibration csv rows without prop key or calibrated text`() {
    val dir = createTempDir(prefix = "calibration-repo-invalid")
    try {
      val repository = CalibrationRepository(dir)

      assertThrows(IllegalArgumentException::class.java) {
        repository.importContent(
          """
          propKey,originalText,calibratedText
          item.magic,Magic,
          """.trimIndent()
        )
      }
    } finally {
      dir.deleteRecursively()
    }
  }
}
