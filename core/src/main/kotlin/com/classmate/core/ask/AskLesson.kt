package com.classmate.core.ask

import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.parser.JsonExtractor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class LessonQuestion(
    val id: String,
    val question: String,
)

data class LessonAnswerEvidenceRef(
    val quote: String,
    val segmentId: String?,
    val knowledgePointTitle: String?,
)

data class LessonAnswer(
    val answer: String,
    val relatedKnowledgePointTitles: List<String>,
    val evidenceRefs: List<LessonAnswerEvidenceRef>,
    /** One of: grounded / partial / not_found / error. */
    val groundedness: String,
    val followUpSuggestion: String,
    val fallbackUsed: Boolean,
    // Stage 4C grounded-QA metadata. Safe, non-sensitive: provider kind + model label only.
    val providerName: String = "local",
    val modelName: String? = null,
    val confidence: Float? = null,
    val suggestedFollowUps: List<String> = emptyList(),
)

object AskLessonPromptBuilder {
    fun build(session: CourseSession, result: CourseAnalysisResult, question: String): String = buildString {
        appendLine("Return JSON only with answer, relatedKnowledgePoints, evidenceQuotes, groundedness, followUpSuggestion.")
        appendLine("Course title: ${session.title}")
        appendLine("Question: $question")
        appendLine("Knowledge points:")
        result.knowledgePoints.forEach { kp ->
            appendLine("- ${kp.title}: ${kp.summary}")
            kp.evidence.firstOrNull()?.let { appendLine("  evidence: ${it.quote}") }
        }
    }
}

object AskLessonResultParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String, session: CourseSession, result: CourseAnalysisResult): LessonAnswer? {
        val text = JsonExtractor.extract(raw) ?: return null
        val dto = try {
            json.decodeFromString(AskLessonDto.serializer(), text)
        } catch (e: Exception) {
            return null
        }
        val resolver = EvidenceResolver()
        val refs = dto.evidenceQuotes.map { quote ->
            val span = resolver.resolveAnywhere(session, quote)
            LessonAnswerEvidenceRef(
                quote = span?.quote ?: quote,
                segmentId = span?.sourceSegmentId,
                knowledgePointTitle = result.knowledgePoints.firstOrNull { kp -> kp.evidence.any { it.quote == span?.quote } }?.title,
            )
        }
        val allLocated = refs.isNotEmpty() && refs.all { it.segmentId != null }
        return LessonAnswer(
            answer = dto.answer.ifBlank { "No clear answer found in this lesson." },
            relatedKnowledgePointTitles = dto.relatedKnowledgePoints.filter { it.isNotBlank() },
            evidenceRefs = refs.filter { it.segmentId != null },
            groundedness = when {
                dto.groundedness == "not_found" -> "not_found"
                allLocated -> "grounded"
                refs.isNotEmpty() -> "partial"
                else -> "not_found"
            },
            followUpSuggestion = dto.followUpSuggestion,
            fallbackUsed = false,
        )
    }

    @Serializable
    private data class AskLessonDto(
        val answer: String = "",
        val relatedKnowledgePoints: List<String> = emptyList(),
        val evidenceQuotes: List<String> = emptyList(),
        val groundedness: String = "partial",
        val followUpSuggestion: String = "",
    )
}

object LocalAskLessonEngine {
    fun answer(question: String, session: CourseSession, result: CourseAnalysisResult): LessonAnswer {
        val tokens = question.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
        val kp = result.knowledgePoints.firstOrNull { point ->
            tokens.any { token -> point.title.lowercase().contains(token) || point.summary.lowercase().contains(token) }
        } ?: result.knowledgePoints.firstOrNull()
        return if (kp == null) {
            LessonAnswer(
                answer = "No extracted knowledge point is available for this lesson yet.",
                relatedKnowledgePointTitles = emptyList(),
                evidenceRefs = emptyList(),
                groundedness = "not_found",
                followUpSuggestion = "Analyze the lesson first.",
                fallbackUsed = true,
            )
        } else {
            LessonAnswer(
                answer = "Based on the extracted lesson notes: ${kp.summary}",
                relatedKnowledgePointTitles = listOf(kp.title),
                evidenceRefs = kp.evidence.map { LessonAnswerEvidenceRef(it.quote, it.sourceSegmentId, kp.title) },
                groundedness = if (kp.evidence.isNotEmpty()) "grounded" else "partial",
                followUpSuggestion = "Open the evidence card for this knowledge point.",
                fallbackUsed = true,
            )
        }
    }
}
