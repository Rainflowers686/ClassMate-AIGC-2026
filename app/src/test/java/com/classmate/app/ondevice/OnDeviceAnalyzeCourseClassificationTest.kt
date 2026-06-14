package com.classmate.app.ondevice

import com.classmate.core.ondevice.OnDeviceCourseAnalysis
import com.classmate.core.ondevice.OnDeviceGenerationResult
import com.classmate.core.ondevice.OnDeviceLlmDiagnostic
import com.classmate.core.ondevice.OnDeviceLlmProvider
import com.classmate.core.ondevice.OnDeviceLlmStatus
import com.classmate.core.ondevice.OnDeviceLlmTaskProfile
import com.classmate.core.ondevice.OnDeviceModelFileStatus
import com.classmate.core.parser.WireAnalysis
import com.classmate.core.provider.LocalHeuristicExtractor
import com.classmate.core.sample.SampleCourses
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8D-2: analyzeCourse must report the REAL failure. A model that can generate text is never
 * "UNAVAILABLE"; bad output is INVALID_JSON/VALIDATION_FAILED, a timeout is TIMEOUT, an onError is
 * GENERATE_FAILED, and only genuine availability problems map to SDK/PERMISSION/FILES/INIT codes.
 */
class OnDeviceAnalyzeCourseClassificationTest {

    private val session = SampleCourses.seriesSession()
    private val json = Json { encodeDefaults = true }

    private fun validJson(): String =
        json.encodeToString<WireAnalysis>(LocalHeuristicExtractor().extract(session, 8, 1))

    /** Spy provider with a scripted generate result + diagnostic facts. Generate is crash-safe. */
    private class SpyProvider(
        private val generateResult: OnDeviceGenerationResult,
        private val sdkPresent: Boolean = true,
        private val modelDir: String = "/nonexistent/cm-8d2",
        private val available: Boolean = true,
    ) : OnDeviceLlmProvider {
        var generateCalls = 0
        override fun status(): OnDeviceLlmStatus =
            if (available) OnDeviceLlmStatus.AVAILABLE else OnDeviceLlmStatus.SDK_PRESENT
        override fun diagnostic(): OnDeviceLlmDiagnostic = OnDeviceLlmDiagnostic(
            status = status(),
            sdkPresent = sdkPresent,
            modelDir = modelDir,
            initSucceeded = available,
        )
        override fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult {
            generateCalls++
            return generateResult
        }
    }

    private fun readyModelDir(): String {
        val dir = Files.createTempDirectory("cm-8d2-model").toFile()
        File(dir, OnDeviceModelFileStatus.TOKENIZER_FILE).writeText("VOCAB")
        File(dir, OnDeviceModelFileStatus.CONFIG_FILE).writeText("{}")
        return dir.absolutePath
    }

    private fun analyze(provider: SpyProvider, allFiles: Boolean = true) =
        runBlocking { OnDeviceLlmController(provider).analyzeCourse(session, allFiles) }

    @Test
    fun validJsonIsAcceptedWithOnDeviceSource() {
        val spy = SpyProvider(OnDeviceGenerationResult.Success(validJson(), 10, 5))
        val run = analyze(spy)

        assertTrue(run.outcome is OnDeviceCourseAnalysis.Outcome.Accepted)
        assertEquals(1, spy.generateCalls) // analyzeCourse really generates — no Settings probe required
        assertEquals("SUCCESS", run.diagnostic.generateState)
        assertEquals("端侧蓝心", run.diagnostic.finalSource)
        assertTrue(run.diagnostic.textGenerationLastSuccess)
    }

    @Test
    fun naturalLanguageOutputIsInvalidJsonNotUnavailable() {
        val run = analyze(SpyProvider(OnDeviceGenerationResult.Success("我先用自然语言说明一下。", 5, 5)))
        val rejected = run.outcome as OnDeviceCourseAnalysis.Outcome.Rejected

        assertEquals(OnDeviceCourseAnalysis.REASON_INVALID_JSON, rejected.reason)
        assertEquals("SUCCESS", run.diagnostic.generateState) // generate worked; the OUTPUT was bad
        assertEquals("安全占位", run.diagnostic.finalSource)
    }

