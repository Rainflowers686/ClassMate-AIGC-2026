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
        assertTrue(snapshot.stepLogs.any { it.step == "QUERY_REWRITE" && it.status == "OFFICIAL_SMOKE_PASS_LOCAL_PLANNING" })
        assertTrue(snapshot.embeddingRecords.any { it.ownerType == "QUESTION" && it.providerStatus == "OFFICIAL_SMOKE_PASS_RECORD_SEAM" })
        assertTrue(snapshot.similarityMatches.any { it.providerStatus == "LOCAL_TOKEN_MATCH_OFFICIAL_SMOKE_PASS" })
        assertTrue(snapshot.knowledgeGraphEdges.isNotEmpty())
        assertTrue(snapshot.similarQuestionRecommendations.isNotEmpty())
        assertTrue(snapshot.semanticIndexChunks.any { it.ownerType == "EVIDENCE" })
        assertTrue(snapshot.semanticIndexRecords.any { it.ownerType == "EVIDENCE" })
        assertTrue(snapshot.semanticSearchResults.isNotEmpty())
        assertTrue(snapshot.toolOrchestrationPlan!!.plannedTools.contains("REVIEW_UPDATE"))
        assertTrue(snapshot.toolStepRecords.any { it.toolName == "REVIEW_UPDATE" })
        assertTrue(snapshot.reviewDailyStats.totalKnowledgePoints > 0)
        assertTrue(snapshot.distractorExplanations.isNotEmpty())
        assertTrue(snapshot.diagnostics.any { it.capability == "FUNCTION_CALLING" && it.status == "LOCAL_ORCHESTRATOR" })
        assertTrue(snapshot.actionItems.isNotEmpty())
        assertTrue(snapshot.masteryStats.all { it.nextReviewAt != null && it.sourceLessonId.isNotBlank() })
    }

    @Test
    fun localQuizQuestionsStayBoundToKnowledgeAndEvidence() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = "物理：电磁感应",
            text = """
                法拉第电磁感应定律说明，穿过闭合回路的磁通量发生变化时会产生感应电动势。
                楞次定律用于判断感应电流方向，感应电流的磁场总是阻碍引起它的磁通量变化。
                感应电动势大小与磁通量变化率有关，可以通过实验线圈和电流计观察。
            """.trimIndent(),
            sourceType = L3SourceType.TEXT,
            providerSummary = providerSummary,
            now = now + 21,
        )

        assertTrue(snapshot.questions.isNotEmpty())
        snapshot.questions.forEach { question ->
            assertTrue(question.knowledgePointId.isNotBlank())
            assertTrue(question.evidenceIds.isNotEmpty())
            assertTrue(question.explanation.contains("答案详解"))
            assertTrue(question.explanation.contains("证据摘录") || question.explanation.contains("来源证据"))
            val joined = question.options.joinToString("\n")
            assertFalse(joined.contains("与本节课无关"))
            assertFalse(joined.contains("只需要背诵"))
        }
    }

    @Test
    fun noisyOcrBuildsSubjectKnowledgeNotClassroomPrompts() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = "高等数学导数",
            text = """
                同学们注意
                重点来了
                大家记一下

                导数表示函数在某一点附近的瞬时变化率，切线斜率可以用导数计算。
                极值点需要结合导数符号变化判断。
                页面 右下角 点击 上传 下载
            """.trimIndent(),
            sourceType = L3SourceType.OCR_IMAGE,
            providerSummary = providerSummary,
            now = now + 31,
        )

        val joinedKnowledge = snapshot.knowledgePoints.joinToString("\n") { it.title }
        assertTrue(snapshot.knowledgePoints.isNotEmpty())
        assertTrue(joinedKnowledge.contains("导数") || joinedKnowledge.contains("极值"))
        listOf("同学们注意", "重点来了", "大家记一下", "右下角", "点击").forEach { noise ->
            assertFalse(joinedKnowledge.contains(noise))
            assertFalse(snapshot.reviewQueue.joinToString("\n") { it.arrangementReason + it.recommendedActions.joinToString() }.contains(noise))
            assertFalse(snapshot.questions.joinToString("\n") { it.stem + it.options.joinToString() + it.explanation }.contains(noise))
        }
        assertTrue(snapshot.questions.all { it.knowledgePointId.isNotBlank() && it.evidenceIds.isNotEmpty() })
        assertTrue(snapshot.relatedKnowledgeSummaries.all { it.sourceKnowledgePointTitle.contains("导数") || it.sourceKnowledgePointTitle.contains("极值") })
    }

    @Test
    fun onlyClassroomNoiseDoesNotGenerateFakeKnowledgeOrQuiz() {
        val snapshot = L3LearningPipeline().buildFromText(
            title = "课堂截图",
            text = "同学们注意\n重点来了\n大家记一下\n页面 右下角 点击 上传 下载\n嗯啊呃 好的 然后呢",
            sourceType = L3SourceType.OCR_IMAGE,
            providerSummary = providerSummary,
            now = now + 32,
        )

        assertTrue(snapshot.knowledgePoints.isEmpty())
        assertTrue(snapshot.questions.isEmpty())
        assertTrue(snapshot.reviewQueue.isEmpty())
        assertTrue(snapshot.summary.contains("资料质量较低") || snapshot.summary.contains("资料不足"))
    }

    @Test
    fun learningLoopInputKeepsOcrAssetMetadataOnEvidence() {
        val input = LearningLoopInput(
            id = "input_ocr",
            title = "Board photo",
            kind = LearningLoopInputKind.OCR_IMAGE,
            sourceType = L3SourceType.OCR_IMAGE,
            text = "Faraday law says changing magnetic flux induces voltage.",
            evidenceAssets = listOf(
                EvidenceAsset(
                    id = "asset_image_1",
                    type = EvidenceAssetType.OCR_IMAGE,
                    sourceType = L3SourceType.OCR_IMAGE,
                    text = "Faraday law says changing magnetic flux induces voltage.",
                    sourceLabel = "blackboard photo",
                    fileName = "board.jpg",
                    mimeType = "image/jpeg",
                    imageRef = "board.jpg",
                    thumbnailRef = "board thumbnail",
                    snippet = "Faraday law says changing magnetic flux induces voltage.",
                    status = "OCR_TEXT_CONFIRMED",
                ),
            ),
            sourceLabel = "blackboard photo",
            providerProvenance = "OCR:true",
        )

        val snapshot = L3LearningPipeline().buildFromLearningLoopInput(input, providerSummary, now)

        assertEquals(1, snapshot.evidenceAssets.size)
        assertEquals(EvidenceAssetType.OCR_IMAGE, snapshot.evidenceAssets.single().type)
        assertTrue(snapshot.evidence.isNotEmpty())
        assertTrue(snapshot.evidence.all { it.assetId == "asset_image_1" })
        assertTrue(snapshot.evidence.any { it.sourceType == L3SourceType.OCR_IMAGE && it.imageRef == "board.jpg" })
        assertTrue(snapshot.evidence.any { it.thumbnailRef == "board thumbnail" && it.snippet.contains("Faraday") })
        assertTrue(snapshot.questions.all { it.evidenceIds.isNotEmpty() })
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
        assertTrue(updated.masteryHistory.any { it.eventType == MasteryHistoryEventType.ANSWER_WRONG })
        assertEquals(1, updated.masteryTrendStats.dailyWrongCount)
        assertTrue(updated.wrongBook.single().mistakeReason.contains("错因分析"))
        assertTrue(updated.wrongBook.single().remediationHint.contains("补救建议"))
        assertTrue(updated.wrongBook.single().relatedKnowledgePointIds.contains(question.knowledgePointId))
        assertTrue(updated.reviewQueue.first { it.knowledgePointId == question.knowledgePointId }.arrangementReason.contains("答错"))
        assertEquals(question.evidenceIds.first(), updated.reviewQueue.first { it.knowledgePointId == question.knowledgePointId }.evidenceId)
        assertTrue(updated.learningDiagnosis.weakKnowledgePoints.any { it.knowledgePointId == question.knowledgePointId })
        assertTrue(updated.learningDiagnosis.nextStudyTasks.isNotEmpty())
    }

    @Test
    fun diagnosisAndReviewPlanDegradeWhenEvidenceIsMissing() {
        val pipeline = L3LearningPipeline()
        val snapshot = pipeline.buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)
            .let { base ->
                base.copy(
                    evidence = emptyList(),
                    knowledgePoints = base.knowledgePoints.map { it.copy(sourceEvidenceIds = emptyList()) },
                    questions = base.questions.map { it.copy(evidenceIds = emptyList()) },
                    reviewQueue = base.reviewQueue.map { it.copy(evidenceId = null) },
                )
            }
        val updated = pipeline.submitAnswer(snapshot, snapshot.questions.first().id, "B", now + 2)

        assertEquals(1, updated.wrongBook.size)
        assertTrue(updated.wrongBook.single().evidenceIds.isEmpty())
        assertTrue(updated.wrongBook.single().remediationHint.isNotBlank())
        assertTrue(updated.learningDiagnosis.weakKnowledgePoints.isNotEmpty())
        assertTrue(updated.learningDiagnosis.weakKnowledgePoints.first().evidenceIds.isEmpty())
    }

    @Test
    fun multiChoiceStrictGradingAndShortAnswerSeamAreExplicit() {
        val item = com.classmate.core.practice.PracticeItem(
            id = "item_multi",
            type = com.classmate.core.practice.PracticeItemType.QUIZ_RETRY,
            knowledgePointId = "kp_multi",
            knowledgePointTitle = "Multi",
            taskId = "task_multi",
            question = "Pick two",
            answer = "A,B",
            options = listOf(
                com.classmate.core.practice.PracticeOption("A", "alpha", true),
                com.classmate.core.practice.PracticeOption("B", "beta", true),
                com.classmate.core.practice.PracticeOption("C", "gamma", false),
            ),
        )

        val partial = PracticeGradingEngine.grade(item, listOf("A"))
        assertEquals(PracticeGradingStatus.PARTIAL, partial.status)
        assertFalse(partial.correct)

        val correct = PracticeGradingEngine.grade(item, listOf("B", "A"))
        assertEquals(PracticeGradingStatus.CORRECT, correct.status)
        assertTrue(correct.correct)

        val shortAnswer = PracticeGradingEngine.grade(item.copy(options = emptyList()), emptyList(), textAnswer = "because")
        assertEquals(PracticeGradingStatus.SELF_ASSESSMENT_REQUIRED, shortAnswer.status)
        assertTrue(shortAnswer.message.contains("AI_GRADING_SEAM_ONLY"))
    }

    @Test
    fun importReportPdfPageSemanticIndexExamAndDailyStatsAreBuiltLocally() {
        val artifact = InputArtifact(
            id = "artifact_pdf",
            kind = InputFileKind.PDF,
            fileName = "lesson.pdf",
            status = InputArtifactStatus.PARSER_PENDING,
            message = "PDF artifact ready; manual page text fallback required.",
            createdAt = now,
        )

        val report = InputReportEngine.reportFor(artifact)
        val page = InputReportEngine.pdfPagesFor(artifact).single()
        val manualPage = InputReportEngine.withManualPageText(page, "manual pdf page text", now + 1)

        assertEquals(1, report.warningCount)
        assertTrue(report.fallbackUsed)
        assertEquals(PdfPageStatus.PAGE_OCR_SEAM_READY, page.status)
        assertEquals(PdfPageStatus.MANUAL_PAGE_TEXT_READY, manualPage.status)

        val snapshot = L3LearningPipeline().buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)
        val similarity = SemanticIndexEngine.similarity(snapshot.evidence.first().text, snapshot.knowledgePoints.first().title)
        assertTrue(similarity >= 0.0)
        assertTrue(snapshot.semanticIndexChunks.size >= snapshot.evidence.size)

        val exam = ExamSession(
            id = "exam_1",
            sourceLessonId = snapshot.lessonSource!!.id,
            questionIds = snapshot.questions.take(2).map { it.id },
            startedAt = now,
            submittedAt = now + 30_000,
            status = ExamStatus.SUBMITTED,
            score = 50,
            correctCount = 1,
            wrongCount = 1,
        )
        val submissions = mapOf(
            "item_1" to PracticeAnswerSubmission("item_1", snapshot.questions.first().id, listOf("B"), correct = false, submittedAt = now + 1, mode = PracticeQuestionMode.EXAM, state = PracticeAnswerState.SUBMITTED_WRONG),
        )
        val examReport = ExamReportEngine.build(exam, snapshot, submissions)
        val daily = ReviewStatsEngine.daily(snapshot, now)

        assertEquals(50, examReport.score)
        assertTrue(examReport.wrongQuestionIds.isNotEmpty())
        assertTrue(examReport.markdownReport.contains("Exam Report"))
        assertTrue(examReport.recommendedReviewItems.isNotEmpty())
        assertTrue(examReport.accuracy in 0.0..1.0)
        assertTrue(daily.dueToday > 0)
        assertEquals(snapshot.masteryStats.size, daily.totalKnowledgePoints)
    }

    @Test
    fun finalProductizationEnginesExposeHonestAsrTtsTranslationSemanticAndToolStates() {
        val snapshot = L3LearningPipeline().buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)

        val missingAsr = AsrLongProductizationEngine.createJob("audio_1", ProviderConfigSummary.defaults(), now)
        assertEquals(L3AsrStatus.ASR_NOT_CONFIGURED, missingAsr.status)
        assertEquals("CORE_CONTRACT_PRESENT_CONFIG_MISSING", missingAsr.providerStatus)

        val configuredAsr = AsrLongProductizationEngine.createJob(
            "audio_2",
            ProviderConfigSummary.defaults().copy(
                officialProviders = OfficialProviderConfigSummary(asrLongConfigured = true),
            ),
            now + 1,
        )
        assertEquals(L3AsrStatus.CORE_CONTRACT_PRESENT_APP_WIRING_PENDING, configuredAsr.status)
        assertEquals("APP_ASR_WIRING_PENDING", configuredAsr.errorCode)

        val filled = AsrLongProductizationEngine.applyTranscript(missingAsr, "第一段讲电磁感应。\n第二段讲楞次定律。", "manual_audio", now + 2)
        assertEquals(L3AsrStatus.TRANSCRIPT_READY, filled.status)
        assertTrue(filled.transcriptSegments.isNotEmpty())

        val tts = TtsPlaybackEngine.prepare(snapshot.summary, TtsPlaybackSourceType.SUMMARY, ProviderConfigSummary.defaults(), now)
        assertEquals(TtsPlaybackStatus.LOCAL_TTS_AVAILABLE, tts.status)
        assertEquals(TtsPlaybackProvider.ANDROID_LOCAL_TTS, tts.provider)

        val translation = TranslationProductEngine.prepare(snapshot.evidence.first().text, TranslationTargetLanguage.ENGLISH, snapshot.evidence.first().id, ProviderConfigSummary.defaults(), now)
        assertEquals(TranslationProductStatus.OFFICIAL_TRANSLATION_NOT_CONFIGURED, translation.status)
        assertEquals(snapshot.evidence.first().id, translation.evidenceId)

        val search = LocalSemanticIndexEngine.search(snapshot.semanticIndexRecords, snapshot.knowledgePoints.first().title)
        assertTrue(search.hits.isNotEmpty())
        assertTrue(search.status == "LOCAL_FALLBACK_OFFICIAL_SMOKE_PASS" || search.status == "LOCAL_FALLBACK")

        val toolSteps = ToolOrchestratorProductizationEngine.stepRecords(ToolInputType.PDF, snapshot.copy(pdfPages = listOf(PdfPageArtifact("p1", "a1", 1, PdfPageStatus.PAGE_OCR_SEAM_READY))), providerSummary, now)
        assertTrue(toolSteps.any { it.toolName == "PDF_PAGE_OCR" })
        assertTrue(toolSteps.any { it.providerMode == ToolProviderMode.LOCAL_FALLBACK || it.providerMode == ToolProviderMode.OFFICIAL || it.providerMode == ToolProviderMode.SEAM_ONLY })
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

        val multi = QuestionBankParser.parse(
            """
            Q: 哪些选项属于课堂证据？
            A. 材料原文
            B. 课堂转写
            C. 随机猜测
            D. 无来源结论
            Answer: A,B
            Explanation: 证据必须来自材料或转写。
            """.trimIndent(),
            "多选题",
            now + 3,
        )
        assertTrue(multi.message, multi.accepted)
        assertEquals("A,B", multi.bank!!.questions.single().correctAnswer)
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
