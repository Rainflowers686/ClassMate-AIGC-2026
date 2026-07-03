package com.classmate.app.l3

import com.classmate.core.model.Difficulty

/**
 * Local fallback quiz generation from accepted subject knowledge cards.
 *
 * Evidence is used as grounding only. Student-visible questions must ask about subject concepts,
 * formulas, properties, and misconceptions rather than copying recognition text or exposing internal
 * quality rules.
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
        val cards = knowledge
            .filter { it.sourceEvidenceIds.any { id -> evidenceById[id]?.text?.isNotBlank() == true } }
            .filter { it.title.isNotBlank() && !SubjectKnowledgeExtractor.isNoiseLine(it.title) }
            .take(maxQuestions.coerceAtLeast(1))

        val base = cards.mapIndexedNotNull { index, kp ->
            val evidenceId = kp.sourceEvidenceIds.firstOrNull { id -> evidenceById[id]?.text?.isNotBlank() == true }
                ?: return@mapIndexedNotNull null
            val quote = subjectQuote(evidenceById[evidenceId]?.text.orEmpty(), kp)
            val related = cards
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
        if (cards.isEmpty() || base.size >= maxQuestions.coerceAtLeast(1)) return base
        val target = minOf(maxQuestions.coerceAtLeast(1), maxOf(3, base.size))
        val extra = mutableListOf<L3GeneratedQuestion>()
        var nextIndex = base.size
        while (base.size + extra.size < target && nextIndex < target + cards.size * 2) {
            val kp = cards[nextIndex % cards.size]
            val evidenceId = kp.sourceEvidenceIds.firstOrNull { id -> evidenceById[id]?.text?.isNotBlank() == true }
                ?: break
            val quote = subjectQuote(evidenceById[evidenceId]?.text.orEmpty(), kp)
            extra += buildQuestion(
                id = "${idPrefix}_${now}_${nextIndex + 1}",
                lessonId = lessonId,
                kp = kp,
                evidenceId = evidenceId,
                evidenceQuote = quote,
                relatedTitles = cards.filter { it.id != kp.id }.map { it.title },
                index = nextIndex,
            )
            nextIndex += 1
        }
        return (base + extra).take(maxQuestions.coerceAtLeast(1))
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
        val quote = subjectQuote(evidenceQuote, kp)
            .ifBlank { kp.explanation.ifBlank { kp.title } }
            .take(180)
        return when {
            index % 5 == 1 -> fillBlankQuestion(id, lessonId, kp, evidenceId, quote)
            index % 5 == 3 -> trueFalseQuestion(id, lessonId, kp, evidenceId, quote, index)
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
        val pack = subjectPack(title, evidenceQuote, relatedTitles)
        val correctLetter = correctLetters[index % correctLetters.size]
        val optionsByLetter = placeCorrectOption(correctLetter, pack.correct, pack.distractors)
        return L3GeneratedQuestion(
            id = id,
            lessonId = lessonId,
            knowledgePointId = kp.id,
            stem = pack.stem,
            options = optionsByLetter.map { (letter, text) -> "$letter. $text" },
            correctAnswer = correctLetter,
            explanation = explanation(
                correctAnswer = correctLetter,
                title = title,
                whyCorrect = pack.whyCorrect,
                wrongRationale = pack.wrongRationale,
                evidenceQuote = evidenceQuote,
            ),
            evidenceIds = listOf(evidenceId),
            difficulty = Difficulty.MEDIUM,
        )
    }

    private fun fillBlankQuestion(
        id: String,
        lessonId: String,
        kp: L3KnowledgePoint,
        evidenceId: String,
        evidenceQuote: String,
    ): L3GeneratedQuestion {
        val title = kp.title.trim().ifBlank { "本课知识点" }
        val answer = fillAnswer(title, evidenceQuote)
        return L3GeneratedQuestion(
            id = id,
            lessonId = lessonId,
            knowledgePointId = kp.id,
            stem = "填空题：本题考查「$title」。请填写关键概念、公式或性质：____。",
            options = emptyList(),
            correctAnswer = answer,
            explanation = "正确答案：$answer。答案详解：本题要求写出「$title」中最核心的概念或公式；掌握它有助于判断条件和结论之间的关系。证据摘录：$evidenceQuote",
            evidenceIds = listOf(evidenceId),
            difficulty = Difficulty.EASY,
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
            falseStatement(title)
        } else {
            trueStatement(title, evidenceQuote)
        }
        val correct = if (makeFalse) "B" else "A"
        return L3GeneratedQuestion(
            id = id,
            lessonId = lessonId,
            knowledgePointId = kp.id,
            stem = "判断题：关于「$title」，下面说法是否正确？$statement",
            options = listOf("A. 正确", "B. 错误"),
            correctAnswer = correct,
            explanation = explanation(
                correctAnswer = correct,
                title = title,
                whyCorrect = if (makeFalse) "该说法颠倒或扩大了适用条件。" else "该说法概括了本知识点的核心关系。",
                wrongRationale = "相反选项没有准确反映定义、条件和结论。",
                evidenceQuote = evidenceQuote,
            ),
            evidenceIds = listOf(evidenceId),
            difficulty = Difficulty.EASY,
        )
    }

    private data class SubjectPack(
        val stem: String,
        val correct: String,
        val distractors: List<String>,
        val whyCorrect: String,
        val wrongRationale: String,
    )

    private fun subjectPack(title: String, quote: String, relatedTitles: List<String>): SubjectPack {
        val lower = title.lowercase()
        return when {
            title.contains("根值") || title.contains("柯西") -> SubjectPack(
                stem = "关于正项级数的根值判别法，下列说法正确的是？",
                correct = "若 limsup (a_n)^(1/n) < 1，则级数收敛",
                distractors = listOf(
                    "若 limsup (a_n)^(1/n) > 1，则级数一定收敛",
                    "若 limsup (a_n)^(1/n) = 1，则一定发散",
                    "根值判别法只能用于有限项求和",
                ),
                whyCorrect = "根值判别法通过根式极限与 1 的大小关系判断收敛性。",
                wrongRationale = "错误项分别把大于 1、等于 1 和适用对象理解错了。",
            )
            title.contains("重排") || title.contains("绝对收敛") -> SubjectPack(
                stem = "关于级数重排的性质，下列说法正确的是？",
                correct = "绝对收敛级数任意重排后仍收敛且和不变",
                distractors = listOf(
                    "条件收敛级数任意重排后和一定不变",
                    "发散级数经过重排后一定收敛",
                    "级数重排只改变前几项，不影响整体性质",
                ),
                whyCorrect = "绝对收敛保证重排后收敛性与和保持不变。",
                wrongRationale = "错误项混淆了绝对收敛、条件收敛和发散级数的重排性质。",
            )
            title.contains("相对论") || title.contains("时间膨胀") || title.contains("长度收缩") -> SubjectPack(
                stem = "关于狭义相对论时空观，下列说法正确的是？",
                correct = "同时性、时间间隔和长度测量会随参考系不同而变化",
                distractors = listOf(
                    "所有参考系中两个事件是否同时都完全相同",
                    "长度收缩会沿任意方向同等发生",
                    "因果先后关系可以被参考系任意颠倒",
                ),
                whyCorrect = "狭义相对论说明时空测量依赖参考系，但因果关系保持一致。",
                wrongRationale = "错误项忽略参考系差异、收缩方向或因果约束。",
            )
            title.contains("牛顿第二定律") || title.contains("F=ma") || title.contains("加速度") || title.contains("合外力") || title.contains("质量") -> SubjectPack(
                stem = "关于牛顿第二定律，下列说法正确的是？",
                correct = "物体加速度与合外力成正比，与质量成反比，可用 F=ma 表示",
                distractors = listOf(
                    "质量越大，在相同合外力下加速度越大",
                    "没有合外力时物体一定产生加速度",
                    "F=ma 只描述速度大小，和力没有关系",
                ),
                whyCorrect = "牛顿第二定律建立了合外力、质量和加速度之间的定量关系。",
                wrongRationale = "错误项分别颠倒质量关系、忽略合外力条件或误解公式含义。",
            )
            lower.contains("指针") || title.contains("二叉树") || title.contains("继承") || title.contains("类") -> SubjectPack(
                stem = "关于「$title」的程序设计理解，下列说法正确的是？",
                correct = subjectCorrectOption(title, quote),
                distractors = genericDistractors(title, relatedTitles, domain = "程序设计"),
                whyCorrect = "正确项体现了该概念在结构、行为或约束上的作用。",
                wrongRationale = "错误项混淆了概念边界、适用场景或实现关系。",
            )
            else -> SubjectPack(
                stem = "关于「$title」的理解，下列说法正确的是？",
                correct = subjectCorrectOption(title, quote),
                distractors = genericDistractors(title, relatedTitles),
                whyCorrect = "正确项概括了该知识点的定义、条件或关键结论。",
                wrongRationale = "错误项分别混淆相近概念、颠倒条件关系，或只停留在名称层面。",
            )
        }
    }

    private fun genericDistractors(title: String, relatedTitles: List<String>, domain: String = "本学科"): List<String> =
        listOfNotNull(
            relatedTitles.getOrNull(0)?.let { "把「$it」的结论误用于「$title」" },
            relatedTitles.getOrNull(1)?.let { "混淆「$it」与「$title」的适用范围" },
            "颠倒「$title」中的条件和结论",
            "把「$title」理解成孤立名称，忽略${domain}中的定义和条件",
        ).take(3)

    private fun subjectCorrectOption(title: String, quote: String): String {
        val clean = SubjectKnowledgeExtractor.cleanSubjectTextForDisplay(quote)
        val relation = clean
            .split(Regex("[。；;.!?！？\\n]"))
            .map { it.trim() }
            .firstOrNull { it.contains(title.take(2)) || SubjectKnowledgeExtractor.subjectScore(it) > 0 }
            ?.take(72)
        return if (relation.isNullOrBlank() || StudentVisibleQuizSanitizer.hasBlockedText(relation)) {
            "能说明「$title」的定义、适用条件和核心结论"
        } else {
            "能说明「$title」的核心关系：$relation"
        }
    }

    private fun fillAnswer(title: String, quote: String): String = when {
        title.contains("F=ma") || quote.contains("F=ma") -> "F=ma"
        title.contains("根值") || title.contains("柯西") -> "根值判别法"
        title.contains("重排") -> "绝对收敛"
        title.contains("时间膨胀") -> "时间膨胀"
        title.contains("长度收缩") -> "长度收缩"
        else -> title.take(24).ifBlank { "核心概念" }
    }

    private fun trueStatement(title: String, quote: String): String =
        subjectCorrectOption(title, quote)

    private fun falseStatement(title: String): String = when {
        title.contains("根值") || title.contains("柯西") -> "当根值极限等于 1 时，根值判别法一定能直接判断收敛或发散。"
        title.contains("重排") -> "条件收敛级数在任意重排后，和一定保持不变。"
        title.contains("相对论") || title.contains("时间膨胀") -> "不同参考系中时间和长度测量完全相同。"
        title.contains("牛顿第二定律") || title.contains("F=ma") -> "在相同合外力下，质量越大加速度越大。"
        else -> "该知识点的适用条件可以任意扩大，不会影响结论。"
    }

    private fun explanation(
        correctAnswer: String,
        title: String,
        whyCorrect: String,
        wrongRationale: String,
        evidenceQuote: String,
    ): String =
        "答案详解：$correctAnswer 正确。知识点：$title。为什么正确：$whyCorrect 其他选项为什么错误：$wrongRationale 证据摘录：$evidenceQuote"

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
                letter to distractors.getOrElse(distractorIndex++) { "混淆条件、概念范围或结论方向" }
            }
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
