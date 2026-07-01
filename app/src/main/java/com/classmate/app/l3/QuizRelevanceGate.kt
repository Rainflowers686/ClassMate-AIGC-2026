package com.classmate.app.l3

object QuizRelevanceGate {
    fun filter(
        questions: List<L3GeneratedQuestion>,
        knowledge: List<L3KnowledgePoint>,
        evidence: List<Evidence>,
    ): List<L3GeneratedQuestion> =
        questions.filter { isRelevant(it, knowledge, evidence) }

    fun isRelevant(
        question: L3GeneratedQuestion,
        knowledge: List<L3KnowledgePoint>,
        evidence: List<Evidence>,
    ): Boolean {
        val kp = knowledge.firstOrNull { it.id == question.knowledgePointId } ?: return false
        if (!SubjectKnowledgeExtractor.isAcceptedKnowledge(kp.title, kp.explanation)) return false
        val evidenceTexts = question.evidenceIds.mapNotNull { id -> evidence.firstOrNull { it.id == id }?.text?.trim() }
        if (evidenceTexts.isEmpty() || evidenceTexts.all { it.isBlank() }) return false
        if (SubjectKnowledgeExtractor.isNoiseLine(question.stem) || question.stem.isBlank()) return false
        if (question.options.any { it.contains("与课程无关") || it.contains("无关废话") }) return false
        val haystack = (question.stem + " " + question.explanation + " " + evidenceTexts.joinToString(" ")).lowercase()
        val titleTokens = subjectTokens(kp.title)
        val evidenceTokens = evidenceTexts.flatMap { subjectTokens(it) }.take(12)
        val hasTitleSignal = titleTokens.any { haystack.contains(it) }
        val hasEvidenceSignal = evidenceTokens.any { token ->
            question.stem.lowercase().contains(token) || question.explanation.lowercase().contains(token)
        }
        val hasAnswerSupport = question.explanation.contains("证据") || evidenceTexts.any { quote ->
            quote.length >= 8 && question.explanation.contains(quote.take(8))
        }
        return (hasTitleSignal || hasEvidenceSignal) && hasAnswerSupport
    }

    private fun subjectTokens(text: String): List<String> =
        text.split(Regex("[\\s，。；：、,.!?！？（）()《》“”\"'\\[\\]{}]+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }
            .filterNot { token -> SubjectKnowledgeExtractor.isNoiseLine(token) }
            .distinct()
}
