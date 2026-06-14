package com.classmate.core.sample

import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.Difficulty
import com.classmate.core.model.EvidenceSpan
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.ProviderKind
import com.classmate.core.model.QuestionType
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import com.classmate.core.model.SourceKind

/**
 * The "load sample course" content: a real 高数 · 无穷级数 lecture plus a HAND-AUTHORED,
 * evidence-bound analysis used for offline demos and screenshots.
 *
 * This is honestly labelled demo data (provenance.modelLabel + SourceKind.SAMPLE) — it is NOT
 * a fabricated live model call. Its evidence spans are resolved at runtime against the real
 * segmented text, so they are guaranteed to pass the same validators as any model output.
 * SampleCourseTest asserts exactly that.
 */
object SampleCourses {

    const val SERIES_SESSION_ID = "session_sample_series"
    const val SERIES_TITLE = "高等数学 · 无穷级数（示例课）"
    private const val SAMPLE_EPOCH = 1_700_000_000_000L

    /** One concept per line so each becomes its own evidence-addressable segment. */
    val SERIES_TEXT: String = listOf(
        "给定一个数列 a1, a2, a3, …，把它们依次相加得到的表达式称为无穷级数，记作 ∑an，其中 an 称为级数的通项或一般项。",
        "级数的前 n 项之和 Sn = a1 + a2 + … + an 称为级数的部分和，所有部分和按顺序排成的数列 {Sn} 称为部分和数列。",
        "如果部分和数列 {Sn} 当 n 趋于无穷时存在有限极限 S，就称级数收敛，并称 S 为级数的和；如果该极限不存在，就称级数发散。",
        "形如 ∑a·r^n 的级数称为等比级数，当公比满足 |r| < 1 时收敛，其和为 a/(1−r)；当 |r| ≥ 1 时发散。",
        "形如 ∑1/n^p 的级数称为 p 级数，它在 p > 1 时收敛，在 p ≤ 1 时发散；特别地，p = 1 时的调和级数 ∑1/n 发散。",
        "对各项非负的正项级数，常用比较判别法与比值判别法判断敛散性；比值判别法考察相邻两项之比的极限 ρ，ρ < 1 时收敛，ρ > 1 时发散。",
        "各项正负交替出现的级数称为交错级数；莱布尼茨判别法指出：若通项绝对值单调递减且趋于零，则交错级数收敛。",
        "如果各项取绝对值后所成的级数 ∑|an| 收敛，就称原级数绝对收敛；如果 ∑an 收敛但 ∑|an| 发散，则称原级数条件收敛。",
    ).joinToString("\n")

    fun seriesSession(nowMs: Long = SAMPLE_EPOCH): CourseSession =
        CourseSegmenter.buildSession(SERIES_SESSION_ID, SERIES_TITLE, SERIES_TEXT, nowMs, SourceKind.SAMPLE)

