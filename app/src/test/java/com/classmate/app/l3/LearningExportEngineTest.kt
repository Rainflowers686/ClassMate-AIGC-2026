package com.classmate.app.l3

import com.classmate.app.exporting.ExportCenter
import com.classmate.app.exporting.ExportFileFormat
import com.classmate.core.model.Difficulty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningExportEngineTest {
    @Test
    fun studyPackExportContainsLearningLoopAssetsAndHidesInternalTerms() {
        val now = 1_700_000_000_000L
        val evidence = Evidence(
            id = "ev_text_1",
            sourceId = "lesson",
            sourceType = L3SourceType.TEXT,
            text = "TCP provides reliable transport for HTTP.",
            sourceLabel = "pasted class note",
            snippet = "TCP provides reliable transport",
        )
        val snapshot = L3PipelineSnapshot(
            lessonSource = LessonSource("lesson", "Networking", L3SourceType.TEXT, now, "TCP provides reliable transport.", "READY"),
            summary = "TCP and HTTP are related protocol concepts.",
            keyTakeaways = listOf("TCP reliability supports HTTP requests."),
            evidence = listOf(evidence),
            knowledgePoints = listOf(
                L3KnowledgePoint("kp_tcp", "TCP reliability", "TCP retransmits lost packets.", listOf(evidence.id), L3MasteryState.WEAK),
            ),
            questions = listOf(
                L3GeneratedQuestion(
                    id = "q_tcp",
                    lessonId = "lesson",
                    knowledgePointId = "kp_tcp",
                    stem = "Which protocol provides reliable transport?",
                    options = listOf("UDP", "TCP", "DNS", "ARP"),
                    correctAnswer = "TCP",
                    explanation = "TCP is reliable and ordered.",
                    evidenceIds = listOf(evidence.id),
                    difficulty = Difficulty.EASY,
                ),
            ),
            wrongBook = listOf(
                WrongQuestionRecord(
                    id = "wrong_tcp",
                    questionId = "q_tcp",
                    userAnswer = "UDP",
                    correctAnswer = "TCP",
                    explanation = "Review the evidence about reliable transport.",
                    knowledgePointId = "kp_tcp",
                    evidenceIds = listOf(evidence.id),
                    createdAt = now,
                    retryCount = 0,
                    mistakeReason = "Confused connectionless and reliable transport.",
                    remediationHint = "Re-read the evidence and retry the TCP question.",
                ),
            ),
            reviewQueue = listOf(
                ReviewQueueItem(
                    id = "review_tcp",
                    knowledgePointId = "kp_tcp",
                    dueAt = now,
                    masteryState = L3MasteryState.WEAK,
                    sourceLessonId = "lesson",
                    priority = 3,
                    arrangementReason = "Wrong answer raised the review priority.",
                    evidenceId = evidence.id,
                    recommendedActions = listOf("Read evidence", "Retry the quiz"),
                ),
            ),
            learningDiagnosis = LearningDiagnosis(
                weakKnowledgePoints = listOf(WeakKnowledgeDiagnosis("kp_tcp", "TCP reliability", "Wrong answer on protocol reliability.", listOf(evidence.id), 1, 3)),
                recentReviewPressure = "One weak knowledge point needs review today.",
                nextStudyTasks = listOf("Retry TCP reliability."),
                evidenceIds = listOf(evidence.id),
                generatedAt = now,
            ),
        )

        val markdown = LearningExportEngine.buildStudyPackMarkdown(snapshot, now)

        // The study pack is a clean Chinese learning document with answers + explanations.
        listOf(
            "AI 整理摘要",
            "知识点",
            "微测题（含答案与解析）",
            "正确答案：",
            "解析：",
            "错题本",
            "20 分钟复习计划",
            "学习诊断",
            "证据索引",
            "学习建议",
        ).forEach { assertTrue("study pack should contain Chinese section: $it", markdown.contains(it)) }

        // No English debug headings and no raw evidence/question ids leak into the export.
        listOf(
            "Knowledge points",
            "Micro quiz",
            "Evidence source index",
            "Capability usage note",
            "Correct answer",
            "Wrong book",
            "ev_text_1",
            "q_tcp",
            "kp_tcp",
        ).forEach { assertFalse("study pack must not contain debug/raw token: $it", markdown.contains(it)) }

        listOf("AppKey", "Authorization", "config.local.json", "adapter injected", "smoke pass").forEach {
            assertFalse(markdown.contains(it, ignoreCase = true))
        }

        val pdf = ExportCenter.artifactFromMarkdown("Networking", markdown, ExportFileFormat.PDF, createdAt = now)
        val word = ExportCenter.artifactFromMarkdown("Networking", markdown, ExportFileFormat.WORD_COMPAT_HTML, createdAt = now)

        assertTrue(pdf.fileName.endsWith(".pdf"))
        assertTrue(word.fileName.endsWith(".html"))
        assertFalse(pdf.containsSensitiveContent)
        assertFalse(word.containsSensitiveContent)
    }
}
