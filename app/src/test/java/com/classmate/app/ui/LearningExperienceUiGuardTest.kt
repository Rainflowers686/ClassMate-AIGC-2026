package com.classmate.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LearningExperienceUiGuardTest {
    @Test
    fun courseDetailShowsStudyPackOverviewAndReviewContinuation() {
        val source = File("src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt").readText()

        assertTrue(source.contains("学习状态总览"))
        assertTrue(source.contains("继续复习"))
        assertTrue(source.contains("生成学习包"))
        assertTrue(source.contains("buildLearningStudyPackArtifact"))
    }

    @Test
    fun audioEvidenceShowsLearningLinksAndLowConfidenceGuard() {
        val source = File("src/main/java/com/classmate/app/ui/screens/evidence/EvidenceDetailScreen.kt").readText()
        val strings = File("src/main/java/com/classmate/app/ui/i18n/Strings.kt").readText()

        assertTrue(source.contains("低置信片段") || strings.contains("低置信片段"))
        assertTrue(source.contains("关联知识点") || strings.contains("关联知识点"))
        assertTrue(source.contains("关联错题") || strings.contains("关联错题"))
        assertTrue(source.contains("关联复习任务") || strings.contains("关联复习任务"))
    }
}
