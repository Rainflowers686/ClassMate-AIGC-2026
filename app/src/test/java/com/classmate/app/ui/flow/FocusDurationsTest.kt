package com.classmate.app.ui.flow

import org.junit.Assert.assertEquals
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
}
