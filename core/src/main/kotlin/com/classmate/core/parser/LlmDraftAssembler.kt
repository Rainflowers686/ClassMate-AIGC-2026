package com.classmate.core.parser

import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Ids
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.QuestionType
import com.classmate.core.model.QuizAnswerNormalizer
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

data class LlmDraftAssemblyStats(
    val draftKpCount: Int = 0,
    val draftQuizCount: Int = 0,
    val keptKpCount: Int = 0,
    val keptQuizCount: Int = 0,
    val droppedEvidenceCount: Int = 0,
)

data class LlmDraftAssemblyResult(
    val result: CourseAnalysisResult,
    val stats: LlmDraftAssemblyStats,
)

class LlmDraftAssembler(
    private val evidenceResolver: EvidenceResolver = EvidenceResolver(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    fun assemble(
        jsonText: String,
        session: CourseSession,
        provenance: AnalysisProvenance,
    ): LlmDraftAssemblyResult? {
        if (!looksLikeDraft(jsonText)) return null
        val draft = try {
            json.decodeFromString(ClassMateLlmDraftV1.serializer(), jsonText)
        } catch (e: Exception) {
            return null
        }

        var droppedEvidence = 0
        val knowledgePoints = mutableListOf<KnowledgePoint>()
        val titleToId = LinkedHashMap<String, String>()
        val seenTitles = mutableSetOf<String>()

        draft.knowledgePoints.forEach { item ->
            val title = normalizeTitle(item.title)
            if (title == null || !seenTitles.add(title)) return@forEach
            val segment = session.segments.firstOrNull { it.index == item.segmentIndex }
                ?: session.segment(Ids.segment(item.segmentIndex))
            if (segment == null) {
                droppedEvidence++
                return@forEach
            }
            val evidence = evidenceResolver.resolve(segment, item.evidenceQuote)
            if (evidence == null) {
                droppedEvidence++
                return@forEach
            }
            val id = Ids.knowledgePoint(knowledgePoints.size + 1)
            knowledgePoints += KnowledgePoint(
                id = id,
                title = title,
                summary = item.explanation.trim().ifBlank { title },
                sourceSegmentId = segment.id,
                evidence = listOf(evidence),
                importance = parseImportance(item.importance),
                difficulty = parseDifficulty(item.difficulty),
                tags = listOfNotNull(item.conceptType.trim().takeIf { it.isNotBlank() }),
            )
            titleToId[title] = id
        }

        val questions = mutableListOf<QuizQuestion>()
        draft.quizItems.forEach { item ->
            val kpTitle = item.knowledgePointTitle.trim()
            val kpId = titleToId[kpTitle] ?: return@forEach
            val evidence = evidenceResolver.resolveAnywhere(session, item.evidenceQuote)
            if (evidence == null) {
                droppedEvidence++
                return@forEach
            }
            val options = normalizedOptions(item) ?: return@forEach
            val qid = Ids.question(questions.size + 1)
            questions += QuizQuestion(
                id = qid,
                type = parseQuestionType(item.type),
                stem = item.question.trim(),
                options = options,
                testedKnowledgePointIds = listOf(kpId),
                evidence = listOf(evidence),
                explanation = item.explanation.trim().ifBlank { item.question.trim() },
                difficulty = knowledgePoints.firstOrNull { it.id == kpId }?.difficulty ?: Difficulty.MEDIUM,
            )
        }

        return LlmDraftAssemblyResult(
            result = CourseAnalysisResult(
                sessionId = session.id,
                knowledgePoints = knowledgePoints,
                quizQuestions = questions,
                provenance = provenance,
            ),
            stats = LlmDraftAssemblyStats(
                draftKpCount = draft.knowledgePoints.size,
                draftQuizCount = draft.quizItems.size,
                keptKpCount = knowledgePoints.size,
                keptQuizCount = questions.size,
                droppedEvidenceCount = droppedEvidence,
            ),
        )
    }

    private fun looksLikeDraft(jsonText: String): Boolean {
        val root = try {
            json.parseToJsonElement(jsonText) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return false
        if ("quizItems" in root) return true
        val points = root["knowledgePoints"] as? JsonArray ?: return false
        return points.any { element ->
            val point = element as? JsonObject
            point?.containsKey("evidenceQuote") == true
        }
    }

    private fun normalizedOptions(item: LlmDraftQuizItem): List<QuizOption>? {
        if (item.question.isBlank()) return null
        val raw = if (item.type.equals("judge", ignoreCase = true) && item.options.size < 2) {
            listOf("\u6b63\u786e", "\u9519\u8bef")
        } else {
            item.options
        }.map { it.trim() }.filter { it.isNotBlank() }
        if (raw.size < 2) return null
        val base = raw.take(4).mapIndexed { index, text -> QuizOption(id = Ids.option('A' + index), text = text, isCorrect = false) }
        // Prefer an explicit answer string ("\u9519\u8bef"/"B"/"false"); else fall back to correctIndex.
        val resolved = when {
            item.correctAnswer.isNotBlank() -> QuizAnswerNormalizer.withResolvedCorrect(base, item.correctAnswer)
            item.correctIndex in base.indices -> base.mapIndexed { i, o -> if (i == item.correctIndex) o.copy(isCorrect = true) else o }
            else -> base
        }
        if (resolved.none { it.isCorrect }) return null // cannot determine the answer -> never show an unanswerable question
        return resolved.map { if (it.isCorrect) it.copy(rationale = item.explanation.trim()) else it }
    }

    private fun normalizeTitle(value: String): String? {
        val title = value.trim()
        if (title.length < 2) return null
        val lower = title.lowercase()
        if (BAD_TITLE_FRAGMENTS.any { lower.contains(it) }) return null
        return title
    }

    private fun parseImportance(value: String): Importance =
        when (value.trim().lowercase()) {
            "core", "critical", "high" -> Importance.HIGH
            "important", "medium" -> Importance.MEDIUM
            "low" -> Importance.LOW
            else -> Importance.MEDIUM
        }

    private fun parseDifficulty(value: String): Difficulty =
        when (value.trim().lowercase()) {
            "intro", "easy" -> Difficulty.EASY
            "advanced", "hard" -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }

    private fun parseQuestionType(value: String): QuestionType =
        when (value.trim().lowercase()) {
            "judge", "judgment", "true_false", "truefalse" -> QuestionType.JUDGMENT
            "application" -> QuestionType.APPLICATION
            else -> QuestionType.CONCEPT_UNDERSTANDING
        }

    private companion object {
        private val BAD_TITLE_FRAGMENTS = listOf(
            "\u540c\u5b66\u4eec\u597d",
            "\u5927\u5bb6\u597d",
            "\u4eca\u5929\u6211\u4eec\u5b66\u4e60",
            "\u4eca\u5929\u5b66\u4e60",
            "\u9996\u5148",
            "\u6700\u540e",
            "\u672c\u8282\u8bfe",
        )
    }
}
