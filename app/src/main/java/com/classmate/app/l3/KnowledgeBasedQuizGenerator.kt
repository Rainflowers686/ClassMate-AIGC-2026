package com.classmate.app.l3

import com.classmate.core.model.Difficulty

/**
 * Builds local fallback quiz questions from accepted subject knowledge cards, not raw OCR prose.
 *
 * The generator keeps evidence as grounding/explanation, but avoids turning the evidence sentence into
 * the question stem or a fixed A option. Correct letters are distributed deterministically so a small
 * local fallback session does not become "all true/false, all A".
 */
object KnowledgeBasedQuizGenerator {
    private val correctLetters = listOf("A", "C", "D", "B")

    fun generate(
        lessonId: String,
        knowledge: List<L3KnowledgePoint>,
        evidence: List<Evidence>,
        now: Long,
        maxQuestions: Int = 5,
        idPrefix: String = "q_local",
    ): List<L3GeneratedQuestion> {
        val evidenceById = evidence.associateBy { it.id }
        return knowledge
            .filter { it.sourceEvidenceIds.any { id -> evidenceById[id]?.text?.isNotBlank() == true } }
            .filter { it.title.isNotBlank() && !SubjectKnowledgeExtractor.isNoiseLine(it.title) }
            .take(maxQuestions.coerceAtLeast(1))
            .mapIndexedNotNull { index, kp ->
                val evidenceId = kp.sourceEvidenceIds.firstOrNull { id -> evidenceById[id]?.text?.isNotBlank() == true }
                    ?: return@mapIndexedNotNull null
                val quote = subjectQuote(evidenceById[evidenceId]?.text.orEmpty(), kp)
                val related = knowledge
                    .filter { it.id != kp.id && !SubjectKnowledgeExtractor.isNoiseLine(it.title) }
                    .map { it.title }
                buildQuestion(
                    id = "${idPrefix}_${now}_${index + 1}",
                    lessonId = lessonId,
                    kp = kp,
                    evidenceId = evidenceId,
                    evidenceQuote = quote,
                    relatedTitles = related,
                    index = index,
                )
            }
    }

    fun buildQuestion(
        id: String,
        lessonId: String,
        kp: L3KnowledgePoint,
        evidenceId: String,
        evidenceQuote: String,
        relatedTitles: List<String>,
        index: Int,
    ): L3GeneratedQuestion {
        val title = kp.title.trim().ifBlank { "本课知识点" }
        val quote = subjectQuote(evidenceQuote, kp)
            .ifBlank { kp.explanation.ifBlank { title } }
            .take(160)
        return when {
            index % 5 == 2 -> trueFalseQuestion(id, lessonId, kp, evidenceId, quote, index)
            else -> singleChoiceQuestion(id, lessonId, kp, evidenceId, quote, relatedTitles, index)
        }
    }

    private fun singleChoiceQuestion(
        id: String,
        lessonId: String,
        kp: L3KnowledgePoint,
        evidenceId: String,
        evidenceQuote: String,
        relatedTitles: List<String>,
        index: Int,
    ): L3GeneratedQuestion {
        val title = kp.title.trim().ifBlank { "本课知识点" }
        val correct = correctConcept(title, evidenceQuote)
        val distractors = listOf(
            relatedTitles.getOrNull(0)?.let { "把「$it」的适用范围误当成「$title」的结论" }
                ?: "只背课堂原句，不能解释「$title」的关键关系",
            relatedTitles.getOrNull(1)?.let { "混淆「$it」与「$title」的证据依据" }
                ?: "忽略题干限定，直接选择未被证据支持的说法",
            "只依据 OCR 字面顺序作答，没有回到知识点和证据核对",
        )
        val correctLetter = correctLetters[index % correctLetters.size]
        val optionsByLetter = placeCorrectOption(correctLetter, correct, distractors)
        return L3GeneratedQuestion(
            id = id,
            lessonId = lessonId,
            knowledgePointId = kp.id,
            stem = "围绕「$title」的理解，下列哪一项最符合本课知识点？",
            options = optionsByLetter.map { (letter, text) -> "$letter. $text" },
            correctAnswer = correctLetter,
            explanation = buildString {
                append("答案详解：$correctLetter 正确。")
                append("本题考查「$title」，正确项概括的是知识点关系，不是照搬 OCR 原句。")
                append("其他选项分别混淆同课概念、忽略题干限定或只背原文，因此不符合本课证据。")
                append("对应知识点：$title。证据摘录：$evidenceQuote")
            },
            evidenceIds = listOf(evidenceId),
            difficulty = Difficulty.MEDIUM,
        )
    }

    private fun trueFalseQuestion(
        id: String,
        lessonId: String,
        kp: L3KnowledgePoint,
        evidenceId: String,
        evidenceQuote: String,
        index: Int,
    ): L3GeneratedQuestion {
        val title = kp.title.trim().ifBlank { "本课知识点" }
        val makeFalse = index % 2 == 0
        val statement = if (makeFalse) {
            "学习「$title」时，只需要记住 OCR 原文出现过这句话，不需要理解证据中的关系。"
        } else {
            "学习「$title」时，需要把概念关系和课程证据对应起来，而不是只背关键词。"
        }
        val correct = if (makeFalse) "B" else "A"
        return L3GeneratedQuestion(
            id = id,
            lessonId = lessonId,
            knowledgePointId = kp.id,
            stem = "判断题：关于「$title」，下面说法是否正确？$statement",
            options = listOf("A. 正确", "B. 错误"),
            correctAnswer = correct,
            explanation = buildString {
                append("答案详解：$correct 正确。")
                append(if (makeFalse) "该说法错误，因为微测要考查知识点理解和证据关系，不能只复制 OCR 原文。" else "该说法正确，因为本题要求把知识点和证据对应起来。")
                append("对应知识点：$title。证据摘录：$evidenceQuote")
            },
            evidenceIds = listOf(evidenceId),
            difficulty = Difficulty.EASY,
        )
    }

    private fun placeCorrectOption(
        correctLetter: String,
        correct: String,
        distractors: List<String>,
    ): List<Pair<String, String>> {
        val letters = listOf("A", "B", "C", "D")
        var distractorIndex = 0
        return letters.map { letter ->
            if (letter == correctLetter) {
                letter to correct
            } else {
                letter to distractors.getOrElse(distractorIndex++) { "没有回到课程证据核对知识点" }
            }
        }
    }

    private fun correctConcept(title: String, evidenceQuote: String): String {
        val clean = SubjectKnowledgeExtractor.cleanSubjectTextForDisplay(evidenceQuote)
        val relation = clean
            .split(Regex("[。；;.!?！？\\n]"))
            .map { it.trim() }
            .firstOrNull { it.contains(title.take(2)) || SubjectKnowledgeExtractor.subjectScore(it) > 0 }
            ?.take(72)
        return if (relation.isNullOrBlank()) {
            "能用课程证据解释「$title」的概念关系和适用条件"
        } else {
            "能说明「$title」中的关键关系：$relation"
        }
    }

    private fun subjectQuote(raw: String, kp: L3KnowledgePoint): String {
        val candidates = raw
            .split(Regex("[。；;.!?！？\\n]"))
            .map { SubjectKnowledgeExtractor.cleanSubjectTextForDisplay(it) }
            .filter { it.isNotBlank() }
        return candidates.firstOrNull { it.contains(kp.title.take(2)) && SubjectKnowledgeExtractor.subjectScore(it) > 0 }
            ?: candidates.firstOrNull { SubjectKnowledgeExtractor.subjectScore(it) > 0 }
            ?: raw.trim().take(140)
    }
}
