package com.classmate.core.ondevice

import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.ProviderKind
import com.classmate.core.parser.AnalysisJsonParser
import com.classmate.core.parser.JsonExtractor
import com.classmate.core.parser.LlmDraftAssembler
import com.classmate.core.validation.ResultValidator
import com.classmate.core.validation.ValidationReport

/**
 * Stage 8C/8D-3 — on-device structured CourseAnalysis seam. After the official cloud BlueLM fails,
 * the app asks the on-device BlueLM 3B for a MINIMAL draft JSON and runs it through the EXACT SAME
 * validation pipeline the cloud path uses:
 *
 *     JsonExtractor → LlmDraftAssembler (EvidenceResolver) / AnalysisJsonParser → ResultValidator (EvidenceValidator)
 *
 * Stage 8D-3 stabilisation: the 3B is NOT given the cloud-grade prompt (5-8 knowledge points plus
 * 5-8 quiz items overflowed the on-device token budget and produced truncated / non-JSON output —
 * the INVALID_JSON seen on the real device). Instead it gets a short, hard-constrained, Chinese
 * JSON-only prompt with one tiny example: 3-5 knowledge points, minimal fields, `quizItems: []`.
 * Validators accept zero quizzes but still require every knowledge point to carry verbatim,
 * locatable evidence — nothing here weakens validation or fabricates evidence. A result is
 * [Outcome.Accepted] (persistable) ONLY when validation passes.
 *
 * Provenance: the on-device analysis keeps `provider = BLUELM` (it is BlueLM, the on-device 3B
 * variant) but is marked `fallbackUsed = true` with `modelLabel = `[ON_DEVICE_MODEL_LABEL] so the
 * UI/history can show 端侧蓝心 distinctly from the cloud 云端蓝心.
 */
object OnDeviceCourseAnalysis {

    const val ON_DEVICE_MODEL_LABEL = "端侧蓝心"

    sealed interface Outcome {
        data class Accepted(val result: CourseAnalysisResult, val report: ValidationReport) : Outcome

        /**
         * [reason] is a short, content-free code (see REASON_* below). [jsonDiagnostic] carries the
         * honest, bounded JSON-shape facts for INVALID_JSON / VALIDATION_FAILED display (never the
         * full model output).
         */
        data class Rejected(
            val reason: String,
            val jsonDiagnostic: OnDeviceJsonDiagnostic? = null,
        ) : Outcome
    }

    // Precise, distinct reasons — a generate timeout / failure / bad output must NEVER be reported as a
    // generic "model unavailable" (the Stage 8D-2 bug). Only a genuine availability problem uses the
    // SDK_MISSING / PERMISSION_MISSING / MODEL_FILES_MISSING / INIT_FAILED codes.
    const val REASON_INVALID_JSON = "INVALID_JSON"
    const val REASON_VALIDATION_FAILED = "VALIDATION_FAILED"
    const val REASON_UNAVAILABLE = "UNAVAILABLE"
    const val REASON_SDK_MISSING = "SDK_MISSING"
    const val REASON_PERMISSION_MISSING = "PERMISSION_MISSING"
    const val REASON_MODEL_FILES_MISSING = "MODEL_FILES_MISSING"
    const val REASON_INIT_FAILED = "INIT_FAILED"
    const val REASON_GENERATE_FAILED = "GENERATE_FAILED"
    const val REASON_TIMEOUT = "TIMEOUT"
    const val REASON_UNKNOWN = "UNKNOWN"

    /** Keep the on-device output small enough to never hit the generation token budget. */
    const val MIN_KNOWLEDGE_POINTS = 3
    const val MAX_KNOWLEDGE_POINTS = 5

    /** One tiny valid example — field names exactly match [com.classmate.core.parser.ClassMateLlmDraftV1]. */
    private const val EXAMPLE_JSON =
        """{"courseTitle":"课程名","knowledgePoints":[{"title":"知识点标题","segmentIndex":1,"evidenceQuote":"从段落原文逐字复制的短句","explanation":"一句话解释"}],"quizItems":[]}"""

