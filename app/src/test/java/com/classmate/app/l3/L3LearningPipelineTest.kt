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
        assertTrue(snapshot.knowledgeGraphEdges.isNotEmpty())
        assertTrue(snapshot.similarQuestionRecommendations.isNotEmpty())
        assertTrue(snapshot.diagnostics.any { it.capability == "FUNCTION_CALLING" && it.status == "LOCAL_ORCHESTRATOR" })
        assertTrue(snapshot.actionItems.isNotEmpty())
        assertTrue(snapshot.masteryStats.all { it.nextReviewAt != null && it.sourceLessonId.isNotBlank() })
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
        assertEquals(now + 1, updated.reviewQueue.first { it.knowledgePointId == question.knowledgePointId }.dueAt)
        assertEquals(3, updated.reviewQueue.first { it.knowledgePointId == question.knowledgePointId }.priority)
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

        val tf = QuestionBankParser.parse(
            """
            Q: 法拉第定律和磁通量变化有关。
            Answer: True
            Explanation: 该判断正确。
            """.trimIndent(),
            "判断题",
            now + 2,
        )
        assertTrue(tf.message, tf.accepted)
        assertEquals(listOf("A. 正确", "B. 错误"), tf.bank!!.questions.single().options)
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

    @Test
    fun officialToolSeamsExposeTranslationTtsOrchestratorAndEdgeFallback() {
        val snapshot = L3LearningPipeline().buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)

        assertTrue(snapshot.officialToolSeams.map { it.capability }.containsAll(listOf("TRANSLATION", "TTS", "FUNCTION_CALLING", "ASR_LONG", "EDGE_MODEL")))

        val translation = L3OfficialToolSeams.translate("hello", "en", "zh-CHS", providerSummary)
        assertEquals(OfficialToolSeamStatus.NOT_CONFIGURED, translation.status)
        assertTrue(translation.translatedText.isBlank())

        val tts = L3OfficialToolSeams.prepareListenReview(snapshot.summary, providerSummary)
        assertEquals(OfficialToolSeamStatus.OFFICIAL_TTS_NOT_CONFIGURED, tts.status)
        assertFalse(tts.officialConfigured)

        val plan = L3OfficialToolSeams.orchestrate("L3 study pipeline", snapshot, providerSummary, now + 10)
        assertEquals(OfficialToolSeamStatus.LOCAL_ORCHESTRATOR, plan.status)
        assertTrue(plan.plannedTools.containsAll(listOf("QUERY_REWRITE", "EMBEDDING", "TEXT_SIMILARITY", "QUESTION_GENERATION", "REVIEW_UPDATE")))
        assertTrue(plan.executedSteps.all { it.provider == "local.orchestrator" })

        val edge = L3OfficialToolSeams.edgeStudyFallback(snapshot.summary)
        assertEquals(OfficialToolSeamStatus.LOCAL_RULE_FALLBACK, edge.status)
        assertTrue(edge.output.isNotBlank())
    }
}
