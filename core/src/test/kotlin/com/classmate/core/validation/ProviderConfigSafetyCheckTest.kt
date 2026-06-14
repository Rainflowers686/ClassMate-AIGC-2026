package com.classmate.core.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigSafetyCheckTest {

    @Test
    fun placeholderConfigIsSafe() {
        val json = """
            {"providers":{"bluelm":{"appId":"YOUR_BLUELM_APP_ID","appKey":"YOUR_BLUELM_APP_KEY"}}}
        """.trimIndent()
        val result = ProviderConfigSafetyCheck.inspectExampleConfig(json)
        assertTrue(result.isExampleSafe)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun realLookingKeyIsDetectedByFieldNameOnly() {
        val json = """
            {"providers":{"bluelm":{"appId":"YOUR_BLUELM_APP_ID","appKey":"a1b2c3d4e5f6g7h8"}}}
        """.trimIndent()
        val result = ProviderConfigSafetyCheck.inspectExampleConfig(json)
        assertFalse(result.isExampleSafe)
        // The finding records the FIELD NAME, never the value.
        assertTrue(result.findings.contains("appKey"))
        assertFalse(result.findings.any { it.contains("a1b2c3") })
    }

    @Test
    fun classificationHelpers() {
        assertTrue(ProviderConfigSafetyCheck.isPlaceholder("YOUR_BLUELM_APP_KEY"))
        assertTrue(ProviderConfigSafetyCheck.isPlaceholder(""))
        assertFalse(ProviderConfigSafetyCheck.isRealSecret("YOUR_BLUELM_APP_KEY"))
        assertTrue(ProviderConfigSafetyCheck.isRealSecret("realkey1234567"))
    }
}
