package com.classmate.core.ai

import com.classmate.core.model.QuizAnswerNormalizer
import com.classmate.core.model.QuizOption
import com.classmate.core.parser.JsonExtractor
import com.classmate.core.practice.PracticeDifficulty
import com.classmate.core.practice.PracticeItem
import com.classmate.core.practice.PracticeItemType
import com.classmate.core.practice.PracticeOption
import com.classmate.core.practice.isAnswerableQuiz
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * P0-4: turns a BlueLM / on-device model's variant-quiz JSON into real, answerable [PracticeItem]s.
 *
 * The model is asked for strict JSON; this recovers the object via [JsonExtractor] (tolerates ```json
 * fences / stray prose), normalizes the answer through [QuizAnswerNormalizer] (A/B/C/D, option text,
 * true/false, 正确/错误, index), and keeps ONLY questions that pass [isAnswerableQuiz] (>=2 options AND a
 * resolved correct answer). Malformed JSON or no usable question → empty list (caller shows an error /
 * empty state). Every item is bound to the supplied knowledge point so results flow back into the loop.
 */
object VariantQuizParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class VariantWire(val questions: List<VariantQuestionWire> = emptyList())

    @Serializable
    private data class VariantQuestionWire(
        val stem: String = "",
        val type: String = "single_choice",
        val options: List<VariantOptionWire> = emptyList(),
        val answer: String = "",
        val explanation: String = "",
        @SerialName("knowledgePointTitle") val knowledgePointTitle: String = "",
        val difficulty: String = "normal",
        @SerialName("whyThisVariant") val whyThisVariant: String = "",
    )

    @Serializable
    private data class VariantOptionWire(val id: String = "", val text: String = "")

    fun parse(
        rawModelText: String,
        source: AiExecutionSource,
        idPrefix: String,
        knowledgePointIdFor: (String) -> String,
    ): List<PracticeItem> {
        val jsonText = JsonExtractor.extract(rawModelText) ?: return emptyList()
        val wire = runCatching { json.decodeFromString(VariantWire.serializer(), jsonText) }.getOrNull() ?: return emptyList()
        return wire.questions.mapIndexedNotNull { index, q ->
            if (q.stem.isBlank() || q.options.size < 2) return@mapIndexedNotNull null
            val options = q.options.mapIndexed { i, o ->
                QuizOption(id = o.id.trim().ifBlank { ('A' + i).toString() }, text = o.text.trim(), isCorrect = false)
            }.filter { it.text.isNotBlank() }
            if (options.size < 2) return@mapIndexedNotNull null
            val correctIds = QuizAnswerNormalizer.resolveCorrectIds(options, q.answer).toSet()
            if (correctIds.isEmpty()) return@mapIndexedNotNull null
            val practiceOptions = options.map { PracticeOption(it.id, it.text, it.id in correctIds) }
            val correctText = practiceOptions.firstOrNull { it.correct }?.text.orEmpty()
            val item = PracticeItem(
                id = "${idPrefix}_$index",
                type = PracticeItemType.QUIZ_RETRY,
                knowledgePointId = knowledgePointIdFor(q.knowledgePointTitle),
                knowledgePointTitle = q.knowledgePointTitle,
                question = q.stem.trim(),
                answer = q.explanation.trim().ifBlank { "正确答案：$correctText" },
                options = practiceOptions,
                difficulty = difficultyOf(q.difficulty),
                whyThisQuestionMatters = q.whyThisVariant.trim(),
                source = source,
            )
            item.takeIf { it.isAnswerableQuiz() }
        }
    }

    private fun difficultyOf(value: String): PracticeDifficulty = when (value.trim().lowercase()) {
        "basic", "easy" -> PracticeDifficulty.EASY
        "advanced", "hard" -> PracticeDifficulty.HARD
        else -> PracticeDifficulty.MEDIUM
    }
}
