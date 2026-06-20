package com.classmate.app.l3

import com.classmate.core.model.Difficulty

object QuestionBankParser {
    private val optionLine = Regex("""^([A-Da-d])[.)、]\s*(.+)$""")

    fun parse(raw: String, title: String = "导入题库", now: Long = System.currentTimeMillis()): QuestionBankParseResult {
        val clean = raw.trim()
        if (clean.isBlank()) return QuestionBankParseResult(false, errors = listOf("题库内容为空"))
        val questions = if (looksLikeCsv(clean)) parseCsv(clean, title, now) else parseMarkdown(clean, title, now)
        if (questions.isEmpty()) {
            return QuestionBankParseResult(
                accepted = false,
                errors = listOf("未解析出题目，请使用 Q:/Answer:/Explanation: 或 CSV 模板。"),
            )
        }
        val bank = L3QuestionBank(
            id = "qb_$now",
            title = title.ifBlank { "导入题库" },
            questions = questions,
            sourceText = clean,
            importedAt = now,
        )
        return QuestionBankParseResult(accepted = true, bank = bank)
    }

    private fun looksLikeCsv(raw: String): Boolean =
        raw.lineSequence().firstOrNull { it.isNotBlank() }?.contains(",") == true &&
            raw.contains("Answer", ignoreCase = true)

    private fun parseMarkdown(raw: String, title: String, now: Long): List<L3GeneratedQuestion> {
        val blocks = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        raw.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("Q:", ignoreCase = true) && current.any { it.startsWith("Q:", ignoreCase = true) }) {
                blocks += current
                current = mutableListOf()
            }
            if (trimmed.isNotBlank()) current += trimmed
        }
        if (current.isNotEmpty()) blocks += current
        return blocks.mapIndexedNotNull { index, lines ->
            val stem = lines.firstOrNull { it.startsWith("Q:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                .orEmpty()
            val options = lines.mapNotNull { optionLine.find(it)?.let { m -> m.groupValues[1].uppercase() to m.groupValues[2].trim() } }
            val answer = lines.firstOrNull { it.startsWith("Answer:", ignoreCase = true) || it.startsWith("答案:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.take(1)
                ?.uppercase()
                .orEmpty()
            val explanation = lines.firstOrNull { it.startsWith("Explanation:", ignoreCase = true) || it.startsWith("解析:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                .orEmpty()
            buildQuestion(index, now, title, stem, options, answer, explanation)
        }
    }

    private fun parseCsv(raw: String, title: String, now: Long): List<L3GeneratedQuestion> {
        val rows = raw.lines().filter { it.isNotBlank() }
        if (rows.size < 2) return emptyList()
        val header = splitCsv(rows.first()).map { it.trim().lowercase() }
        fun indexOf(vararg names: String): Int = names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } } ?: -1
        val stemAt = indexOf("q", "question", "stem", "题干")
        val answerAt = indexOf("answer", "答案")
        val explanationAt = indexOf("explanation", "解析")
        val optionIndices = listOf("a", "b", "c", "d").map { indexOf(it) }
        if (stemAt < 0 || answerAt < 0 || optionIndices.any { it < 0 }) return emptyList()
        return rows.drop(1).mapIndexedNotNull { index, line ->
            val cols = splitCsv(line)
            val stem = cols.getOrNull(stemAt).orEmpty().trim()
            val options = optionIndices.mapIndexed { i, col -> ('A' + i).toString() to cols.getOrNull(col).orEmpty().trim() }
            val answer = cols.getOrNull(answerAt).orEmpty().trim().take(1).uppercase()
            val explanation = cols.getOrNull(explanationAt).orEmpty().trim()
            buildQuestion(index, now, title, stem, options, answer, explanation)
        }
    }

    private fun buildQuestion(
        index: Int,
        now: Long,
        title: String,
        stem: String,
        options: List<Pair<String, String>>,
        answer: String,
        explanation: String,
    ): L3GeneratedQuestion? {
        val validOptions = options.filter { it.second.isNotBlank() }
        if (stem.isBlank() || validOptions.size < 2 || answer !in validOptions.map { it.first }) return null
        val kpId = "qb_kp_${now}_${index + 1}"
        val evidenceId = "qb_ev_${now}_${index + 1}"
        return L3GeneratedQuestion(
            id = "qb_q_${now}_${index + 1}",
            lessonId = "qb_lesson_$now",
            knowledgePointId = kpId,
            stem = stem,
            options = validOptions.map { "${it.first}. ${it.second}" },
            correctAnswer = answer,
            explanation = explanation.ifBlank { "答案 $answer 来自导入题库；请结合原题与课堂证据复核。" },
            evidenceIds = listOf(evidenceId),
            difficulty = Difficulty.MEDIUM,
        )
    }

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        line.forEach { ch ->
            when {
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    out += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        out += current.toString()
        return out
    }
}
