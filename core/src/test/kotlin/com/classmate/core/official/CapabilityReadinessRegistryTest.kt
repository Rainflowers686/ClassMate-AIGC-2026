package com.classmate.core.official

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityReadinessRegistryTest {

    private val readiness = CapabilityReadinessRegistry.all
    private val included = VivoOfficialProviderRegistry.included // the 18 non-excluded capabilities

    @Test
    fun coversExactlyTheEighteenIncludedCapabilities() {
        assertEquals(18, included.size)
        assertEquals(18, readiness.size)
        val readinessIds = readiness.map { it.id }.toSet()
        val includedIds = included.map { it.id }.toSet()
        assertEquals("readiness registry must mirror the included official capabilities", includedIds, readinessIds)
    }

    @Test
    fun everyCapabilityHasRoutesEntryEvidenceAndActionInBothLanguages() {
        readiness.forEach { r ->
            assertTrue("${r.id} needs at least one route", r.routes.isNotEmpty())
            assertTrue("${r.id} zh entry blank", r.entryPointZh.isNotBlank())
            assertTrue("${r.id} en entry blank", r.entryPointEn.isNotBlank())
            assertTrue("${r.id} zh evidence blank", r.evidencePolicyZh.isNotBlank())
            assertTrue("${r.id} en evidence blank", r.evidencePolicyEn.isNotBlank())
            assertTrue("${r.id} zh action blank", r.failureActionZh.isNotBlank())
            assertTrue("${r.id} en action blank", r.failureActionEn.isNotBlank())
            // readiness labels are localized.
            assertNotEquals("${r.id} readiness label not localized", r.readinessLabelZh(), r.readinessLabelEn())
        }
    }

    @Test
    fun onlyTheVerifiedMainChainClaimsDeviceReady() {
        // Honesty: nothing may claim TRUE_DEVICE_READY except the device-verified main text chain.
        val deviceReady = readiness.filter { it.readiness == L3Readiness.TRUE_DEVICE_READY }.map { it.id }
        assertEquals(listOf("LARGE_MODEL"), deviceReady)
    }

    @Test
    fun usedInLearningLoopIsLimitedToGenuinelyWiredCapabilities() {
        // Prevents a future "mark everything USED" regression: only the main model + OCR are app-integrated.
        val used = readiness.filter {
            it.readiness == L3Readiness.USED_IN_LEARNING_LOOP || it.readiness == L3Readiness.TRUE_DEVICE_READY
        }.map { it.id }.toSet()
        assertEquals(setOf("LARGE_MODEL", "OCR"), used)
    }

    @Test
    fun experimentalCapabilitiesAreDefaultOff() {
        val experimental = readiness.filter { it.experimentalDefaultOff }.map { it.id }.toSet()
        assertEquals(setOf("IMAGE_GENERATION", "VIDEO_GENERATION", "SIMULTANEOUS_INTERPRETATION"), experimental)
        readiness.filter { it.experimentalDefaultOff }.forEach {
            assertEquals("${it.id} experimental must be EXPERIMENTAL readiness", L3Readiness.EXPERIMENTAL, it.readiness)
        }
    }

    @Test
    fun goldenStandardIsPresentAndComplete() {
        assertEquals(17, CapabilityReadinessRegistry.goldenStandard.size)
        assertTrue(CapabilityReadinessRegistry.goldenStandard.all { it.isNotBlank() })
    }

    @Test
    fun deviceTestPendingCapabilitiesAreFlagged() {
        // The cloud/edge/device-dependent abilities are explicitly marked for real-device spot-checks.
        val pending = CapabilityReadinessRegistry.deviceTestPending.map { it.id }.toSet()
        assertTrue(pending.containsAll(setOf("LARGE_MODEL", "OCR", "ASR_LONG", "ON_DEVICE_3B", "ON_DEVICE_FILES")))
    }
}
