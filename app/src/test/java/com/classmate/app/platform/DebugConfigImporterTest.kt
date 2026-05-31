package com.classmate.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugConfigImporterTest {

    @Test
    fun placeholderConfigIsParsedButNotConfigured() {
        val json = """
            {"providers":{"bluelm":{"appId":"YOUR_BLUELM_APP_ID","appKey":"YOUR_BLUELM_APP_KEY"},"localFallback":{}}}
        """.trimIndent()
        val preview = DebugConfigImporter.inspect(json)

        assertTrue(preview.valid)
        assertFalse("placeholders must not count as configured", preview.bluelmConfigured)
        assertFalse(preview.containsRealSecret)
        assertTrue(preview.providersFound.contains("bluelm"))
    }

    @Test
    fun realCredentialsAreDetected() {
        val json = """
            {"providers":{"bluelm":{"appId":"abc123id","appKey":"abcdef1234567"}}}
        """.trimIndent()
        val preview = DebugConfigImporter.inspect(json)

        assertTrue(preview.valid)
        assertTrue(preview.bluelmConfigured)
        assertTrue(preview.containsRealSecret)
    }

    @Test
    fun invalidJsonIsReported() {
        val preview = DebugConfigImporter.inspect("definitely not json")
        assertFalse(preview.valid)
    }

    @Test
    fun blankInputIsRejected() {
        assertEquals(false, DebugConfigImporter.inspect("   ").valid)
    }
}
