package com.example.pdtranslator

import org.junit.Assert.*
import org.junit.Test

class CalibrationStoreTest {

    @Test
    fun `put and get calibration entry`() {
        var store = CalibrationStore()
        store = store.put("key1", CalibrationEntry("wrong text", "correct text", 1L))
        val entry = store.get("key1")
        assertNotNull(entry)
        assertEquals("wrong text", entry!!.originalText)
        assertEquals("correct text", entry.calibratedText)
    }

    @Test
    fun `get returns null for missing key`() {
        val store = CalibrationStore()
        assertNull(store.get("nonexistent"))
    }

    @Test
    fun `remove calibration entry`() {
        var store = CalibrationStore()
        store = store.put("key1", CalibrationEntry("wrong", "correct", 1L))
        store = store.remove("key1")
        assertNull(store.get("key1"))
    }

    @Test
    fun `clear removes all entries`() {
        var store = CalibrationStore()
        store = store.put("key1", CalibrationEntry("a", "b", 1L))
        store = store.put("key2", CalibrationEntry("c", "d", 2L))
        store = store.clear()
        assertEquals(0, store.count)
    }

    @Test
    fun `merge adds new entries and overwrites existing`() {
        var store = CalibrationStore()
        store = store.put("key1", CalibrationEntry("old", "old_cal", 1L))
        val incoming = mapOf(
            "key1" to CalibrationEntry("new", "new_cal", 2L),
            "key2" to CalibrationEntry("a", "b", 3L)
        )
        store = store.merge(incoming)
        assertEquals("new_cal", store.get("key1")!!.calibratedText)
        assertEquals("b", store.get("key2")!!.calibratedText)
    }

    @Test
    fun `upsert preserves first original text when recalibrating same key`() {
        var store = CalibrationStore()
        store = store.upsert("key1", "wrong text", "fixed text", 1L)
        store = store.upsert("key1", "fixed text", "fixed again", 2L)

        val entry = store.get("key1")
        assertNotNull(entry)
        assertEquals("wrong text", entry!!.originalText)
        assertEquals("fixed again", entry.calibratedText)
        assertEquals(2L, entry.timestamp)
    }
}
