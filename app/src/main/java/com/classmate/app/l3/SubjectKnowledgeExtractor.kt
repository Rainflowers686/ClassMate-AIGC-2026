package com.classmate.app.l3

/**
 * Filters classroom/OCR filler before the L3 pipeline turns text into knowledge points and quizzes.
 *
 * The goal is conservative: keep subject terms and evidence-backed sentences, but stop oral prompts,
 * page/UI noise, and emphasis phrases from becoming standalone knowledge points.
 */
object SubjectKnowledgeExtractor {
    private val noisePhrases = listOf(
        "同学们注意",
        "重点来了",
        "大家记一下",
        "这里可能考",
        "下面我们来看",
        "这个地方",
        "老师说",
        "作业要求",
        "请看这里",
        "看这里",
        "首先其次然后",
        "也就是说",
        "然后呢",
        "这个那个",
        "左上角",
        "右下角",
        "拍照",
        "截图",
        "页面",
        "按钮",
        "点击",
        "打开",
        "关闭",
        "上传",
        "下载",
        "注意",
        "好的",
        "反正",
        "总之",
    )

    private val weakFillers = listOf("嗯", "啊", "呃", "呢", "这个", "那个")

    private val subjectTerms = listOf(
        "定义",
        "概念",
        "原理",
        "定律",
        "定理",
        "公式",
        "性质",
        "结论",
        "证明",
        "步骤",
        "方法",
        "模型",
        "结构",
        "分类",
        "条件",
        "原因",
        "影响",
        "作用",
        "导数",
        "极限",
        "积分",
        "函数",
        "矩阵",
        "概率",
        "显著性检验",
        "牛顿",
        "加速度",
        "速度",
        "电磁感应",
        "磁通量",
        "感应电流",
        "电动势",
        "动能",
        "能量",
        "守恒",
        "知识点",
        "二叉树",
        "指针",
        "继承",
        "多态",
        "算法",
        "复杂度",
        "类",
        "实践",
        "认识",
        "矛盾",
        "生态系统",
        "污染物",
        "化学需氧量",
        "生态",
        "law",
        "theorem",
        "principle",
        "function",
        "derivative",
        "matrix",
        "probability",
        "algorithm",
        "class",
        "inheritance",
        "force",
        "acceleration",
        "flux",
        "voltage",
        "current",
        "TCP",
        "HTTP",
        "JVM",
    )

    private val formulaRegex = Regex("""[=+\-*/^∑√π≤≥<>]|(\blim\b|\bsin\b|\bcos\b|\btan\b|\blog\b)""", RegexOption.IGNORE_CASE)
    private val punctuationRegex = Regex("""^[\s:：，,。.!！?？、；;]+""")
    private val dateLikeRegex = Regex("""^\d{1,4}([./\-年])\d{1,2}([./\-月])?\d{0,2}(日)?$""")

    fun filterEvidenceParagraphs(paragraphs: List<String>, courseTitle: String = ""): List<String> {
        val accepted = paragraphs
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .filterNot { isNoiseLine(it) }
            .filter { subjectScore(it, courseTitle) > 0 || meaningfulLength(it) >= 18 }
            .distinct()
        return accepted
    }

    fun titleFromEvidence(text: String, index: Int, courseTitle: String = ""): String {
        val candidates = sentenceCandidates(text)
            .map { stripLeadingNoise(it) }
            .filter { it.length >= 2 }
            .filterNot { isNoiseLine(it) }
            .sortedWith(compareByDescending<String> { subjectScore(it, courseTitle) }.thenBy { it.length })
        val selected = candidates.firstOrNull { subjectScore(it, courseTitle) > 0 }
            ?: candidates.firstOrNull { meaningfulLength(it) >= 6 }
            ?: return ""
        return compactTitle(selected).ifBlank { "知识点${index + 1}" }.take(22)
    }

    fun isAcceptedKnowledge(title: String, evidenceText: String, courseTitle: String = ""): Boolean {
        val cleanTitle = stripLeadingNoise(title)
        val cleanEvidence = normalize(evidenceText)
        if (cleanTitle.isBlank() || cleanEvidence.isBlank()) return false
        if (isNoiseLine(cleanTitle)) return false
        if (cleanTitle.startsWith("知识点") && subjectScore(cleanEvidence, courseTitle) == 0) return false
        return subjectScore("$cleanTitle $cleanEvidence", courseTitle) > 0 ||
            (!isNoiseLine(cleanEvidence) && meaningfulLength(cleanEvidence) >= 6 && meaningfulLength(cleanTitle) >= 2)
    }

    fun isNoiseLine(text: String): Boolean {
        val clean = normalize(text)
        if (clean.isBlank()) return true
        if (clean.length <= 1) return true
        if (clean.all { it.isDigit() || it.isWhitespace() }) return true
        if (dateLikeRegex.matches(clean)) return true
        if (clean.length <= 4 && weakFillers.any { clean == it || clean == "$it。" || clean == "$it，" }) return true
        val hasSubject = subjectScore(clean, "") > 0
        val noiseHits = noisePhrases.count { clean.contains(it, ignoreCase = true) }
        if (noiseHits > 0 && !hasSubject && clean.length <= 28) return true
        if (noiseHits >= 2 && !hasSubject) return true
        val mostlySymbols = clean.count { !it.isLetterOrDigit() && !isCjk(it) } > clean.length * 0.55
        if (mostlySymbols && !hasSubject) return true
        return false
    }

    fun subjectScore(text: String, courseTitle: String = ""): Int {
        val clean = normalize(text)
        if (clean.isBlank()) return 0
        var score = subjectTerms.count { clean.contains(it, ignoreCase = true) }
        if (formulaRegex.containsMatchIn(clean)) score += 2
        if (courseTitle.isNotBlank()) {
            courseTitle.split(Regex("""[\s:：，,。.!！?？、；;]+"""))
                .filter { it.length >= 2 }
                .forEach { if (clean.contains(it, ignoreCase = true)) score += 1 }
        }
        if (Regex("""(是|表示|说明|用于|由|决定|导致|影响|计算|判断|定义为)""").containsMatchIn(clean)) score += 1
        return score
    }

    private fun sentenceCandidates(text: String): List<String> =
        normalize(text)
            .split(Regex("""(?<=[。.!！?？；;])\s*|[\n\r]+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(normalize(text)) }

    private fun stripLeadingNoise(text: String): String {
        var clean = normalize(text)
        var changed: Boolean
        do {
            changed = false
            noisePhrases.forEach { phrase ->
                if (clean.startsWith(phrase, ignoreCase = true)) {
                    clean = clean.removePrefix(phrase).replaceFirst(punctuationRegex, "").trim()
                    changed = true
                }
            }
        } while (changed)
        return clean
    }

    private fun compactTitle(text: String): String {
        val stripped = stripLeadingNoise(text)
        val beforeDefinition = stripped.split(Regex("""(是|表示|说明|用于|由|决定|导致|影响|可以|需要)"""), limit = 2).first().trim()
        val picked = beforeDefinition.takeIf { it.length in 2..22 } ?: stripped
        return picked.replace(Regex("""[\s，,。.!！?？；;：:]+$"""), "").trim()
    }

    private fun normalize(text: String): String =
        text.replace(Regex("""\s+"""), " ").trim()

    private fun meaningfulLength(text: String): Int =
        text.count { it.isLetterOrDigit() || isCjk(it) }

    private fun isCjk(ch: Char): Boolean =
        ch.code in 0x4E00..0x9FFF
}
