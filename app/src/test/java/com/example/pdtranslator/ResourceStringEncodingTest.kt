package com.example.pdtranslator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResourceStringEncodingTest {

  private data class ResourceExpectation(
    val path: String,
    val mustContain: List<String>,
    val mustNotContain: List<String>
  )

  @Test
  fun `string resource files keep expected unicode text without mojibake`() {
    val expectations = listOf(
      ResourceExpectation(
        path = "app/src/main/res/values/strings.xml",
        mustContain = listOf(
          "<string name=\"language_chinese\">简体中文</string>",
          "Android’s modern toolkit for building native UI.",
          "<string name=\"loading_importing\">Importing…</string>",
          "16×16 viewport"
        ),
        mustNotContain = listOf(
          "Android鈥檚",
          "Importing鈥?",
          "16脳16",
          "绠€浣撲腑鏂?"
        )
      ),
      ResourceExpectation(
        path = "app/src/main/res/values-en/strings.xml",
        mustContain = listOf(
          "<string name=\"language_chinese\">简体中文</string>",
          "<string name=\"loading_importing\">Importing…</string>",
          "16×16 viewport"
        ),
        mustNotContain = listOf(
          "Importing鈥?",
          "16脳16",
          "绠€浣撲腑鏂?"
        )
      ),
      ResourceExpectation(
        path = "app/src/main/res/values-zh/strings.xml",
        mustContain = listOf(
          "<string name=\"screen_title_config\">基础配置</string>",
          "<string name=\"dict_import\">导入字典</string>",
          "<string name=\"loading_importing\">正在导入…</string>",
          "有道 Web 当前不支持这个语言对。请尝试将源语言改为自动识别，或切换其他引擎。"
        ),
        mustNotContain = listOf(
          "鍩虹閰嶇疆",
          "瀵煎叆瀛楀吀",
          "姝ｅ湪瀵煎叆鈥?",
          "鏈夐亾 Web"
        )
      ),
      ResourceExpectation(
        path = "app/src/main/res/values-zh-rCN/strings.xml",
        mustContain = listOf(
          "<string name=\"screen_title_config\">基础配置</string>",
          "<string name=\"dict_import\">导入字典</string>",
          "<string name=\"loading_importing\">正在导入…</string>",
          "有道 Web 当前不支持这个语言对。请尝试将源语言改为自动识别，或切换其他引擎。"
        ),
        mustNotContain = listOf(
          "鍩虹閰嶇疆",
          "瀵煎叆瀛楀吀",
          "姝ｅ湪瀵煎叆鈥?",
          "鏈夐亾 Web"
        )
      )
    )

    expectations.forEach { expectation ->
      val text = projectFile(expectation.path).readText(Charsets.UTF_8)

      assertFalse("${expectation.path} should not start with a UTF-8 BOM", text.startsWith("\uFEFF"))

      expectation.mustContain.forEach { needle ->
        assertTrue("${expectation.path} should contain $needle", text.contains(needle))
      }

      expectation.mustNotContain.forEach { needle ->
        assertFalse("${expectation.path} should not contain $needle", text.contains(needle))
      }
    }
  }

  private fun projectFile(relativePath: String): File {
    val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
      .firstOrNull { File(it, "app/src/main/res/values/strings.xml").exists() }
      ?: error("Unable to find project root from ${System.getProperty("user.dir")}")

    return File(root, relativePath)
  }
}
