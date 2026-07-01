package com.classmate.app.ui.screens.review

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPlanKnowledgeSummaryTextTest {
    @Test
    fun reviewPlanUsesSubjectKnowledgeCopyInsteadOfTechnicalPipelineCopy() {
        val source = listOf(
            File("src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("本课知识点复习"))
        assertTrue(source.contains("本课核心知识点"))
        assertTrue(source.contains("相关知识点"))
        assertFalse(source.contains("L3 闭环统计"))
        assertFalse(source.contains("provider trace"))
        assertFalse(source.contains("semantic index"))
        assertFalse(source.contains("LOCAL_FALLBACK"))
    }
}
