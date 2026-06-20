package com.classmate.app.l3

import com.classmate.app.platform.OfficialProviderConfigSummary
import com.classmate.app.platform.ProviderConfigSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class L3LearningPipelineTest {
    private val now = 1_700_000_000_000L
    private val providerSummary = ProviderConfigSummary.defaults().copy(
        officialProviders = OfficialProviderConfigSummary(
            ocrConfigured = true,
            queryRewriteConfigured = true,
            textSimilarityConfigured = true,
            embeddingConfigured = true,
        ),
    )

    @Test
    fun textMaterialBuildsEvidenceQuestionsReviewAndProviderSeams() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = L3DemoSeeds.lessonTitle,
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.TEXT,
            providerSummary = providerSummary,
            now = now,
        )

        assertTrue(snapshot.summary.isNotBlank())
        assertTrue(snapshot.evidence.isNotEmpty())
        assertTrue(snapshot.knowledgePoints.isNotEmpty())
        assertTrue(snapshot.questions.size in 3..5)
        assertEquals(snapshot.knowledgePoints.size, snapshot.reviewQueue.size)
        assertTrue(snapshot.questions.all { it.correctAnswer.isNotBlank() && it.evidenceIds.isNotEmpty() })
        assertTrue(snapshot.stepLogs.any { it.step == "QUERY_REWRITE" && it.status == "READY_SEAM_USED" })
        assertTrue(snapshot.embeddingRecords.any { it.ownerType == "QUESTION" && it.providerStatus == "PROVIDER_READY_RECORD" })
        assertTrue(snapshot.similarityMatches.any { it.providerStatus == "PROVIDER_READY_MATCH" })
    }

    @Test
    fun wrongAnswerUpdatesWrongBookReviewQueueAndMastery() {
        val pipeline = L3LearningPipeline()
        val snapshot = pipeline.buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)
        val question = snapshot.questions.first()

        val updated = pipeline.submitAnswer(snapshot, question.id, "B", now + 1)

        assertEquals(1, updated.wrongBook.size)
        assertEquals(question.id, updated.wrongBook.single().questionId)
        assertEquals(L3MasteryState.WEAK, updated.masteryStats.first { it.knowledgePointId == question.knowledgePointId }.state)
        assertEquals(L3MasteryState.WEAK, updated.reviewQueue.first { it.knowledgePointId == question.knowledgePointId }.masteryState)
    }

    @Test
    fun questionBankMarkdownAndCsvParseIntoAnsweredQuestions() {
        val markdown = QuestionBankParser.parse(L3DemoSeeds.questionBankMarkdown, "题库", now)
        assertTrue(markdown.message, markdown.accepted)
        val markdownBank = markdown.bank!!
        assertEquals(3, markdownBank.questions.size)
        assertTrue(markdownBank.questions.all { it.correctAnswer.isNotBlank() && it.explanation.isNotBlank() })

        val csv = QuestionBankParser.parse(
            """
            stem,a,b,c,d,answer,explanation
            法拉第定律描述什么,磁通量变化率,温度,电阻,光强,A,感应电动势与磁通量变化率相关
            """.trimIndent(),
            "CSV 题库",
            now + 1,
        )
        assertTrue(csv.message, csv.accepted)
        assertEquals("A", csv.bank!!.questions.single().correctAnswer)
    }

    @Test
    fun invalidQuestionBankReportsFormatError() {
        val result = QuestionBankParser.parse("只有一行普通文字", "坏题库", now)
        assertFalse(result.accepted)
        assertTrue(result.message.contains("未解析出题目") || result.message.contains("格式"))
    }

    @Test
    fun snapshotCanBeProjectedToExistingCourseArtifactsWithEvidence() {
        val snapshot = L3LearningPipeline().buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)
        val artifacts = L3LearningPipeline().toCourseArtifacts(snapshot, now)

        assertNotNull(artifacts)
        assertTrue(artifacts!!.result.knowledgePoints.all { it.hasEvidence })
        assertTrue(artifacts.result.quizQuestions.all { it.hasEvidence })
    }
}
