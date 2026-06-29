package com.classmate.core.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisIntensityTest {

    @Test fun readTimeoutsAreOrderedFastStandardDeep() {
        assertTrue(AnalysisIntensity.FAST.readTimeoutMs < AnalysisIntensity.STANDARD.readTimeoutMs)
        assertTrue(AnalysisIntensity.STANDARD.readTimeoutMs < AnalysisIntensity.DEEP.readTimeoutMs)
    }

    @Test fun deepHasTheMostBudgetAndRetries() {
        assertTrue(AnalysisIntensity.DEEP.maxKnowledgePoints >= AnalysisIntensity.STANDARD.maxKnowledgePoints)
        assertTrue(AnalysisIntensity.STANDARD.maxKnowledgePoints >= AnalysisIntensity.FAST.maxKnowledgePoints)
        assertTrue(AnalysisIntensity.DEEP.readRetries >= AnalysisIntensity.STANDARD.readRetries)
    }

    @Test fun profilesMapToCloudQualityProfiles() {
        assertEquals(CloudModelQualityProfile.FAST, AnalysisIntensity.FAST.profile)
        assertEquals(CloudModelQualityProfile.BALANCED, AnalysisIntensity.STANDARD.profile)
        assertEquals(CloudModelQualityProfile.DEEP_STUDY, AnalysisIntensity.DEEP.profile)
    }

    @Test fun fastBudgetIsClearlySmallerThanStandard() {
        // FAST must be materially lighter than STANDARD: fewer output tokens and fewer knowledge points.
        assertTrue(AnalysisIntensity.FAST.profile.maxTokens < AnalysisIntensity.STANDARD.profile.maxTokens)
        assertTrue(AnalysisIntensity.FAST.maxKnowledgePoints < AnalysisIntensity.STANDARD.maxKnowledgePoints)
        // STANDARD must not be as heavy as DEEP (everyday default stays well under the 1–3 min ceiling).
        assertTrue(AnalysisIntensity.STANDARD.readTimeoutMs < AnalysisIntensity.DEEP.readTimeoutMs)
    }

    @Test fun httpTimeoutsReflectTheIntensity() {
        assertEquals(45_000L, AnalysisIntensity.FAST.httpTimeouts().readTimeoutMs)
        assertEquals(240_000L, AnalysisIntensity.DEEP.httpTimeouts().readTimeoutMs)
    }

    @Test fun fromWireIsLenientAndDefaultsToStandard() {
        assertEquals(AnalysisIntensity.STANDARD, AnalysisIntensity.Default)
        assertEquals(AnalysisIntensity.DEEP, AnalysisIntensity.fromWire("deep"))
        assertEquals(AnalysisIntensity.FAST, AnalysisIntensity.fromWire("FAST"))
        assertEquals(AnalysisIntensity.STANDARD, AnalysisIntensity.fromWire(null))
        assertEquals(AnalysisIntensity.STANDARD, AnalysisIntensity.fromWire("nonsense"))
    }
}