    @Test
    fun brokenReferenceJsonIsValidationFailedNotUnavailable() {
        val broken = """{"knowledgePoints":[{"title":"级数的定义","summary":"无穷级数的概念","sourceSegmentId":"seg_1","evidenceQuotes":["称为无穷级数"],"importance":"HIGH","difficulty":"EASY","tags":[]}],"quizQuestions":[{"type":"CONCEPT_UNDERSTANDING","stem":"下列说法正确的是？","options":[{"text":"无穷级数是数列各项之和","isCorrect":true,"rationale":"对"},{"text":"无穷级数就是数列本身","isCorrect":false,"rationale":"错"}],"testedKnowledgePoints":["完全不存在的知识点"],"evidenceQuotes":["称为无穷级数"],"explanation":"讲解","difficulty":"EASY"}]}"""
        val run = analyze(SpyProvider(OnDeviceGenerationResult.Success(broken, 10, 5)))

        assertEquals(
            OnDeviceCourseAnalysis.REASON_VALIDATION_FAILED,
            (run.outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason,
        )
    }

    @Test
    fun timeoutIsTimeoutAndErrorIsGenerateFailed() {
        val timeout = analyze(SpyProvider(OnDeviceGenerationResult.Error("TIMEOUT", "端侧生成失败")))
        assertEquals(OnDeviceCourseAnalysis.REASON_TIMEOUT, (timeout.outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason)
        assertEquals("TIMEOUT", timeout.diagnostic.generateState)

        val err = analyze(SpyProvider(OnDeviceGenerationResult.Error("ONDEVICE_42", "端侧生成失败")))
        assertEquals(OnDeviceCourseAnalysis.REASON_GENERATE_FAILED, (err.outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason)
        assertEquals("FAILED", err.diagnostic.generateState)
    }

    @Test
    fun unavailableClassifiesSdkPermissionFilesAndInit() {
        val unavailable = OnDeviceGenerationResult.Unavailable(OnDeviceLlmStatus.INIT_FAILED)

        // No SDK at all.
        assertEquals(
            OnDeviceCourseAnalysis.REASON_SDK_MISSING,
            (analyze(SpyProvider(unavailable, sdkPresent = false, available = false)).outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason,
        )
        // SDK present, model files unreadable, NO all-files access → permission advice.
        assertEquals(
            OnDeviceCourseAnalysis.REASON_PERMISSION_MISSING,
            (analyze(SpyProvider(unavailable, available = false), allFiles = false).outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason,
        )
        // SDK present, permission granted, but files unreadable → file advice.
        assertEquals(
            OnDeviceCourseAnalysis.REASON_MODEL_FILES_MISSING,
            (analyze(SpyProvider(unavailable, available = false), allFiles = true).outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason,
        )
        // SDK + readable files + permission → the remaining cause is a real init failure.
        val initRun = analyze(SpyProvider(unavailable, modelDir = readyModelDir(), available = false), allFiles = true)
        assertEquals(
            OnDeviceCourseAnalysis.REASON_INIT_FAILED,
            (initRun.outcome as OnDeviceCourseAnalysis.Outcome.Rejected).reason,
        )
        assertTrue(initRun.diagnostic.modelFilesReady)
    }

    @Test
    fun diagnosticSafeLinesAreContentFreeAndComplete() {
        val run = analyze(SpyProvider(OnDeviceGenerationResult.Success(validJson(), 10, 5)))
        val blob = run.diagnostic.safeLines().joinToString("\n")

        listOf(
            "sdk_present=", "model_dir=", "all_files_access=", "model_files_ready=",
            "text_generation_last_success=", "course_analysis_attempted=",
            "course_analysis_generate_state=", "course_analysis_reject_reason=",
            "course_analysis_final_source=",
        ).forEach { assertTrue("missing field: $it", blob.contains(it)) }
        listOf("prompt", "appKey", "Authorization", "Bearer", "reasoning").forEach {
            assertTrue("leaked $it", !blob.contains(it, ignoreCase = true))
        }
    }
}
