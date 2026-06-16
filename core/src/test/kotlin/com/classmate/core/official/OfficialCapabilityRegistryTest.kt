package com.classmate.core.official

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialCapabilityRegistryTest {
    @Test
    fun registrySeparatesProductSmokeAndExcludedCapabilities() {
        val registry = VivoOfficialProviderRegistry

        assertEquals(20, registry.all.size)
        assertEquals(18, registry.included.size)
        assertEquals(12, registry.productFacing.size)
        assertEquals(6, registry.smokeOnly.size)
        assertEquals(2, registry.excluded.size)

        listOf("OCR", "ASR_LONG", "QUERY_REWRITE", "TEXT_SIMILARITY", "EMBEDDING", "TTS", "TRANSLATION", "FUNCTION_CALLING").forEach {
            assertTrue("missing product-facing $it", registry.byId(it)?.productFacing == true)
        }
        listOf("IMAGE_GENERATION", "VIDEO_GENERATION", "SHORT_ASR", "LONG_DICTATION", "DIALECT_ASR", "SIMULTANEOUS_INTERPRETATION").forEach {
            assertEquals("smoke-only expected for $it", OfficialCapabilityTier.DEV_LAB_SMOKE, registry.byId(it)?.tier)
        }
        listOf("VOICE_CLONE", "GEO_POI").forEach {
            val cap = registry.byId(it)!!
            assertTrue(cap.excluded)
            assertFalse(cap.safeSmokeAllowed)
            assertFalse(cap.productFacing)
        }
    }

    @Test
    fun allSafeSmokeDoesNotIncludeDevLabOrExcludedCapabilities() {
        val safe = VivoOfficialProviderRegistry.allSafeSmoke

        assertTrue(safe.isNotEmpty())
        assertTrue(safe.all { it.tier == OfficialCapabilityTier.PRODUCT_FACING })
        assertFalse(safe.any { it.excluded })
        assertFalse(safe.any { it.id == "VOICE_CLONE" || it.id == "GEO_POI" })
    }
}
