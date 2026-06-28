package com.classmate.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductLightweightActionsGuardTest {
    private fun read(path: String): String =
        listOf(File(path), File("app/$path")).first { it.exists() }.readText()

    @Test
    fun courseDeleteHasConfirmationAndClearWarning() {
        val history = read("src/main/java/com/classmate/app/ui/screens/history/HistoryScreen.kt")
        val course = read("src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt")
        val state = read("src/main/java/com/classmate/app/state/AppViewModel.kt")

        listOf("删除课程？", "知识点", "题目", "错题", "复习任务", "导出草稿", "本地记录").forEach {
            assertTrue("delete warning missing: $it", history.contains(it) || course.contains(it))
        }
        assertTrue(history.contains("onDelete"))
        assertTrue(course.contains("deleteCurrentCourse"))
        assertTrue(state.contains("deleteCourse(courseKey"))
        assertTrue(state.contains("deleteCourseSessions(sessionIds)"))
        assertTrue(state.contains("课程不存在或已删除"))
    }

    @Test
    fun homeMetricStripAndContinueLearningAreClickable() {
        val home = read("src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt")
        val state = read("src/main/java/com/classmate/app/state/AppViewModel.kt")

        assertTrue(home.contains("onDueClick"))
        assertTrue(home.contains("onHistoryClick"))
        assertTrue(home.contains("onKnowledgeClick"))
        assertTrue(home.contains(".clickable(onClick = click)"))
        assertTrue(home.contains("viewModel.selectTab(Tab.REVIEW)"))
        assertTrue(home.contains("viewModel.selectTab(Tab.HISTORY)"))
        assertTrue(home.contains("viewModel.openLatestKnowledgeFromHome()"))
        assertTrue(home.contains("QuietCard(onClick = { viewModel.openHistory(recent) })"))
        assertTrue(home.contains("pendingRecentDelete"))
        assertTrue(home.contains("viewModel.deleteHistory(recent.id)"))
        assertTrue(state.contains("暂无知识点"))
    }

    @Test
    fun ocrDraftRequiresUserCheckBeforeLearningLoop() {
        val import = read("src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt")
        val state = read("src/main/java/com/classmate/app/state/AppViewModel.kt")
        val strings = read("src/main/java/com/classmate/app/ui/i18n/Strings.kt")

        assertTrue(import.contains("检查识别结果") || strings.contains("检查识别结果"))
        assertTrue(import.contains("建议人工检查识别结果") || strings.contains("建议人工检查识别结果"))
        assertTrue(import.contains("updateImageDraftText"))
        assertTrue(import.contains("confirmImageDraft"))
        assertTrue(state.contains("confirmImageDraft"))
        assertTrue(state.contains("OCR_IMAGE"))
    }

    @Test
    fun knowledgeStructureOutlineDoesNotPromiseFakeMindMap() {
        val course = read("src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt")
        val home = read("src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt")
        val exportFormats = read("src/main/java/com/classmate/app/exporting/ExportFileFormat.kt")
        val exporter = read("src/main/java/com/classmate/app/exporting/ExportCenter.kt")

        assertTrue(course.contains("知识结构大纲"))
        assertTrue(home.contains("知识结构大纲"))
        assertTrue(exportFormats.contains("知识结构大纲"))
        assertTrue(exporter.contains("知识结构大纲"))
        assertFalse(course.contains("已生成思维导图"))
        assertFalse(home.contains("已生成思维导图"))
    }

    @Test
    fun lightweightActionCopyAvoidsEllipsisAndOverlongButtons() {
        val export = read("src/main/java/com/classmate/app/ui/components/ExportCenterCard.kt")
        val settings = read("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")
        val transcript = read("src/main/java/com/classmate/app/ui/screens/transcript/TranscriptImportScreen.kt")
        val practice = read("src/main/java/com/classmate/app/ui/screens/practice/PracticeSessionScreen.kt")
        val strings = read("src/main/java/com/classmate/app/ui/i18n/Strings.kt")
        val searchable = export + settings + transcript + practice + strings

        listOf("保存到文件…", "分享…", "确认转写并生成课堂学习闭环", "进入转写编辑器", "查看答案 / 证据").forEach {
            assertFalse("old long button text remains: $it", searchable.contains(it))
        }
        listOf("保存文件", "分享", "转写编辑", "查看证据", "生成听背脚本", "图片测试").forEach {
            assertTrue("short copy missing: $it", searchable.contains(it))
        }
    }
}
