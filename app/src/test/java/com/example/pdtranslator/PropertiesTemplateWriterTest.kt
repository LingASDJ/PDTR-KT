package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Properties

class PropertiesTemplateWriterTest {

  @Test
  fun `render preserves comments blank lines and untouched raw entry text`() {
    val template = """
      # keep this comment
      actors.buffs.status.foundchest.name = 宝藏搜寻
      actors.buffs.status.foundchest.desc=你来到了拟态王的宝藏区域，看起来你还能搜查_%s/5个_宝箱怪，然后这里就会变得不稳定，从而导致其他宝箱怪魂飞魄散离开这里！\n\n偶尔还能看见拟态王的幼年形态，但由于这里时空错乱，你无法将它杀死……
      
      actors.buffs.herodisguise.name=伪装
      actors.buffs.herodisguise.desc=幻术魔法改变了你的外貌！虽然此效果是完全装饰性的，但无论如何感觉起来还是很奇怪。\n\n伪装效果剩余时长：%s回合
    """.trimIndent().replace("\n", "\r\n")

    val props = Properties().apply {
      setProperty("actors.buffs.status.foundchest.name", "宝藏搜寻")
      setProperty(
        "actors.buffs.status.foundchest.desc",
        "你来到了拟态王的宝藏区域，看起来你还能搜查_%s/5个_宝箱怪，然后这里就会变得不稳定，从而导致其他宝箱怪魂飞魄散离开这里！\n\n偶尔还能看见拟态王的幼年形态，但由于这里时空错乱，你无法将它杀死……"
      )
      setProperty("actors.buffs.herodisguise.name", "伪装术")
      setProperty(
        "actors.buffs.herodisguise.desc",
        "幻术魔法改变了你的外貌！虽然此效果是完全装饰性的，但无论如何感觉起来还是很奇怪。\n\n伪装效果剩余时长：%s回合"
      )
    }

    val rendered = PropertiesTemplateWriter.render(template, props)

    assertEquals(
      """
        # keep this comment
        actors.buffs.status.foundchest.name = 宝藏搜寻
        actors.buffs.status.foundchest.desc=你来到了拟态王的宝藏区域，看起来你还能搜查_%s/5个_宝箱怪，然后这里就会变得不稳定，从而导致其他宝箱怪魂飞魄散离开这里！\n\n偶尔还能看见拟态王的幼年形态，但由于这里时空错乱，你无法将它杀死……
        
        actors.buffs.herodisguise.name=伪装术
        actors.buffs.herodisguise.desc=幻术魔法改变了你的外貌！虽然此效果是完全装饰性的，但无论如何感觉起来还是很奇怪。\n\n伪装效果剩余时长：%s回合
      """.trimIndent().replace("\n", "\r\n"),
      rendered
    )
  }

  @Test
  fun `render appends keys missing from template at end`() {
    val template = """
      alpha=1
      beta=2
    """.trimIndent() + "\n"

    val props = Properties().apply {
      setProperty("alpha", "1")
      setProperty("beta", "2")
      setProperty("gamma", "3")
    }

    val rendered = PropertiesTemplateWriter.render(template, props)

    assertEquals(
      """
        alpha=1
        beta=2
        gamma=3
      """.trimIndent() + "\n",
      rendered
    )
  }

  @Test
  fun `render omits deleted keys from template`() {
    val template = """
      alpha=1
      beta=2
    """.trimIndent() + "\n"

    val props = Properties().apply {
      setProperty("alpha", "1")
    }

    val rendered = PropertiesTemplateWriter.render(template, props)

    assertEquals("alpha=1\n", rendered)
  }

  @Test
  fun `render preserves duplicate keys and updates only effective occurrence`() {
    val template = """
      alpha=1
      alpha=2
    """.trimIndent() + "\n"

    val props = Properties().apply {
      setProperty("alpha", "3")
    }

    val rendered = PropertiesTemplateWriter.render(template, props)

    assertEquals(
      """
        alpha=1
        alpha=3
      """.trimIndent() + "\n",
      rendered
    )
  }

  @Test
  fun `render removes comment block that only described deleted property`() {
    val template = """
      # alpha stays
      alpha=1
      # beta removed
      beta=2
    """.trimIndent() + "\n"

    val props = Properties().apply {
      setProperty("alpha", "1")
    }

    val rendered = PropertiesTemplateWriter.render(template, props)

    assertEquals(
      """
        # alpha stays
        alpha=1
      """.trimIndent() + "\n",
      rendered
    )
  }

  @Test
  fun `render keeps shared section comment when first property is deleted but later section property remains`() {
    val template = """
      # actors section
      actors.a=1
      actors.b=2
    """.trimIndent() + "\n"

    val props = Properties().apply {
      setProperty("actors.b", "2")
    }

    val rendered = PropertiesTemplateWriter.render(template, props)

    assertEquals(
      """
        # actors section
        actors.b=2
      """.trimIndent() + "\n",
      rendered
    )
  }
}
