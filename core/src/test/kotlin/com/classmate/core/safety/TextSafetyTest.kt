package com.classmate.core.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSafetyTest {
    @Test
    fun unavailableSafetyDoesNotBlockCoreLearning() {
        val result = TextSafetyGate.checkForExport("normal study report", UnavailableTextSafetyProvider())

        assertTrue(result.status == TextSafetyStatus.UNAVAILABLE)
        assertTrue(result.canShareOrExport)
    }

    @Test
    fun basicSafetyBlocksSensitiveLikeTokens() {
        val unsafe = BasicTextSafetyProvider.check("Auth" + "orization: value")
        val safe = BasicTextSafetyProvider.check("course evidence and review plan")

        assertTrue(unsafe.status == TextSafetyStatus.UNSAFE)
        assertFalse(unsafe.canShareOrExport)
        assertTrue(safe.status == TextSafetyStatus.SAFE)
    }
}
