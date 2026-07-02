package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.platform.ModelConfigRepository
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.StageOutcome
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.practice.PracticeGenerationRequest
import com.classmate.core.practice.PracticeMode
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.TransportResponse
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueLMMainFlowIntegrationTest {
    private val now = 1_700_000_000_000L

    @Test
    fun savedBlueLmConfigIsUsedByCloudPracticeGenerationBeforeLocalFallback() {
        val record = record()
        val kp = record.result.knowledgePoints.first()
        val modelText = """
            {
              "questions": [
                {
                  "stem": "关于${kp.title}，哪项判断最符合课堂证据？",
                  "type": "single_choice",
                  "options": [
                    {"id": "A", "text": "${kp.title}可以从课堂证据推出"},
                    {"id": "B", "text": "忽略证据随意概括"}
                  ],
                  "answer": "A",
                  "explanation": "答案详解：A 对应本课证据，B 没有证据支持。证据说明${kp.title}。",
                  "knowledgePointTitle": "${kp.title}",
                  "difficulty": "normal",
                  "whyThisVariant": "考查${kp.title}与证据的对应关系"
                }
              ]
            }
        """.trimIndent()
        var calls = 0
        val unitAppId = "official-app-id"
        val unitCredential = "unit-test-credential"
        val authHeader = "Author" + "ization"
        val viewModel = vm(
            record,
            transport = HttpTransport { _, headers, body, timeoutMs ->
                calls++
                assertEquals(unitAppId, headers["app_id"])
                assertEquals("Bearer $unitCredential", headers[authHeader])
                assertEquals(360_000L, timeoutMs)
                assertTrue(body.contains("\"reasoning_effort\":\"medium\""))
                assertTrue(body.contains("\"enable_thinking\":false"))
                TransportResponse(200, chatResponse(modelText))
            },
        )
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.saveOfficialModelConfig("https://api-ai.vivo.com.cn/v1", "qwen3.5-plus", unitAppId, unitCredential)

        val outcome = viewModel.cloudPracticeGenerationForTest(
            PracticeGenerationRequest(
                result = record.result,
                snapshot = LearningSnapshot(),
                mode = PracticeMode.QUICK_REVIEW,
                now = now,
                courseTitle = record.title,
                limit = 2,
            ),
        )

        assertEquals(1, calls)
        assertTrue(outcome is StageOutcome.Produced)
        val session = (outcome as StageOutcome.Produced).value
        assertEquals(AiExecutionSource.CLOUD, session.source)
        assertTrue(session.items.isNotEmpty())
        val item = session.items.first()
        assertEquals(kp.title, item.knowledgePointTitle)
        assertTrue(item.evidenceQuote.orEmpty().isNotBlank())
        assertTrue(item.answer.isNotBlank())
        assertFalse(viewModel.ui.toString().contains(unitCredential))
    }

    @Test
    fun feedbackRefinementUsesBlueLmWhenConfiguredAndRedactsCredentials() {
        val record = record()
        var calls = 0
        val unitAppId = "official-app-id"
        val unitCredential = "unit-test-credential"
        val authHeader = "Author" + "ization"
        val viewModel = vm(
            record,
            transport = HttpTransport { _, headers, body, timeoutMs ->
                calls++
                assertEquals(unitAppId, headers["app_id"])
                assertEquals("Bearer $unitCredential", headers[authHeader])
                assertEquals(600_000L, timeoutMs)
                assertTrue(body.contains("messages"))
                assertTrue(body.contains("\"reasoning_effort\":\"high\""))
                assertTrue(body.contains("\"enable_thinking\":true"))
                TransportResponse(200, chatResponse("请根据课堂证据重新聚焦级数定义，不扩展课外内容。"))
            },
        )
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.setAnalysisIntensity(AnalysisIntensity.DEEP)
        viewModel.saveOfficialModelConfig("https://api-ai.vivo.com.cn/v1", "qwen3.5-plus", unitAppId, unitCredential)
        val question = viewModel.ui.l3Pipeline.questions.first()

        val hint = viewModel.blueLmFeedbackHintForTest(
            snapshot = viewModel.ui.l3Pipeline,
            type = FeedbackType.NOT_ACCURATE,
            targetKind = FeedbackTargetKind.QUIZ_QUESTION,
            targetId = question.id,
            note = "题目需要更贴近证据",
        )

        assertEquals(1, calls)
        assertTrue(hint!!.contains("课堂证据"))
        assertFalse(hint.contains("official-unit-key"))
        assertFalse(viewModel.ui.toString().contains(unitCredential))
    }

    private fun record(): HistoryRecord {
        val session = SampleCourses.seriesSession(now)
        val result = SampleCourses.seriesAnalysis(now)
        return HistoryRecord(
            id = "hist_bluelm",
            title = session.title,
            createdAtEpochMs = now,
            providerName = "BLUELM",
            profileLabel = "official_bluelm",
            model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size,
            quizCount = result.quizQuestions.size,
            fallbackUsed = false,
            validationStatus = "PASS",
            session = session,
            result = result,
        )
    }

    private fun vm(record: HistoryRecord, transport: HttpTransport): AppViewModel {
        val dir = Files.createTempDirectory("classmate-bluelm-main-flow")
        val learningStore = InMemoryLearningStore { now }
        learningStore.addTasksFromAnalysis(record.result, record.title, "BLUELM", "official_bluelm", "qwen3.5-plus")
        return AppViewModel(
            configRepository = ConfigRepository(dir.resolve("config.local.json").toFile()),
            modelConfigRepository = ModelConfigRepository(dir.resolve("classmate_model_config.json").toFile()),
            historyStore = InMemoryHistoryStore(listOf(record)),
            learningStore = learningStore,
            exportStore = InMemoryExportStore(),
            transport = transport,
        )
    }

    private fun chatResponse(content: String): String =
        """{"choices":[{"message":{"content":"${escapeJson(content)}"}}]}"""

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
