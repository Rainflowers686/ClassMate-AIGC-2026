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
            {"providers":{"bluelm":{"baseUrl":"https://fake-blue-lm.test","model":"fake-blue-model","appId":"fake-app-id","appKey":"fake-app-key-for-tests"}}}
        """.trimIndent()
        val preview = DebugConfigImporter.inspect(json)

        assertTrue(preview.valid)
        assertTrue(preview.bluelmConfigured)
        assertTrue(preview.containsRealSecret)
        assertTrue(preview.providerSummaries.any { it.provider == "BLUELM" && it.credentialPresent })
        assertFalse(preview.toString().contains("fake-app-key-for-tests"))
        assertFalse(preview.toString().contains("fake-app-id"))
    }

    @Test
    fun releaseGuardRejectsImport() {
        val preview = DebugConfigImporter.inspect(
            """{"providers":{"bluelm":{"appId":"fake-app-id","appKey":"fake-app-key-for-tests"}}}""",
            debugEnabled = false,
        )

        assertFalse(preview.valid)
        assertFalse(preview.bluelmConfigured)
        assertEquals(null, preview.runtimeConfig)
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
