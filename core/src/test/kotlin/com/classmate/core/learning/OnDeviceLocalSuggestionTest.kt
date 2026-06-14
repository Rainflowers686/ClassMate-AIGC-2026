package com.classmate.core.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceLocalSuggestionTest {

    @Test
    fun onDeviceTextIsLabelledAsOnDeviceGenerated() {
        val labelled = OnDeviceLocalSuggestion.label("先复习级数判别法，再做 2 道同类题。")
        assertEquals("由端侧 BlueLM 生成：先复习级数判别法，再做 2 道同类题。", labelled)
        assertTrue(OnDeviceLocalSuggestion.isOnDevice(labelled))
    }

    @Test
    fun unavailableModelFallsBackToFixedSafetyPlaceholderNeverFabricated() {
        assertEquals(OnDeviceLocalSuggestion.SAFETY_PLACEHOLDER, OnDeviceLocalSuggestion.label(null))
        assertEquals(OnDeviceLocalSuggestion.SAFETY_PLACEHOLDER, OnDeviceLocalSuggestion.label(""))
        assertEquals(OnDeviceLocalSuggestion.SAFETY_PLACEHOLDER, OnDeviceLocalSuggestion.label("   "))
        assertFalse(OnDeviceLocalSuggestion.isOnDevice(OnDeviceLocalSuggestion.SAFETY_PLACEHOLDER))
        assertEquals("模型不可用，建议先复习高优先级任务并重新测试。", OnDeviceLocalSuggestion.SAFETY_PLACEHOLDER)
    }

    @Test
    fun reportPromptCarriesTopicsButNoSecrets() {
        val prompt = OnDeviceLocalSuggestion.buildReportPrompt("高数：级数", listOf("p 级数", "比较判别法"), dueCount = 3)
        assertTrue(prompt.contains("高数：级数"))
        assertTrue(prompt.contains("p 级数"))
        listOf("appKey", "Authorization", "Bearer", "reasoning_content").forEach {
            assertFalse(prompt.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun practicePromptCarriesWeakTopicsAndWrongCount() {
        val prompt = OnDeviceLocalSuggestion.buildPracticePrompt("C++ 指针", listOf("野指针"), wrongCount = 2)
        assertTrue(prompt.contains("C++ 指针"))
        assertTrue(prompt.contains("野指针"))
        assertTrue(prompt.contains("2"))
    }
}
