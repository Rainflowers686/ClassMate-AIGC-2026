package com.classmate.core.ask

import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.parser.JsonExtractor
import com.classmate.core.prompt.Prompt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Stage 4C — grounded "Ask This Lesson" QA. Answers MUST be bound to the current analysis result's
 * knowledge points and located evidence; when nothing matches, the engine returns NOT_FOUND instead
 * of letting the model invent an answer. The provider is reached through a pluggable [AskChatSeam]
 * so the app can route it through the active profile's resolver order, and so the core stays
 * network-free and unit-testable.
 *
 * Anti-fabrication is enforced in [GroundedAskParser]: a citation quote must come verbatim from the
 * retrieved candidates, and every related knowledge point must be one of the candidate titles.
 */
enum class AskStatus { GROUNDED, PARTIAL, NOT_FOUND, ERROR }

/** One retrieval candidate: a located evidence quote tied to a knowledge point. */
data class AskCandidate(
    val knowledgePointId: String,
    val knowledgePointTitle: String,
    val segmentId: String?,
    val quote: String,
    val score: Int,
)

/** The raw provider reply plus the safe metadata we are allowed to surface (kind + model label). */
data class AskChatResult(val text: String, val providerName: String, val modelName: String?)

/** Pluggable provider call. Returns null when no networked provider can serve (e.g. local_only). */
fun interface AskChatSeam {
    fun chat(prompt: Prompt, repairHint: String?): AskChatResult?
}

/** Safe, enum/count-only telemetry. Deliberately carries NO question, prompt, answer, or body text. */
data class AskTelemetry(
    val askStatus: AskStatus,
    val provider: String,
    val latencyMs: Long,
    val candidateCount: Int,
    val citationCount: Int,
    val parseOk: Boolean,
    val validationOk: Boolean,
    val fallbackUsed: Boolean,
) {
    fun safeLine(): String =
        "ask_status=$askStatus provider=$provider latency_ms=$latencyMs candidates=$candidateCount " +
            "citations=$citationCount parse_ok=$parseOk validation_ok=$validationOk fallback_used=$fallbackUsed"
}

data class AskOutcome(val answer: LessonAnswer, val telemetry: AskTelemetry)

/**
 * Local keyword/title/quote scoring — no vector DB. Works for both Latin words and CJK bigrams so
 * Chinese questions still hit. Only knowledge points with a locatable evidence quote become candidates.
 */
object EvidenceRetriever {
    fun retrieve(question: String, result: CourseAnalysisResult, max: Int = 6): List<AskCandidate> {
        val terms = queryTerms(question)
        if (terms.isEmpty()) return emptyList()
        val candidates = mutableListOf<AskCandidate>()
        for (kp in result.knowledgePoints) {
            val titleHits = countHits(kp.title, terms) * 3
            val summaryHits = countHits(kp.summary, terms)
            for (span in kp.evidence) {
                if (span.sourceSegmentId == null || span.quote.isBlank()) continue
                val quoteHits = countHits(span.quote, terms) * 2
                val score = titleHits + summaryHits + quoteHits
                if (score > 0) {
                    candidates += AskCandidate(kp.id, kp.title, span.sourceSegmentId, span.quote, score)
                }
            }
        }
        return candidates.sortedByDescending { it.score }.take(max)
    }

    private fun queryTerms(q: String): Set<String> {
        val lower = q.lowercase().trim()
        val out = linkedSetOf<String>()
        lower.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }.forEach { out += it }
        val cjk = lower.filter { it.code in 0x3400..0x9FFF }
        for (i in 0 until cjk.length - 1) out += cjk.substring(i, i + 2)
        return out
    }

    private fun countHits(text: String, terms: Set<String>): Int {
        val t = text.lowercase()
        return terms.count { t.contains(it) }
    }
}

/** Builds the provider prompt. Includes ONLY the question, title, candidate titles, candidate quotes
 *  and the strict JSON schema — never the full lecture text, never a credential. */
