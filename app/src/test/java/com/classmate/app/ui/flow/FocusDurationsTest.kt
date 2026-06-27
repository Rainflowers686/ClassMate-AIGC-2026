package com.classmate.app.ui.flow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusDurationsTest {

    @Test
    fun hasSensiblePresetsAndDefault() {
        assertTrue(FocusDurations.presets.containsAll(listOf(15, 25, 45)))
        assertTrue(FocusDurations.DEFAULT_MIN in FocusDurations.presets)
    }

    @Test
    fun coerceClampsIllegalInput() {
        assertEquals(5, FocusDurations.coerce(0))
        assertEquals(5, FocusDurations.coerce(-30))
        assertEquals(180, FocusDurations.coerce(9999))
        assertEquals(25, FocusDurations.coerce(25))
    }

    @Test
    fun labelIsChinese() {
        assertEquals("25 分钟", FocusDurations.label(25))
    }

    // F0-9: custom focus length — any whole number in 1..180 is accepted; everything else is rejected
    // (the UI then shows the Chinese hint instead of starting a bogus timer).
    @Test
    fun parseCustomAcceptsInRangeMinutes() {
        assertEquals(37, FocusDurations.parseCustom("37"))
        assertEquals(1, FocusDurations.parseCustom("1"))
        assertEquals(180, FocusDurations.parseCustom("180"))
        assertEquals(37, FocusDurations.parseCustom(" 37 "))
    }

    @Test
    fun parseCustomRejectsIllegalInput() {
        assertNull(FocusDurations.parseCustom("0"))
        assertNull(FocusDurations.parseCustom("999"))
        assertNull(FocusDurations.parseCustom("-5"))
        assertNull(FocusDurations.parseCustom(""))
        assertNull(FocusDurations.parseCustom("abc"))
        assertNull(FocusDurations.parseCustom("12.5"))
    }

    @Test
    fun customHintIsChinese() {
        assertTrue(FocusDurations.customHint.contains("分钟"))
        assertTrue(FocusDurations.customHint.contains("1"))
        assertTrue(FocusDurations.customHint.contains("180"))
    }
}
