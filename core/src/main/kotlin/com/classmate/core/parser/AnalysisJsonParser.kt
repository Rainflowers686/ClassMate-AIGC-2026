package com.classmate.core.parser

import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.QuizAnswerNormalizer
import com.classmate.core.model.CourseSession
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Ids
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.QuestionType
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import kotlinx.serialization.json.Json

/**
 * Maps the model's [WireAnalysis] JSON onto validated domain objects:
 *  - assigns stable ids (the model never sees them),
 *  - resolves every evidence quote to a real [com.classmate.core.model.EvidenceSpan]
 *    (unresolvable quotes are dropped, which later fails validation),
 *  - resolves question -> knowledge-point references by title into ids (reference closure).
 *
 * Returns null only when the JSON itself can't be deserialised; structural problems are left
 * for the validator to report so the failure reason is precise.
 */
class AnalysisJsonParser(
    private val evidenceResolver: EvidenceResolver = EvidenceResolver(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    fun parse(
        jsonText: String,
        session: CourseSession,
        provenance: AnalysisProvenance,
    ): CourseAnalysisResult? {
        val wire = try {
            json.decodeFromString(WireAnalysis.serializer(), jsonText)
        } catch (e: Exception) {
            return null
        }

        val knowledgePoints = mutableListOf<KnowledgePoint>()
        val titleToId = LinkedHashMap<String, String>()
        wire.knowledgePoints.forEachIndexed { index, w ->
            if (w.title.isBlank()) return@forEachIndexed
            val id = Ids.knowledgePoint(index + 1)
            val segment = session.segment(w.sourceSegmentId)
            val spans = if (segment == null) {
                emptyList()
            } else {
                w.evidenceQuotes.mapNotNull { evidenceResolver.resolve(segment, it) }
            }
            knowledgePoints += KnowledgePoint(
                id = id,
                title = w.title.trim(),
                summary = w.summary.trim(),
                sourceSegmentId = w.sourceSegmentId,
                evidence = spans,
                importance = parseImportance(w.importance),
                difficulty = parseDifficulty(w.difficulty),
                tags = w.tags,
            )
            titleToId[w.title.trim()] = id
        }

        val questions = mutableListOf<QuizQuestion>()
        wire.quizQuestions.forEachIndexed { index, w ->
            if (w.stem.isBlank() || w.options.isEmpty()) return@forEachIndexed
            val qid = Ids.question(index + 1)
            val rawOptions = w.options.mapIndexed { oi, o ->
                QuizOption(
                    id = Ids.option('A' + oi),
                    text = o.text.trim(),
                    isCorrect = o.isCorrect,
                    rationale = o.rationale.trim(),
                )
            }
            // Recover the correct option from a separate answer field when no per-option flag was set
            // (root fix for true/false where the model returns e.g. {"answer":"错误"}).
            val options = QuizAnswerNormalizer.withResolvedCorrect(rawOptions, w.correctAnswer)
            val testedIds = w.testedKnowledgePoints.mapNotNull { ref ->
                val r = ref.trim()
                titleToId[r] ?: r.takeIf { titleToId.containsValue(it) }
            }.distinct()
            val spans = w.evidenceQuotes.mapNotNull { evidenceResolver.resolveAnywhere(session, it) }
            questions += QuizQuestion(
                id = qid,
                type = QuestionType.fromWire(w.type),
                stem = w.stem.trim(),
                options = options,
                testedKnowledgePointIds = testedIds,
                evidence = spans,
                explanation = w.explanation.trim(),
                difficulty = parseDifficulty(w.difficulty),
            )
        }

        return CourseAnalysisResult(
            sessionId = session.id,
            knowledgePoints = knowledgePoints,
            quizQuestions = questions,
            provenance = provenance,
        )
    }

    private fun parseImportance(value: String): Importance = when (value.trim().uppercase()) {
        "LOW" -> Importance.LOW
        "HIGH" -> Importance.HIGH
        "CRITICAL" -> Importance.CRITICAL
        else -> Importance.MEDIUM
    }

    private fun parseDifficulty(value: String): Difficulty = when (value.trim().uppercase()) {
        "EASY" -> Difficulty.EASY
        "HARD" -> Difficulty.HARD
        else -> Difficulty.MEDIUM
    }
}
