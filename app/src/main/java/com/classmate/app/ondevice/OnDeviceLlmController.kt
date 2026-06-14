package com.classmate.app.ondevice

import com.classmate.core.ask.AskChatSeam
import com.classmate.core.model.CourseSession
import com.classmate.core.ondevice.LocalProviderChain
import com.classmate.core.ondevice.OnDeviceAnalysisDiagnostic
import com.classmate.core.ondevice.OnDeviceAnalysisRun
import com.classmate.core.ondevice.ModelPathDetection
import com.classmate.core.ondevice.OnDeviceCourseAnalysis
import com.classmate.core.ondevice.OnDeviceModelFileProbe
import com.classmate.core.ondevice.OnDeviceModelPathDetector
import com.classmate.core.ondevice.MissingOnDeviceBlueLmBridge
import com.classmate.core.ondevice.OnDeviceAskChatSeam
import com.classmate.core.ondevice.OnDeviceGenerationResult
import com.classmate.core.ondevice.OnDeviceLlmDiagnostic
import com.classmate.core.ondevice.OnDeviceLlmProvider
import com.classmate.core.ondevice.OnDeviceLlmStatus
import com.classmate.core.ondevice.OnDeviceErrorExplain
import com.classmate.core.ondevice.OnDeviceLlmTaskProfile
import com.classmate.core.ondevice.OnDeviceImageDraftResult
import com.classmate.core.ondevice.OnDeviceMultimodalDiagnostic
import com.classmate.core.ondevice.OnDeviceMultimodalProbe
import com.classmate.core.ondevice.OnDeviceProbeState
import com.classmate.core.ondevice.RgbImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App-side owner of the on-device BlueLM 3B model. It keeps the SDK lifecycle off the UI thread and
 * exposes only safe, content-free state (diagnostic / status) plus the Ask fallback seam.
 *
 * In production the composition root injects [RealVivoOnDeviceLlmBridge], which reflects onto the
 * vivo SDK when `app/libs/llm-sdk-release.aar` is bundled and otherwise reports SDK_MISSING (so CI /
 * AAR-less machines behave exactly like the honest seam). Unit tests inject
 * [MissingOnDeviceBlueLmBridge] or a fake.
 */