    /**
     * Stage 8D-3 prompt: short, hard-constrained, Chinese, JSON-only, example-driven. The 3B must
     * output ONE object whose first char is `{` and last is `}` — no markdown, no fences, no chatty
     * prefix. Fewer-but-honest knowledge points are demanded; quizzes are intentionally an empty
     * array (cloud remains the quiz source) so the output always fits the token budget.
     */
    fun buildPrompt(session: CourseSession, maxKnowledgePoints: Int = MAX_KNOWLEDGE_POINTS): String {
        val body = buildString {
            appendLine("只输出一个 JSON 对象：第一个字符必须是 { ，最后一个字符必须是 } 。")
            appendLine("禁止 markdown，禁止```，禁止解释，禁止“好的”“以下是”等任何开场白或结尾语。")
            appendLine("任务：从下面课文中提取 ${MIN_KNOWLEDGE_POINTS}-${maxKnowledgePoints} 个知识点。")
            appendLine("规则：")
            appendLine("1. evidenceQuote 必须从对应段落原文中逐字复制一小句，不得改写、不得编造。")
            appendLine("2. segmentIndex 必须等于该证据所在段落的段号。")
            appendLine("3. 不确定就少写知识点，不要编造内容。")
            appendLine("4. quizItems 固定输出空数组 []。")
            appendLine("输出格式示例（字段名必须完全一致）：")
            appendLine(EXAMPLE_JSON)
            appendLine("课文标题：${session.title.ifBlank { "未命名课程" }}")
            appendLine("课文段落：")
            session.segments.forEach { seg -> appendLine("段号${seg.index}：${seg.text}") }
        }.trim()
        return OnDevicePromptTemplate.format(body)
    }

    /**
     * Validate raw on-device output through the same pipeline. [rawText] must never be logged in full.
     * Returns [Outcome.Accepted] only when [ResultValidator] passes.
     */
    fun process(
        rawText: String,
        session: CourseSession,
        assembler: LlmDraftAssembler = LlmDraftAssembler(),
        parser: AnalysisJsonParser = AnalysisJsonParser(),
        validator: ResultValidator = ResultValidator(),
        clock: () -> Long = System::currentTimeMillis,
    ): Outcome {
        // JsonExtractor already recovers fenced / prose-wrapped / brace-sliced objects; the balanced
        // scan below additionally rescues "{...} + trailing prose containing }" shapes. Neither path
        // invents fields or evidence — the slice must still decode AND pass validators.
        val extracted = JsonExtractor.extractWithStrategy(rawText)?.jsonText
            ?: firstBalancedObject(rawText)
            ?: return Outcome.Rejected(REASON_INVALID_JSON, jsonDiagnostic(rawText, "EXTRACT_FAILED"))

        val provenance = AnalysisProvenance(
            provider = ProviderKind.BLUELM,
            fallbackUsed = true,
            modelLabel = ON_DEVICE_MODEL_LABEL,
            createdAtEpochMs = clock(),
        )

        assembler.assemble(extracted, session, provenance)?.let { assembled ->
            val report = validator.validate(assembled.result, session)
            return if (report.ok) {
                Outcome.Accepted(assembled.result, report)
            } else {
                Outcome.Rejected(
                    REASON_VALIDATION_FAILED,
                    jsonDiagnostic(rawText, "VALIDATION_" + (report.validationErrorType ?: "FAILED")),
                )
            }
        }

        val parsed = parser.parse(extracted, session, provenance)
            ?: return Outcome.Rejected(REASON_INVALID_JSON, jsonDiagnostic(rawText, "DECODE_FAILED"))
        val report = validator.validate(parsed, session)
        return if (report.ok) {
            Outcome.Accepted(parsed, report)
        } else {
            Outcome.Rejected(
                REASON_VALIDATION_FAILED,
                jsonDiagnostic(rawText, "VALIDATION_" + (report.validationErrorType ?: "FAILED")),
            )
        }
    }

    /** True when an analysis result came from the on-device 3B (for honest provenance display). */
    fun isOnDevice(provenance: AnalysisProvenance): Boolean =
        provenance.modelLabel == ON_DEVICE_MODEL_LABEL

    private fun jsonDiagnostic(rawText: String, parseErrorClass: String): OnDeviceJsonDiagnostic =
        OnDeviceJsonDiagnostic(
            jsonStartDetected = rawText.contains('{'),
            jsonEndDetected = rawText.contains('}'),
            markdownFenceDetected = rawText.contains("```"),
            parseErrorClass = parseErrorClass,
            safePreview = OnDeviceLlmDiagnostic.safePreview(rawText),
        )

    /** String-/escape-aware scan for the FIRST balanced {...} object (rescues trailing-prose '}'). */
    private fun firstBalancedObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1).trim()
                    }
                }
            }
        }
        return null
    }
}

/**
 * Stage 8D-3 Task E — honest, bounded JSON-shape facts shown when on-device analysis output is
 * rejected. Carries flags + a ≤80-char preview only, never the full model output.
 */
data class OnDeviceJsonDiagnostic(
    val jsonStartDetected: Boolean,
    val jsonEndDetected: Boolean,
    val markdownFenceDetected: Boolean,
    val parseErrorClass: String,
    val safePreview: String?,
) {
    fun safeLines(): List<String> = buildList {
        add("json_start_detected=$jsonStartDetected")
        add("json_end_detected=$jsonEndDetected")
        add("markdown_fence_detected=$markdownFenceDetected")
        add("parse_error_class=$parseErrorClass")
        safePreview?.let { add("safe_preview=$it") }
    }
}
