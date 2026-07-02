package com.classmate.app.l3

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectKnowledgeExtractorTest {

    @Test
    fun emphasisWordsDoNotBecomeKnowledgeTitles() {
        val paragraphs = listOf(
            "同学们注意",
            "重点来了",
            "大家记一下",
            "导数表示函数在某一点附近的瞬时变化率。",
        )

        val filtered = SubjectKnowledgeExtractor.filterEvidenceParagraphs(paragraphs, "高等数学")

        assertFalse(filtered.any { it == "同学们注意" || it == "重点来了" || it == "大家记一下" })
        assertTrue(filtered.any { it.contains("导数") && it.contains("瞬时变化率") })
    }

    @Test
    fun noisyOcrKeepsSubjectTermsAndDropsUiNoise() {
        val paragraphs = listOf(
            "截图 页面 右下角 点击 上传 下载",
            "牛顿第二定律说明物体加速度与所受合力成正比，与质量成反比。",
            "作业要求 拍照 提交",
            "二叉树的遍历包括前序、中序和后序遍历。",
        )

        val filtered = SubjectKnowledgeExtractor.filterEvidenceParagraphs(paragraphs, "物理与数据结构")

        assertTrue(filtered.any { it.contains("牛顿第二定律") })
        assertTrue(filtered.any { it.contains("二叉树") })
        assertFalse(filtered.any { it.contains("右下角") || it.contains("作业要求") })
    }

    @Test
    fun acceptedKnowledgeRequiresEvidenceBackedSubjectSignal() {
        assertFalse(SubjectKnowledgeExtractor.isAcceptedKnowledge("同学们注意", "同学们注意", "课堂"))
        assertTrue(SubjectKnowledgeExtractor.isAcceptedKnowledge("化学需氧量", "化学需氧量用于衡量水体中可被氧化物质的含量。", "环境监测"))
        assertTrue(SubjectKnowledgeExtractor.isAcceptedKnowledge("TCP 三次握手", "TCP 三次握手用于建立可靠连接。", "计算机网络"))
    }

    @Test
    fun titleExtractionStripsLeadingClassroomPrompt() {
        val title = SubjectKnowledgeExtractor.titleFromEvidence("同学们注意：极限描述自变量趋近某一点时函数值的变化趋势。", 0, "高等数学")

        assertTrue(title.contains("极限"))
        assertFalse(title.contains("同学们注意"))
    }

    @Test
    fun mixedFreshOcrKeepsSubjectCandidateButCleansPromptFragments() {
        val text = "同学们注意，重点来了，这个地方可能考。下面看牛顿第二定律，物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。大家记一下，作业截图上传。"

        val candidates = SubjectKnowledgeExtractor.subjectKnowledgeCandidates(text, "高中物理")
        val title = SubjectKnowledgeExtractor.titleFromEvidence(text, 0, "高中物理")
        val display = SubjectKnowledgeExtractor.cleanSubjectTextForDisplay("下面看牛顿第二定律，公式 F=ma")

        assertTrue(candidates.any { it.contains("牛顿第二定律") || it.contains("加速度") || it.contains("F=ma") })
        listOf("同学们注意", "重点来了", "大家记一下", "作业截图上传", "下面看").forEach { noise ->
            assertFalse("candidate leaked $noise", candidates.joinToString("\n").contains(noise))
            assertFalse("title leaked $noise", title.contains(noise))
            assertFalse("display leaked $noise", display.contains(noise))
        }
        assertTrue(title.contains("牛顿第二定律") || title.contains("加速度") || title.contains("F=ma"))
    }
}