class OnDeviceLlmController(
    private val provider: OnDeviceLlmProvider = MissingOnDeviceBlueLmBridge(),
) {
    private val chain = LocalProviderChain(provider)

    fun status(): OnDeviceLlmStatus = provider.status()

    fun isAvailable(): Boolean = provider.isAvailable()

    /** Lightweight, content-free readiness snapshot (no init/generate) for the initial UI state. */
    fun diagnostic(): OnDeviceLlmDiagnostic = provider.diagnostic()

    /** Internal short local-chain path labels, e.g. [OnDeviceBlueLM, SafetyPlaceholder]. */
    fun localPath(): List<String> = chain.path()

    /** User-facing Chinese local-chain path labels, e.g. [端侧蓝心, 安全占位] or [安全占位]. */
    fun localPathZh(): List<String> = chain.pathZh()

    /** The Ask-This-Lesson fallback seam (returns null when the model is unavailable). */
    fun askSeam(): AskChatSeam = OnDeviceAskChatSeam(provider)

    /** Fixed Settings self-test question for the pure-text probe (P3). */
    fun textProbeQuestion(): String = TEXT_PROBE_QUESTION

    fun multimodalProbeQuestion(): String = MULTIMODAL_PROBE_QUESTION

    // --- functional-permission gating (Stage 8A-2.2) ---

    /** True when the SDK is actually loaded (real native present), i.e. gating must be enforced. */
    fun sdkPresent(): Boolean = provider.diagnostic().sdkPresent

    /** True only after a successful text init (status AVAILABLE). Gate for the multimodal probe. */
    fun lastTextInitSucceeded(): Boolean = provider.isAvailable()

    /** A real native text init is only allowed once all-files access is granted (Task 5). */
    fun isTextProbeAllowed(allFilesGranted: Boolean): Boolean = !sdkPresent() || allFilesGranted

    /**
     * The multimodal native path (init+callVit) is the P0 crash source. It is allowed ONLY when the
     * SDK is present, all-files access is granted, AND a text init has already succeeded (Task 6).
     */
    fun isMultimodalProbeAllowed(allFilesGranted: Boolean): Boolean =
        sdkPresent() && allFilesGranted && lastTextInitSucceeded()

    /** Synchronous text probe with gating (testable without coroutines). */
    fun runTextProbeBlocking(allFilesGranted: Boolean): OnDeviceLlmDiagnostic =
        if (!isTextProbeAllowed(allFilesGranted)) {
            provider.diagnostic().copy(errorCode = OnDeviceErrorExplain.ALL_FILES_ACCESS_REQUIRED)
        } else {
            provider.runTextProbe(TEXT_PROBE_QUESTION)
        }

    /**
     * Synchronous multimodal probe with gating. When blocked it returns a diagnostic WITHOUT ever
     * calling native init/callVit — this is the fix that prevents the multimodal crash.
     */
    fun runMultimodalBlocking(allFilesGranted: Boolean): OnDeviceMultimodalDiagnostic {
        if (!isMultimodalProbeAllowed(allFilesGranted)) {
            val reason = if (!allFilesGranted) {
                OnDeviceErrorExplain.ALL_FILES_ACCESS_REQUIRED
            } else {
                OnDeviceErrorExplain.TEXT_INIT_REQUIRED
            }
            return blockedMultimodal(reason)
        }
        val probe = provider as? OnDeviceMultimodalProbe ?: return unavailableMultimodal()
        return probe.probeMultimodal(BitmapToRgb.diagnosticTestImage(), MULTIMODAL_PROBE_QUESTION)
    }

    /** Real text self-test on a background thread: init + one fixed-question generation (gated). */
    suspend fun runTextDiagnostic(allFilesGranted: Boolean): OnDeviceLlmDiagnostic =
        withContext(Dispatchers.IO) { runTextProbeBlocking(allFilesGranted) }

    /** Experimental multimodal VIT diagnostic on a background thread (P4, gated). Never the course pipeline. */
    suspend fun runMultimodalDiagnostic(allFilesGranted: Boolean): OnDeviceMultimodalDiagnostic =
        withContext(Dispatchers.IO) { runMultimodalBlocking(allFilesGranted) }

    /**
     * Stage 8E: REAL-image multimodal diagnostic (user-picked photo instead of the 2x2 self-check).
     * Same crash gate as the built-in probe; runs callVit + generate over the supplied image and
     * returns the honest diagnostic. Never persists anything; never logs the image or full output.
     */
    suspend fun runRealImageDiagnostic(image: RgbImage, allFilesGranted: Boolean): OnDeviceMultimodalDiagnostic =
        withContext(Dispatchers.IO) {
            if (!isMultimodalProbeAllowed(allFilesGranted)) {
                runMultimodalBlocking(allFilesGranted) // returns the blocked diagnostic without native calls
            } else {
                val probe = provider as? OnDeviceMultimodalProbe
                if (probe == null) runMultimodalBlocking(allFilesGranted)
                else probe.probeMultimodal(image, MULTIMODAL_PROBE_QUESTION)
            }
        }

    /**
     * Stage 8E Phase 1: bounded model-directory detection — user path + four FIXED official
     * candidates, first-level file names only. Used by Settings to recommend switching to the
     * versioned official directory (e.g. /sdcard/1225/1.7.0.4_1225_mtk9500).
     */
    suspend fun detectModelPath(): ModelPathDetection = withContext(Dispatchers.IO) {
        OnDeviceModelPathDetector.detect(provider.diagnostic().modelDir)
    }

    /** Change the model directory (P5) and return the refreshed snapshot. */
    suspend fun updateModelPath(path: String): OnDeviceLlmDiagnostic = withContext(Dispatchers.IO) {
        provider.updateModelPath(path)
        provider.diagnostic()
    }

    /** Background-thread generation. Blocking SDK call is confined to [Dispatchers.IO]. */
    suspend fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult =
        withContext(Dispatchers.IO) { provider.generate(profile, prompt) }

    /**
     * Stage 8C/8D-2: ask the on-device BlueLM 3B for a structured CourseAnalysis and validate it through
     * the SAME validators as the cloud path. It independently inits/generates (no Settings text probe
     * required) and is crash-safe (init failures are caught). Returns [OnDeviceAnalysisRun] with the
     * outcome AND an honest diagnostic.
     *
     * CRITICAL (8D-2 fix): a generate **timeout/error** or **bad output** is NEVER reported as
     * "model unavailable". Only a real availability problem yields SDK_MISSING / PERMISSION_MISSING /
     * MODEL_FILES_MISSING / INIT_FAILED; a successful generate that fails parsing/validation yields
     * INVALID_JSON / VALIDATION_FAILED; a timeout/onError yields TIMEOUT / GENERATE_FAILED.
     */
    suspend fun analyzeCourse(session: CourseSession, allFilesGranted: Boolean): OnDeviceAnalysisRun =
        withContext(Dispatchers.IO) {
            val prompt = OnDeviceCourseAnalysis.buildPrompt(session)
            val generateResult = provider.generate(OnDeviceLlmTaskProfile.ANALYSIS, prompt)
            val outcome: OnDeviceCourseAnalysis.Outcome = when (generateResult) {
                is OnDeviceGenerationResult.Success ->
                    // Generate worked → the only failures left are bad JSON or failed validation.
                    OnDeviceCourseAnalysis.process(generateResult.text, session)
                is OnDeviceGenerationResult.Unavailable ->
                    OnDeviceCourseAnalysis.Outcome.Rejected(classifyUnavailable(allFilesGranted))
                is OnDeviceGenerationResult.Error ->
                    OnDeviceCourseAnalysis.Outcome.Rejected(
                        if (generateResult.code.contains("TIMEOUT", ignoreCase = true)) OnDeviceCourseAnalysis.REASON_TIMEOUT
                        else OnDeviceCourseAnalysis.REASON_GENERATE_FAILED,
                    )
            }
            OnDeviceAnalysisRun(outcome, analysisDiagnostic(allFilesGranted, generateResult, outcome))
        }

    /** Classify a genuine generate-Unavailable into a precise reason (never a blanket "unavailable"). */
    private fun classifyUnavailable(allFilesGranted: Boolean): String {
        val diag = provider.diagnostic()
        val files = OnDeviceModelFileProbe.probe(diag.modelDir)
        return when {
            !diag.sdkPresent -> OnDeviceCourseAnalysis.REASON_SDK_MISSING
            files.tokenizerReadable && files.configReadable -> OnDeviceCourseAnalysis.REASON_INIT_FAILED
            !allFilesGranted -> OnDeviceCourseAnalysis.REASON_PERMISSION_MISSING
            else -> OnDeviceCourseAnalysis.REASON_MODEL_FILES_MISSING
        }
    }

    private fun analysisDiagnostic(
        allFilesGranted: Boolean,
        generateResult: OnDeviceGenerationResult,
        outcome: OnDeviceCourseAnalysis.Outcome,
    ): OnDeviceAnalysisDiagnostic {
        val diag = provider.diagnostic()
        val files = OnDeviceModelFileProbe.probe(diag.modelDir)
        val generateState = when (generateResult) {
            is OnDeviceGenerationResult.Success -> "SUCCESS"
            is OnDeviceGenerationResult.Error ->
                if (generateResult.code.contains("TIMEOUT", ignoreCase = true)) "TIMEOUT" else "FAILED"
            is OnDeviceGenerationResult.Unavailable -> "NOT_RUN"
        }
        val rejected = outcome as? OnDeviceCourseAnalysis.Outcome.Rejected
        val finalSource = if (outcome is OnDeviceCourseAnalysis.Outcome.Accepted) "端侧蓝心" else "安全占位"
        return OnDeviceAnalysisDiagnostic(
            sdkPresent = diag.sdkPresent,
            modelDir = diag.modelDir,
            allFilesAccess = allFilesGranted,
            modelFilesReady = files.modelDirExists && files.tokenizerReadable && files.configReadable,
            textGenerationLastSuccess = diag.initSucceeded || provider.isAvailable(),
            courseAnalysisAttempted = true,
            generateState = generateState,
            rejectReason = rejected?.reason,
            finalSource = finalSource,
            // Stage 8D-3: honest JSON-shape facts (brace/fence flags, parse class, ≤80-char preview).
            extraLines = rejected?.jsonDiagnostic?.safeLines().orEmpty(),
        )
    }

    /**
     * Stage 8C Phase C: turn an image into an editable learning-text DRAFT via on-device multimodal
     * understanding. Reuses the multimodal crash guard ([isMultimodalProbeAllowed]) so native
     * init/callVit is never touched without all-files access + a prior successful init. When blocked or
     * the model fails, returns [OnDeviceImageDraftResult.Unavailable] so the UI shows a manual-input box.
     * This produces a DRAFT only — it never writes to the knowledge base and is not OCR.
     */
    suspend fun describeImageToDraft(image: RgbImage, allFilesGranted: Boolean): OnDeviceImageDraftResult =
        withContext(Dispatchers.IO) {
            if (!isMultimodalProbeAllowed(allFilesGranted)) {
                OnDeviceImageDraftResult.Unavailable(
                    if (!allFilesGranted) OnDeviceErrorExplain.ALL_FILES_ACCESS_REQUIRED
                    else OnDeviceErrorExplain.TEXT_INIT_REQUIRED,
                )
            } else {
                val probe = provider as? OnDeviceMultimodalProbe
                if (probe == null) {
                    OnDeviceImageDraftResult.Unavailable("MULTIMODAL_UNAVAILABLE")
                } else {
                    when (val r = probe.describeImage(image, IMAGE_PROBE_QUESTION)) {
                        is OnDeviceGenerationResult.Success -> OnDeviceImageDraftResult.Draft(r.text)
                        else -> OnDeviceImageDraftResult.Unavailable("MULTIMODAL_UNAVAILABLE")
                    }
                }
            }
        }

    fun release() = provider.release()

    private fun unavailableMultimodal(): OnDeviceMultimodalDiagnostic {
        val img = BitmapToRgb.diagnosticTestImage()
        return OnDeviceMultimodalDiagnostic(
            state = OnDeviceProbeState.MULTIMODAL_UNAVAILABLE,
            sdkSupportsMultimodalField = false,
            callVitMethodPresent = false,
            modelDir = provider.diagnostic().modelDir,
            testImageWidth = img.width,
            testImageHeight = img.height,
            rgbByteLength = img.bytes.size,
        )
    }

    /** Gate-blocked multimodal result. Crucially: NO native init/callVit was called. */
    private fun blockedMultimodal(reason: String): OnDeviceMultimodalDiagnostic {
        val img = BitmapToRgb.diagnosticTestImage()
        val diag = provider.diagnostic()
        return OnDeviceMultimodalDiagnostic(
            state = OnDeviceProbeState.MULTIMODAL_UNAVAILABLE,
            sdkSupportsMultimodalField = (provider as? OnDeviceMultimodalProbe)?.supportsMultimodal() == true,
            callVitMethodPresent = (provider as? OnDeviceMultimodalProbe)?.supportsMultimodal() == true,
            modelDir = diag.modelDir,
            testImageWidth = img.width,
            testImageHeight = img.height,
            rgbByteLength = img.bytes.size,
            errorCode = reason,
        )
    }

    companion object {
        const val TEXT_PROBE_QUESTION = "用一句话解释什么是学习复习计划。"
        const val MULTIMODAL_PROBE_QUESTION = "请用一句话描述这张图片。"
        const val IMAGE_PROBE_QUESTION =
            "请把这张图片中的学习内容整理为可编辑的学习资料草稿：包含可见文字、公式、表格、题目或板书要点；" +
                "不确定的内容请标注“不确定”，不要编造；只输出草稿文本本身。"

        /**
         * Production factory: real reflection bridge (honest SDK_MISSING when the AAR is absent).
         * Stage 8D-2: generation timeout raised to fit structured CourseAnalysis output (~1k tokens on
         * the NPU) — the one-line Settings probe fit in 30s but full analysis JSON did not, and that
         * timeout used to surface as a false "UNAVAILABLE". A real timeout now reports TIMEOUT.
         */
        fun real(): OnDeviceLlmController =
            OnDeviceLlmController(RealVivoOnDeviceLlmBridge(generateTimeoutMs = ANALYSIS_TIMEOUT_MS))

        const val ANALYSIS_TIMEOUT_MS = 120_000L
    }
}
