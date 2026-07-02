package com.classmate.app.ui.screens.course

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F0-3: CourseDetail must stay focused on the high-value learning loop. Low-signal modules were removed
 * ("端侧学习建议") or demoted into a folded "更多操作" drawer ("生成听背文稿" + experimental assets), and the
 * knowledge map is trimmed to a title + entry rather than a verbose edge dump. The core stat chips
 * (knowledge / quiz / wrong-book / review / evidence) must still navigate to their real surfaces.
 */
class CourseDetailModuleGuardTest {

    private val source: String = listOf(
        File("app/src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt"),
        File("src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt"),
    ).first { it.exists() }.readText()

    @Test
    fun deadOrLowValueModulesAreRemovedOrDemoted() {
        // The standalone on-device-advice module is gone (it was a low-value placeholder block).
        assertFalse(
            "CourseDetail should not show the 端侧学习建议 main module",
            source.contains("ProductSectionTitle(\"端侧学习建议\")"),
        )
        // The honest-but-unconfigured audio placeholder must never resurface.
        assertFalse(
            "CourseDetail must not show 音频生成待配置 placeholder",
            source.contains("音频生成待配置"),
        )
        // 生成听背文稿 + experimental assets now live in a folded 更多操作 drawer, not a prominent section.
        assertTrue(
            "Secondary assets should live in a folded 更多操作 drawer",
            source.contains("ProductCollapse(title = \"更多操作"),
        )
        assertFalse(
            "The prominent 学习增强入口 header should be gone (demoted into 更多操作)",
            source.contains("Text(\"学习增强入口\""),
        )
    }

    @Test
    fun lessonRecordsAreNotClickableDeadEnds() {
        // P0-2: tapping a 课堂记录 used to reload the same course into the timeline (a confusing
        // dead-end the user read as a deprecated page). The list is now a read-only summary.
        assertFalse(
            "CourseDetail lesson records must not navigate via openHistoryTimeline",
            source.contains("openHistoryTimeline"),
        )
        assertFalse(
            "LessonRecordCard must not take an onOpen click handler",
            source.contains("LessonRecordCard(record, onOpen"),
        )
    }

    @Test
    fun knowledgeMapIsTrimmedToTitleAndEntry() {
        // The verbose per-edge "A → B" dump is replaced by a count + a single entry button.
        assertFalse(
            "Knowledge map should not dump raw edge text",
            source.contains("\"\$from → \$to\""),
        )
        assertTrue(
            "Knowledge map should offer a single 查看知识结构 entry",
            source.contains("查看知识结构"),
        )
    }

    @Test
    fun coreStatChipsStillNavigate() {
        listOf(
            "Screen.KNOWLEDGE",
            "startPractice(PracticeMode.QUICK_REVIEW)",
            "Screen.REVIEW",
            "openEvidenceById",
        ).forEach {
            assertTrue("CourseDetail stat chips should still navigate via $it", source.contains(it))
        }
        assertFalse("CourseDetail micro-quiz chip should not use the old Quiz screen", source.contains("navigateTo(Screen.QUIZ)"))
    }
}