    fun seriesAnalysis(nowMs: Long = SAMPLE_EPOCH): CourseAnalysisResult {
        val session = seriesSession(nowMs)
        val resolver = EvidenceResolver()
        fun ev(segmentId: String, quote: String): List<EvidenceSpan> =
            resolver.resolve(session.segment(segmentId)!!, quote)?.let { listOf(it) } ?: emptyList()

        val knowledgePoints = listOf(
            KnowledgePoint(
                id = "kp_1", title = "级数的定义",
                summary = "把数列各项依次相加得到的无穷和式 ∑an 称为无穷级数，an 是它的通项。",
                sourceSegmentId = "seg_1", evidence = ev("seg_1", "称为无穷级数"),
                importance = Importance.HIGH, difficulty = Difficulty.EASY,
                relatedPointIds = listOf("kp_2"), tags = listOf("定义"),
            ),
            KnowledgePoint(
                id = "kp_2", title = "部分和数列",
                summary = "前 n 项之和 Sn 是部分和，{Sn} 构成部分和数列——级数敛散性正是用它来定义的。",
                sourceSegmentId = "seg_2", evidence = ev("seg_2", "称为部分和数列"),
                importance = Importance.HIGH, difficulty = Difficulty.EASY,
                relatedPointIds = listOf("kp_3"), tags = listOf("定义"),
            ),
            KnowledgePoint(
                id = "kp_3", title = "级数收敛与发散",
                summary = "部分和数列有有限极限则级数收敛、该极限为级数的和；极限不存在则发散。",
                sourceSegmentId = "seg_3", evidence = ev("seg_3", "就称级数收敛"),
                importance = Importance.CRITICAL, difficulty = Difficulty.MEDIUM,
                relatedPointIds = listOf("kp_2"), tags = listOf("核心", "判断"),
            ),
            KnowledgePoint(
                id = "kp_4", title = "等比级数",
                summary = "∑a·r^n 形式的级数；|r|<1 时收敛于 a/(1−r)，|r|≥1 时发散。",
                sourceSegmentId = "seg_4", evidence = ev("seg_4", "称为等比级数"),
                importance = Importance.HIGH, difficulty = Difficulty.MEDIUM,
                relatedPointIds = listOf("kp_3"), tags = listOf("典型级数"),
            ),
            KnowledgePoint(
                id = "kp_5", title = "p 级数",
                summary = "∑1/n^p；p>1 收敛、p≤1 发散，p=1 即发散的调和级数，是判别的重要标尺。",
                sourceSegmentId = "seg_5", evidence = ev("seg_5", "称为 p 级数"),
                importance = Importance.HIGH, difficulty = Difficulty.MEDIUM,
                relatedPointIds = listOf("kp_3"), tags = listOf("典型级数"),
            ),
            KnowledgePoint(
                id = "kp_6", title = "正项级数判别法",
                summary = "正项级数常用比较判别法与比值判别法；比值极限 ρ<1 收敛、ρ>1 发散。",
                sourceSegmentId = "seg_6", evidence = ev("seg_6", "比较判别法与比值判别法"),
                importance = Importance.HIGH, difficulty = Difficulty.HARD,
                relatedPointIds = listOf("kp_5"), tags = listOf("判别法"),
            ),
            KnowledgePoint(
                id = "kp_7", title = "交错级数",
                summary = "正负交替的级数；莱布尼茨判别法：通项绝对值单调递减且趋于零则收敛。",
                sourceSegmentId = "seg_7", evidence = ev("seg_7", "称为交错级数"),
                importance = Importance.MEDIUM, difficulty = Difficulty.MEDIUM,
                relatedPointIds = listOf("kp_8"), tags = listOf("判别法"),
            ),
            KnowledgePoint(
                id = "kp_8", title = "绝对收敛与条件收敛",
                summary = "∑|an| 收敛为绝对收敛；∑an 收敛但 ∑|an| 发散为条件收敛。",
                sourceSegmentId = "seg_8", evidence = ev("seg_8", "称原级数绝对收敛"),
                importance = Importance.HIGH, difficulty = Difficulty.HARD,
                relatedPointIds = listOf("kp_7", "kp_3"), tags = listOf("核心"),
            ),
        )

        val quizQuestions = listOf(
            QuizQuestion(
                id = "q_1", type = QuestionType.CONCEPT_UNDERSTANDING,
                stem = "判断一个级数是否收敛，本质上是在考察下列哪个对象的极限？",
                options = listOf(
                    QuizOption("opt_A", "部分和数列 {Sn} 的极限", true, "级数收敛的定义就是部分和数列存在有限极限。"),
                    QuizOption("opt_B", "通项 an 的极限", false, "通项趋于零只是收敛的必要条件，不能决定收敛。"),
                    QuizOption("opt_C", "项数 n 本身的大小", false, "项数总是趋于无穷，与敛散无关。"),
                    QuizOption("opt_D", "公比 r 的取值", false, "公比只对等比级数有意义。"),
                ),
                testedKnowledgePointIds = listOf("kp_3", "kp_2"),
                evidence = ev("seg_3", "就称级数收敛"),
                explanation = "级数收敛 ⇔ 部分和数列 {Sn} 收敛。这是把“无穷相加”转化为“数列极限”的关键一步。",
                difficulty = Difficulty.MEDIUM,
            ),
            QuizQuestion(
                id = "q_2", type = QuestionType.ERROR_ANALYSIS,
                stem = "小明认为「只要通项 an → 0，级数 ∑an 就一定收敛」。这个判断错在哪里？",
                options = listOf(
                    QuizOption("opt_A", "通项趋于零只是必要条件，调和级数 ∑1/n 的通项趋于零却发散", true, "经典反例：调和级数通项趋于零但发散。"),
                    QuizOption("opt_B", "没有错，通项趋于零就一定收敛", false, "这正是被反例推翻的常见误解。"),
                    QuizOption("opt_C", "应该看通项是否趋于 1", false, "收敛的必要条件是通项趋于零，不是趋于一。"),
                    QuizOption("opt_D", "只有正项级数才需要通项趋于零", false, "通项趋于零对任何收敛级数都成立。"),
                ),
                testedKnowledgePointIds = listOf("kp_5", "kp_3"),
                evidence = ev("seg_5", "调和级数"),
                explanation = "通项趋于零是收敛的必要而非充分条件；调和级数是必须记住的反例。",
                difficulty = Difficulty.HARD,
            ),
            QuizQuestion(
                id = "q_3", type = QuestionType.APPLICATION,
                stem = "下列级数中，哪一个是收敛的？",
                options = listOf(
                    QuizOption("opt_A", "∑ 1/n^2（p = 2）", true, "p 级数 p>1 收敛。"),
                    QuizOption("opt_B", "∑ 1/n（调和级数）", false, "p = 1，发散。"),
                    QuizOption("opt_C", "∑ 1/√n（p = 1/2）", false, "p ≤ 1，发散。"),
                    QuizOption("opt_D", "∑ 1（通项不趋于零）", false, "通项不趋于零，必发散。"),
                ),
                testedKnowledgePointIds = listOf("kp_5"),
                evidence = ev("seg_5", "在 p > 1 时收敛"),
                explanation = "套用 p 级数结论：只有 p>1 收敛。1/n^2 对应 p=2，故收敛。",
                difficulty = Difficulty.MEDIUM,
            ),
            QuizQuestion(
                id = "q_4", type = QuestionType.JUDGMENT,
                stem = "判断：等比级数 ∑a·r^n 当 |r| ≥ 1 时一定发散。",
                options = listOf(
                    QuizOption("opt_A", "正确", true, "|r|≥1 时部分和不趋于有限值，级数发散。"),
                    QuizOption("opt_B", "错误", false, "等比级数仅在 |r|<1 时收敛，故此判断成立。"),
                ),
                testedKnowledgePointIds = listOf("kp_4"),
                evidence = ev("seg_4", "当 |r| ≥ 1 时发散"),
                explanation = "等比级数收敛的充要条件是 |r|<1；|r|≥1 时必发散。",
                difficulty = Difficulty.MEDIUM,
            ),
            QuizQuestion(
                id = "q_5", type = QuestionType.TRANSFER,
                stem = "已知交错级数 ∑(−1)^(n+1)/n 收敛，而 ∑1/n 发散。据此该级数属于？",
                options = listOf(
                    QuizOption("opt_A", "条件收敛", true, "收敛但取绝对值后发散，正是条件收敛的定义。"),
                    QuizOption("opt_B", "绝对收敛", false, "绝对收敛要求 ∑|an| 也收敛，这里 ∑1/n 发散。"),
                    QuizOption("opt_C", "发散", false, "题目已说明该交错级数收敛。"),
                    QuizOption("opt_D", "等比级数", false, "它不具有 ∑a·r^n 的形式。"),
                ),
                testedKnowledgePointIds = listOf("kp_8", "kp_7"),
                evidence = ev("seg_8", "称原级数条件收敛"),
                explanation = "∑an 收敛但 ∑|an| 发散 ⇒ 条件收敛；这正是交错调和级数的著名例子。",
                difficulty = Difficulty.HARD,
            ),
        )

        return CourseAnalysisResult(
            sessionId = session.id,
            knowledgePoints = knowledgePoints,
            quizQuestions = quizQuestions,
            provenance = AnalysisProvenance(
                provider = ProviderKind.LOCAL_FALLBACK,
                fallbackUsed = true,
                modelLabel = "示例课程（人工编写演示数据）",
                createdAtEpochMs = nowMs,
            ),
        )
    }
}