object AskContextBuilder {
    fun buildPrompt(question: String, courseTitle: String, candidates: List<AskCandidate>, repairHint: String? = null): Prompt {
        val system = buildString {
            appendLine("你是课堂助教。只能依据给定的【候选证据】回答问题，不得编造课外内容。")
            appendLine("必须只输出 JSON，字段：status, answer, relatedKnowledgePoints, citations, confidence。")
            appendLine("status 只能是 grounded / partial / not_found。")
            appendLine("citations[].quote 必须从候选证据原样复制，不能改写或自造。")
            appendLine("relatedKnowledgePoints 必须来自候选知识点标题。")
            appendLine("若候选证据无法支撑回答，返回 status=not_found，不要编造结论。")
            if (repairHint != null) appendLine("修正提示：$repairHint")
        }
        val user = buildString {
            appendLine("课程标题：$courseTitle")
            appendLine("问题：$question")
            appendLine("候选知识点标题：")
            candidates.map { it.knowledgePointTitle }.distinct().forEach { appendLine("- $it") }
            appendLine("候选证据（只能引用这些原句）：")
            candidates.forEach { appendLine("- [${it.knowledgePointTitle}] ${it.quote}") }
            appendLine("""请输出 JSON：{"status":"grounded|partial|not_found","answer":"...","relatedKnowledgePoints":["..."],"citations":[{"knowledgePointTitle":"...","quote":"原样复制候选证据","segmentId":"..."}],"confidence":0.0}""")
        }
        return Prompt(system, user)
    }
}

/** Parses + validates a provider reply against the candidates. Returns null on any anti-fabrication
 *  violation or parse failure, so the engine can repair-retry or fall back instead of trusting it. */
object GroundedAskParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    sealed interface Result {
        data class Ok(val answer: LessonAnswer, val citationCount: Int) : Result
        data class Invalid(val reason: String) : Result
    }

    fun parse(raw: String, candidates: List<AskCandidate>, providerName: String, modelName: String?): Result {
        val text = JsonExtractor.extract(raw) ?: return Result.Invalid("JSON_PARSE_FAILED")
        val dto = try {
            json.decodeFromString(GroundedDto.serializer(), text)
        } catch (e: Exception) {
            return Result.Invalid("JSON_PARSE_FAILED")
        }
        val status = dto.status.trim().lowercase()
        if (status !in setOf("grounded", "partial", "not_found")) return Result.Invalid("BAD_STATUS")

        val candidateTitles = candidates.map { it.knowledgePointTitle }.toSet()
        val relatedClean = dto.relatedKnowledgePoints.map { it.trim() }.filter { it.isNotBlank() }
        // Anti-fabrication: a related KP must be one of the candidate titles.
        if (relatedClean.any { it !in candidateTitles }) return Result.Invalid("RELATED_KP_FABRICATED")

        // Anti-fabrication: every cited quote must come from a candidate quote.
        val refs = mutableListOf<LessonAnswerEvidenceRef>()
        for (c in dto.citations) {
            val q = c.quote.trim()
            if (q.isBlank()) continue
            val cand = candidates.firstOrNull { it.quote == q }
                ?: candidates.firstOrNull { it.quote.contains(q) || q.contains(it.quote) }
                ?: return Result.Invalid("CITATION_FABRICATED")
            refs += LessonAnswerEvidenceRef(quote = cand.quote, segmentId = cand.segmentId, knowledgePointTitle = cand.knowledgePointTitle)
        }

        if (status != "not_found" && dto.answer.isBlank()) return Result.Invalid("EMPTY_ANSWER")

        val finalStatus = when {
            status == "not_found" -> "not_found"
            status == "grounded" && refs.isNotEmpty() -> "grounded"
            else -> "partial"
        }
        val answer = LessonAnswer(
            answer = if (finalStatus == "not_found") dto.answer.ifBlank { NOT_FOUND_TEXT } else dto.answer,
            relatedKnowledgePointTitles = relatedClean,
            evidenceRefs = refs,
            groundedness = finalStatus,
            followUpSuggestion = "",
            fallbackUsed = false,
            providerName = providerName,
            modelName = modelName,
            confidence = dto.confidence?.toFloat(),
        )
        return Result.Ok(answer, refs.size)
    }

    const val NOT_FOUND_TEXT = "本节课资料中没有找到明确依据。"

    @Serializable
    private data class GroundedDto(
        val status: String = "partial",
        val answer: String = "",
        val relatedKnowledgePoints: List<String> = emptyList(),
        val citations: List<CitationDto> = emptyList(),
        val confidence: Double? = null,
    )

    @Serializable
    private data class CitationDto(
        val knowledgePointTitle: String = "",
        val quote: String = "",
        val segmentId: String = "",
    )
}

/** Orchestrates retrieval → provider → validate → (repair) → local fallback. Never fabricates. */
object GroundedAskLessonEngine {
    private const val NOT_FOUND_TEXT = "本节课资料中没有找到明确依据。"

