package com.classmate.core.roadmap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityRoadmapTest {
    @Test
    fun roadmapSeparatesConnectedPlannedAndDemoEnhancement() {
        val stages = CapabilityRoadmap.cards.map { it.stage }.toSet()
        val text = CapabilityRoadmap.cards.joinToString("\n") { "${it.title} ${it.description}" }

        assertTrue(CapabilityStage.CONNECTED in stages)
        assertTrue(CapabilityStage.PLANNED in stages)
        assertTrue(CapabilityStage.DEMO_ENHANCEMENT in stages)
        assertTrue(text.contains("qwen3.5-plus"))
        assertTrue(text.contains("Not implemented yet"))
        assertFalse(text.contains("completed ASR", ignoreCase = true))
        assertFalse(text.contains("completed OCR", ignoreCase = true))
        listOf("appKey", "apiKey", "Authorization", "Bearer").forEach {
            assertFalse(text.contains(it, ignoreCase = true))
        }
    }
}
