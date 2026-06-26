package com.classmate.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps developer/debug text out of the ordinary learning pages a student sees, and keeps demo data
 * honestly labelled. Source-scan guard (real-device cleanup round) so the leaks cannot regress.
 */
class UserPageDebugLeakGuardTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing $rel")

    private val review by lazy { read("app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt") }
    private val home by lazy { read("app/src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt") }
    private val asrEngine by lazy { read("app/src/main/java/com/classmate/app/l3/L3ProductizationEngines.kt") }
    private val transcript by lazy { read("app/src/main/java/com/classmate/app/ui/screens/transcript/TranscriptImportScreen.kt") }

    @Test
    fun reviewPageHasNoDebugOrProviderTrace() {
        listOf(
            "Evidence chain",
            "语义检索",
            "recentSevenDaySummary",
            "mastery events",
            "7-day trend",
            "task.sourceProfile",
            "task.sourceModel",
            "OnDeviceReviewSuggestionCard",
            "生成端侧复习建议",
        ).forEach { banned ->
            assertFalse("ReviewPlanScreen still leaks: $banned", review.contains(banned))
        }
    }

    @Test
    fun reviewPageDoesNotShowRawKnowledgePointIdsAsTitles() {
        // Raw ids like kp_123 must not be the fallback title shown to users.
        assertFalse(review.contains("?: item.knowledgePointId"))
        assertFalse(review.contains("?: wrong.questionId"))
    }

    @Test
    fun homeContinueRecordUsesFriendlyRecordLabel() {
        // The saved-record chip must use recordLabelZh (本地整理), not sourceLabelZh (安全占位).
        assertTrue("home continue card should use recordLabelZh", home.contains("recordLabelZh(recent.providerName)"))
    }

    @Test
    fun asrFallbackMessageIsChineseNotEnglishDiagnostic() {
        assertFalse(asrEngine.contains("Core ASR Long contract exists"))
        assertFalse(asrEngine.contains("Core VivoAsrProvider doc 1739 contract exists"))
        assertTrue("ASR fallback must be Chinese product copy", asrEngine.contains("已切换为手动转写模式"))
    }

    @Test
    fun transcriptGlossaryDistinguishesDemoFromReal() {
        assertTrue("built-in glossary must be marked as demo", transcript.contains("演示数据"))
        assertTrue("real extraction must be labelled from the transcript", transcript.contains("从当前转写稿提取"))
        // The old misleading "本课程术语：" framing (demo shown as real) must be gone.
        assertFalse(transcript.contains("\"本课程术语："))
    }
}
