package com.classmate.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactedLoggerTest {

    @Test
    fun formatContainsOnlyWhitelistedFields() {
        val line = RedactedLogEntry(
            provider = "BLUELM",
            status = "FAIL",
            latencyMs = 42,
            validation = "SKIPPED",
            fallbackUsed = true,
            errorType = "HTTP_NON_2XX",
        ).format()

        assertTrue(line.contains("provider=BLUELM"))
        assertTrue(line.contains("status=FAIL"))
        assertTrue(line.contains("latency_ms=42"))
        assertTrue(line.contains("fallback_used=true"))
        assertTrue(line.contains("error_type=HTTP_NON_2XX"))
        // There is structurally nowhere for a key/prompt/body to appear.
        assertFalse(line.contains("appKey"))
        assertFalse(line.contains("Authorization"))
        assertFalse(line.contains("app_id"))
        assertFalse(line.contains("fake-app-id"))
        assertFalse(line.contains("fake-app-key-for-tests"))
        assertFalse(line.contains("sk-xuanji-FAKE-ONLY-DO-NOT-USE"))
        assertFalse(line.contains("vendor-body"))
        assertFalse(line.contains("级数文本"))
    }

    @Test
    fun inMemoryLoggerAccumulatesEntries() {
        val logger = InMemoryRedactedLogger()
        logger.log(RedactedLogEntry("BLUELM", "FAIL", 1, "SKIPPED", false, "CONFIG_MISSING"))
        logger.log(RedactedLogEntry("LOCAL_FALLBACK", "OK", 3, "PASS", true, null))
        assertEquals(2, logger.entries().size)
        assertEquals("LOCAL_FALLBACK", logger.entries()[1].provider)
    }

    @Test
    fun maskNeverEchoesSecret() {
        assertEquals("***", Redaction.mask("super-secret-value"))
    }
}