    fun answer(
        question: String,
        session: CourseSession,
        result: CourseAnalysisResult,
        seam: AskChatSeam,
        maxCandidates: Int = 6,
        clock: () -> Long = System::currentTimeMillis,
    ): AskOutcome {
        val start = clock()
        val candidates = EvidenceRetriever.retrieve(question, result, maxCandidates)

        // No candidate evidence → NOT_FOUND, and we DO NOT call the provider.
        if (candidates.isEmpty()) {
            return AskOutcome(
                LessonAnswer(NOT_FOUND_TEXT, emptyList(), emptyList(), "not_found", "", fallbackUsed = false, providerName = "none"),
                AskTelemetry(AskStatus.NOT_FOUND, "none", clock() - start, 0, 0, parseOk = true, validationOk = true, fallbackUsed = false),
            )
        }

        val prompt = AskContextBuilder.buildPrompt(question, session.title, candidates)
        val first = seam.chat(prompt, null)
            ?: return localFallback(candidates, start, clock, provider = "local") // local_only / no network

        when (val parsed = GroundedAskParser.parse(first.text, candidates, first.providerName, first.modelName)) {
            is GroundedAskParser.Result.Ok -> return AskOutcome(parsed.answer, telemetry(parsed.answer, first.providerName, clock() - start, candidates.size, parsed.citationCount, parseOk = true, validationOk = true, fallbackUsed = false))
            is GroundedAskParser.Result.Invalid -> Unit // fall through to a single repair retry
        }

        // One repair retry with a short, non-sensitive corrective hint.
        val repaired = seam.chat(AskContextBuilder.buildPrompt(question, session.title, candidates, repairHint = "上一次输出不符合要求：citations.quote 必须原样来自候选证据，relatedKnowledgePoints 必须来自候选标题。"), "repair")
        if (repaired != null) {
            when (val parsed = GroundedAskParser.parse(repaired.text, candidates, repaired.providerName, repaired.modelName)) {
                is GroundedAskParser.Result.Ok -> return AskOutcome(parsed.answer, telemetry(parsed.answer, repaired.providerName, clock() - start, candidates.size, parsed.citationCount, parseOk = true, validationOk = true, fallbackUsed = false))
                is GroundedAskParser.Result.Invalid -> Unit
            }
        }

        // Provider could not be validated → safe local fallback from candidates (no fabrication).
        return localFallback(candidates, start, clock, provider = "local", afterProviderFailure = true)
    }

    private fun localFallback(
        candidates: List<AskCandidate>,
        start: Long,
        clock: () -> Long,
        provider: String,
        afterProviderFailure: Boolean = false,
    ): AskOutcome {
        val top = candidates.maxByOrNull { it.score }
        val answer = if (top == null) {
            LessonAnswer(NOT_FOUND_TEXT, emptyList(), emptyList(), "not_found", "", fallbackUsed = true, providerName = provider)
        } else {
            LessonAnswer(
                answer = "根据本节课证据：${top.quote}",
                relatedKnowledgePointTitles = candidates.map { it.knowledgePointTitle }.distinct(),
                evidenceRefs = candidates.map { LessonAnswerEvidenceRef(it.quote, it.segmentId, it.knowledgePointTitle) },
                groundedness = "partial",
                followUpSuggestion = "可根据这条证据复习对应知识点。",
                fallbackUsed = true,
                providerName = provider,
            )
        }
        val status = if (answer.groundedness == "not_found") AskStatus.NOT_FOUND else AskStatus.PARTIAL
        return AskOutcome(
            answer,
            AskTelemetry(status, provider, clock() - start, candidates.size, answer.evidenceRefs.size, parseOk = !afterProviderFailure, validationOk = !afterProviderFailure, fallbackUsed = true),
        )
    }

    private fun telemetry(answer: LessonAnswer, provider: String, latency: Long, candidateCount: Int, citationCount: Int, parseOk: Boolean, validationOk: Boolean, fallbackUsed: Boolean) =
        AskTelemetry(
            askStatus = when (answer.groundedness) {
                "grounded" -> AskStatus.GROUNDED
                "not_found" -> AskStatus.NOT_FOUND
                "error" -> AskStatus.ERROR
                else -> AskStatus.PARTIAL
            },
            provider = provider, latencyMs = latency, candidateCount = candidateCount, citationCount = citationCount,
            parseOk = parseOk, validationOk = validationOk, fallbackUsed = fallbackUsed,
        )
}
