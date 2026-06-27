package com.classmate.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-1/P0-2/P0-3: the enhancement prompts and local templates must be grounded in real data, Chinese, and
 * never leak ids/debug tokens. The local study-pack forms are genuine structured material, not a stub.
 */
class EnhancementContentTest {

    private val points = listOf(
        EnhancementPoint("牛顿第二定律", "F = ma：合外力等于质量乘以加速度", "实验记录显示 a 与 F 成正比"),
        EnhancementPoint("受力分析", "先确定研究对象，再画出所有外力", ""),
    )

    @Test
    fun handoutPromptIsGroundedChineseAndCarriesTheData() {
        val p = EnhancementPromptBuilder.studyPackHandout("物理 · 第3讲", points)
        assertTrue(p.system.contains("讲义"))
        assertTrue("prompt forbids fabrication", p.system.contains("不要编造"))
        assertTrue(p.user.contains("牛顿第二定律"))
        assertTrue(p.user.contains("物理 · 第3讲"))
    }

    @Test
    fun evidenceExplanationPromptHedgesWeakLinks() {
        val weak = EnhancementPromptBuilder.evidenceExplanation("牛顿第二定律", "a 与 F 成正比", null, weak = true)
        val strong = EnhancementPromptBuilder.evidenceExplanation("牛顿第二定律", "a 与 F 成正比", null, weak = false)
        assertTrue("weak prompt asks to flag 待核对", weak.system.contains("核对") || weak.user.contains("待核对"))
        assertFalse("strong prompt is not hedged in the system role", strong.system.contains("关联较弱"))
    }

    @Test
    fun localHandoutIsRealStructuredMaterial() {
        val handout = LocalEnhancementTemplates.studyPackHandout("物理 · 第3讲", points)
        assertTrue(handout.contains("讲义版"))
        assertTrue("includes a knowledge point", handout.contains("牛顿第二定律"))
        assertTrue("includes its evidence", handout.contains("成正比"))
        assertTrue("has review guidance", handout.contains("复习建议"))
    }

    @Test
    fun localCramSheetIsCompactWithExamTips() {
        val cram = LocalEnhancementTemplates.examCramSheet("物理 · 第3讲", points)
        assertTrue(cram.contains("速记版"))
        assertTrue(cram.contains("考前提示"))
    }

    @Test
    fun emptyPointsDegradeHonestly() {
        assertTrue(LocalEnhancementTemplates.studyPackHandout("空课程", emptyList()).contains("暂无知识点"))
        assertTrue(LocalEnhancementTemplates.examCramSheet("空课程", emptyList()).contains("暂无知识点"))
    }
}
