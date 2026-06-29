package com.classmate.core.exporting

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.PolishedStudyMaterial
import com.classmate.core.ai.PolishedStudyPackInput
import com.classmate.core.ai.PolishedStudyPackPromptBuilder
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.provider.HttpTimeouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishedStudyPackTest {

    private fun input() = PolishedStudyPackInput(
        courseTitle = "高等数学 - 数项级数",
        sourceSummary = "课堂文本、OCR 图片",
        knowledgePoints = listOf(
            PolishedStudyMaterial("比值判别法", "用相邻项比值的极限判断收敛", "若 lim |a(n+1)/a(n)| < 1 则收敛"),
        ),
        quizStems = listOf("比值判别法在极限等于 1 时能否判断收敛？"),
        weakPoints = listOf("比值判别法"),
        lowConfidenceNotes = listOf("OCR 模糊：根值判别法定义"),
        hasImageMaterial = true,
    )

    @Test
    fun promptIsGroundedAndAsksForAStructuredPack() {
        val prompt = PolishedStudyPackPromptBuilder.build(input())
        // grounded, anti-fabrication contract
        assertTrue(prompt.system.contains("严格只依据") || prompt.system.contains("不要编造"))
        assertTrue(prompt.user.contains("待核对"))
        // uses the REAL material, not invented content
        assertTrue(prompt.user.contains("高等数学 - 数项级数"))
        assertTrue(prompt.user.contains("比值判别法"))
        assertTrue(prompt.user.contains("lim |a(n+1)/a(n)| < 1"))
        // asks for the premium multi-section structure
        assertTrue(prompt.user.contains("考前速记版"))
        assertTrue(prompt.user.contains("复习计划"))
        assertTrue(prompt.user.contains("薄弱点建议"))
        assertTrue(prompt.user.contains("下一步学习建议"))
    }

    @Test
    fun promptForbidsIdsAndDebugTokens() {
        val prompt = PolishedStudyPackPromptBuilder.build(input())
        assertTrue(prompt.system.contains("不要输出任何编号 id"))
    }

    @Test
    fun polishedTimeoutIsAlwaysTheLongTaskTimeoutNeverThe30sEnhancement() {
        // The polished pass reuses the chosen intensity but is clamped 120s..240s; never the 30s diagnostic.
        AnalysisIntensity.entries.forEach { intensity ->
            val read = PolishedExportPlan.timeouts(intensity).readTimeoutMs
            assertTrue("$intensity >= 120s", read >= 120_000L)
            assertTrue("$intensity <= 240s", read <= 240_000L)
            assertFalse("$intensity is not the 30s enhancement", read == HttpTimeouts.BLUE_LM_DIAGNOSTIC.readTimeoutMs)
        }
        assertEquals(120_000L, PolishedExportPlan.timeouts(AnalysisIntensity.FAST).readTimeoutMs)
        assertEquals(240_000L, PolishedExportPlan.timeouts(AnalysisIntensity.DEEP).readTimeoutMs)
    }

    @Test
    fun sourceLabelNeverPassesLocalOffAsBlueLm() {
        assertEquals("蓝心精修版", PolishedExportPlan.sourceLabel(AiExecutionSource.CLOUD))
        assertEquals("端侧精修草稿", PolishedExportPlan.sourceLabel(AiExecutionSource.ON_DEVICE))
        val local = PolishedExportPlan.sourceLabel(AiExecutionSource.SAFE_PLACEHOLDER)
        assertEquals("本地整理版", local)
        assertFalse("local organize is never labelled 蓝心", local.contains("蓝心"))
    }

    @Test
    fun headedMarkdownCarriesHonestSourceAndTitle() {
        val pack = PolishedStudyPack(
            courseTitle = "高等数学",
            sourceLabel = "本地整理版",
            generatedAtLabel = "2026-06-29 10:00",
            markdown = "## 核心知识结构\n- 比值判别法",
        )
        val md = pack.headedMarkdown()
        assertTrue(md.contains("整理来源：本地整理版"))
        assertTrue(md.contains("高等数学"))
        assertTrue(md.contains("比值判别法"))
        assertFalse("a local pack header never claims 蓝心", md.contains("蓝心"))
    }
}
