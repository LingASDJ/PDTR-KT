package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringWriter
import java.util.Properties

class PropertiesBomHandlingTest {

  @Test
  fun `loadProperties strips leading BOM before parsing`() {
    val method = TranslatorViewModel::class.java.getDeclaredMethod("loadProperties", String::class.java)
    method.isAccessible = true

    val props = method.invoke(
      allocateTranslatorViewModel(),
      "\uFEFFgreeting=hello\nfarewell=bye"
    ) as Properties

    assertEquals("hello", props.getProperty("greeting"))
    assertEquals("bye", props.getProperty("farewell"))
  }

  @Test
  fun `loadProperties repairs embedded BOM-prefixed keys`() {
    val method = TranslatorViewModel::class.java.getDeclaredMethod("loadProperties", String::class.java)
    method.isAccessible = true

    val content = "alpha=one\n\uFEFFbeta=two\ngamma=three"
    val props = method.invoke(allocateTranslatorViewModel(), content) as Properties

    assertEquals("two", props.getProperty("beta"))
    assertEquals(null, props.getProperty("\uFEFFbeta"))
    assertEquals("one", props.getProperty("alpha"))
    assertEquals("three", props.getProperty("gamma"))
  }

  @Test
  fun `loadProperties prefers clean key over BOM key on collision`() {
    val method = TranslatorViewModel::class.java.getDeclaredMethod("loadProperties", String::class.java)
    method.isAccessible = true

    val content = "foo=clean\n\uFEFFfoo=bommed"
    val props = method.invoke(allocateTranslatorViewModel(), content) as Properties

    assertEquals("clean", props.getProperty("foo"))
  }

  @Test
  fun `write strips BOM from keys before writing`() {
    val props = Properties().apply {
      setProperty("\uFEFFgreeting", "hello")
    }
    val writer = StringWriter()

    PropertiesWriter.write(props, writer)

    assertEquals("greeting=hello\n", writer.toString())
  }

  @Test
  fun `write deduplicates BOM and clean keys`() {
    val props = Properties().apply {
      setProperty("greeting", "clean")
      setProperty("\uFEFFgreeting", "bommed")
    }
    val writer = StringWriter()

    PropertiesWriter.write(props, writer)

    val lines = writer.toString().trim().split("\n")
    assertEquals(1, lines.size)
    assertEquals("greeting=clean", lines[0])
  }

  private fun allocateTranslatorViewModel(): TranslatorViewModel {
    val reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory")
    val getReflectionFactory = reflectionFactoryClass.getDeclaredMethod("getReflectionFactory")
    val reflectionFactory = getReflectionFactory.invoke(null)
    val objectConstructor = Any::class.java.getDeclaredConstructor()
    val newConstructorForSerialization = reflectionFactoryClass.getDeclaredMethod(
      "newConstructorForSerialization",
      Class::class.java,
      java.lang.reflect.Constructor::class.java
    )
    val serializationConstructor = newConstructorForSerialization.invoke(
      reflectionFactory,
      TranslatorViewModel::class.java,
      objectConstructor
    ) as java.lang.reflect.Constructor<TranslatorViewModel>
    serializationConstructor.isAccessible = true
    return serializationConstructor.newInstance()
  }
}
