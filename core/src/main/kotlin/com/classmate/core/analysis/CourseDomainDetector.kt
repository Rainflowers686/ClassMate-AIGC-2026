package com.classmate.core.analysis

/**
 * Evidence-based course-domain detection. Replaces the old "lock the learner into one of a few
 * built-in subjects (and default physics-ish)" behaviour: the domain is inferred from what the
 * learner actually gave us (title / manual name / file name / OCR / document / transcript / known
 * knowledge points), scored against weighted per-domain term banks.
 *
 * Honesty contract:
 *  - This is a deterministic, on-device heuristic. It never claims cloud/AI provenance.
 *  - When the signal is weak or ambiguous it returns [CourseDomain.GENERAL] (通用课堂) and sets
 *    [CourseDomainResult.requiresUserConfirmation] = true rather than guessing a specific subject.
 *  - It must NEVER map clearly-mechanical content (齿轮/轴承/应力/应变/材料力学…) onto 大学物理:
 *    each domain owns disambiguating terms, and physics only scores on 洛伦兹力/电磁感应/磁通量… .
 */
enum class CourseDomain(val id: String, val displayName: String, val glossarySubjectHint: String?) {
    COMPUTER_SCIENCE("computer_science", "计算机", "C++"),
    MATHEMATICS("mathematics", "数学", "高等数学"),
    DISCRETE_MATH("discrete_math", "离散数学", "离散数学"),
    PHYSICS("physics", "大学物理", "大学物理"),
    MECHANICAL("mechanical", "机械", null),
    ELECTRICAL("electrical", "电子与电路", null),
    CHEMISTRY_MATERIALS("chemistry_materials", "化学与材料", null),
    ENVIRONMENT("environment", "环境", null),
    BIOLOGY("biology", "生物", null),
    MEDICINE("medicine", "医学", null),
    PSYCHOLOGY("psychology", "心理学", null),
    ECONOMICS_MANAGEMENT("economics_management", "经管", null),
    LAW_POLITICS("law_politics", "法学与思政", "马原"),
    LANGUAGE("language", "语言", null),
    AI_ML("ai_ml", "人工智能", "AI/机器学习"),
    GENERAL("general", "通用课堂", null);

    companion object {
        fun fromId(id: String?): CourseDomain? = entries.firstOrNull { it.id == id }
    }
}

data class CourseDomainResult(
    val domainId: String,
    val displayName: String,
    val confidence: Double,
    val evidenceTerms: List<String>,
    val alternativeDomains: List<String>,
    val requiresUserConfirmation: Boolean,
    val glossarySubjectHint: String? = null,
) {
    val domain: CourseDomain get() = CourseDomain.fromId(domainId) ?: CourseDomain.GENERAL
}

object CourseDomainDetector {

    /** Below this weighted score the top guess is too weak to assert — fall back to 通用课堂. */
    private const val WEAK_THRESHOLD = 2
    /** At/above this the top guess is confident enough to not require confirmation (with margin). */
    private const val CONFIDENT_THRESHOLD = 5
    /** The top must beat the runner-up by this margin to be confident. */
    private const val CONFIDENT_MARGIN = 3

    fun detect(
        title: String = "",
        manualCourseName: String = "",
        fileName: String = "",
        ocrText: String = "",
        documentText: String = "",
        transcript: String = "",
        knowledgePoints: List<String> = emptyList(),
    ): CourseDomainResult {
        // Title / manual name / file name are stronger signals than long body text, so weight them up
        // by repeating them; body text contributes once.
        val titleBlock = listOf(title, manualCourseName, fileName, knowledgePoints.joinToString(" "))
            .filter { it.isNotBlank() }.joinToString(" \n ")
        val bodyBlock = listOf(ocrText, documentText, transcript)
            .filter { it.isNotBlank() }.joinToString(" \n ")
        val haystack = (titleBlock + " \n " + titleBlock + " \n " + bodyBlock).lowercase()

        if (haystack.isBlank()) {
            return general(emptyList(), emptyList())
        }

        val scored = TERM_BANKS.map { (domain, terms) ->
            val matched = LinkedHashMap<String, Int>()
            var score = 0
            for ((term, weight) in terms) {
                if (haystack.contains(term.lowercase())) {
                    matched[term] = weight
                    score += weight
                }
            }
            DomainScore(domain, score, matched.keys.toList())
        }.filter { it.score > 0 }.sortedByDescending { it.score }

        if (scored.isEmpty()) {
            return general(emptyList(), emptyList())
        }

        val top = scored.first()
        val second = scored.getOrNull(1)
        val secondScore = second?.score ?: 0
        val alternatives = scored.drop(1).take(3).map { it.domain.id }
        val confidence = (top.score.toDouble() / (top.score + secondScore + 1.0)).coerceIn(0.0, 1.0)

        val confident = top.score >= CONFIDENT_THRESHOLD && (top.score - secondScore) >= CONFIDENT_MARGIN

        // Too weak to assert a specific subject → honest 通用课堂 + confirmation.
        if (top.score < WEAK_THRESHOLD) {
            return general(top.evidenceTerms, alternatives, baseConfidence = confidence)
        }

        return CourseDomainResult(
            domainId = top.domain.id,
            displayName = top.domain.displayName,
            confidence = confidence,
            evidenceTerms = top.evidenceTerms,
            alternativeDomains = alternatives,
            requiresUserConfirmation = !confident,
            glossarySubjectHint = top.domain.glossarySubjectHint,
        )
    }

    private fun general(
        evidence: List<String>,
        alternatives: List<String>,
        baseConfidence: Double = 0.0,
    ): CourseDomainResult = CourseDomainResult(
        domainId = CourseDomain.GENERAL.id,
        displayName = CourseDomain.GENERAL.displayName,
        confidence = baseConfidence,
        evidenceTerms = evidence,
        alternativeDomains = alternatives,
        requiresUserConfirmation = true,
        glossarySubjectHint = null,
    )

    /** The known seed terms for a domain — used to seed [com.classmate.core.glossary.DynamicGlossaryExtractor]. */
    fun seedTermsFor(domain: CourseDomain): List<String> = TERM_BANKS[domain]?.map { it.first } ?: emptyList()

    private data class DomainScore(val domain: CourseDomain, val score: Int, val evidenceTerms: List<String>)

    // Weighted term banks. 3 = strongly disambiguating, 2 = typical, 1 = weak/ambiguous.
    // Generic shared words (力 / 能量 / 运动 / 函数) are deliberately weak or absent so they cannot
    // tip a different domain into physics/math by themselves.
    private val TERM_BANKS: Map<CourseDomain, List<Pair<String, Int>>> = mapOf(
        CourseDomain.COMPUTER_SCIENCE to listOf(
            "python" to 3, "java" to 3, "c++" to 3, "javascript" to 3, "kotlin" to 3,
            "算法" to 2, "数据结构" to 3, "操作系统" to 3, "数据库" to 3, "编程" to 2,
            "链表" to 3, "二叉树" to 3, "哈希" to 2, "递归" to 2, "指针" to 2,
            "函数" to 1, "循环" to 1, "数组" to 2, "字典" to 1, "列表" to 1, "异常" to 1,
            "类" to 1, "对象" to 1, "进程" to 2, "线程" to 2, "编译" to 2,
            "sql" to 2, "http" to 2, "tcp" to 2, "缩进" to 2, "def " to 2, "import " to 1,
        ),
        CourseDomain.MATHEMATICS to listOf(
            "高数" to 3, "微积分" to 3, "极限" to 2, "导数" to 2, "积分" to 2,
            "级数" to 2, "收敛" to 2, "发散" to 2, "线性代数" to 3, "矩阵" to 2,
            "行列式" to 3, "特征值" to 3, "概率" to 2, "期望" to 2, "方差" to 2,
            "数列" to 2, "向量空间" to 3, "泰勒" to 3, "傅里叶" to 2,
        ),
        CourseDomain.DISCRETE_MATH to listOf(
            "离散数学" to 3, "命题逻辑" to 3, "谓词逻辑" to 3, "二元关系" to 3, "等价关系" to 3,
            "偏序" to 3, "哈斯图" to 3, "图论" to 2, "集合论" to 2, "范式" to 2, "自反" to 2,
            "传递" to 1, "笛卡尔积" to 3,
        ),
        CourseDomain.PHYSICS to listOf(
            "洛伦兹力" to 3, "电磁感应" to 3, "楞次定律" to 3, "磁通量" to 3, "法拉第" to 3,
            "安培定律" to 3, "麦克斯韦" to 3, "电动势" to 2, "感应电流" to 2, "动量守恒" to 3,
            "万有引力" to 3, "牛顿第二定律" to 3, "热力学第一定律" to 3, "光的干涉" to 3,
            "光的衍射" to 3, "相对论" to 2, "电场强度" to 3, "磁感应强度" to 3,
        ),
        CourseDomain.MECHANICAL to listOf(
            "齿轮" to 3, "轴承" to 3, "连杆" to 3, "扭矩" to 3, "机械设计" to 3,
            "机械原理" to 3, "自由度" to 2, "机构" to 2, "零件图" to 3, "装配图" to 3,
            "公差" to 3, "凸轮" to 3, "螺纹" to 2, "联轴器" to 3, "材料力学" to 3,
            "理论力学" to 3, "应力" to 2, "应变" to 2, "弯矩" to 3, "剪力" to 2,
            "工程制图" to 3, "三视图" to 3, "液压" to 2, "传动" to 2, "曲柄" to 3,
        ),
        CourseDomain.ELECTRICAL to listOf(
            "电路" to 3, "电阻" to 2, "电容" to 2, "电感" to 2, "二极管" to 3, "三极管" to 3,
            "运算放大器" to 3, "模拟电路" to 3, "数字电路" to 3, "逻辑门" to 3, "触发器" to 2,
            "整流" to 2, "滤波" to 2, "放大器" to 2, "单片机" to 3, "嵌入式" to 3,
            "传递函数" to 2, "自动控制" to 3, "pid" to 2, "调制" to 2, "通信原理" to 3,
        ),
        CourseDomain.CHEMISTRY_MATERIALS to listOf(
            "化学" to 2, "化合物" to 3, "化学键" to 3, "氧化还原" to 3, "摩尔" to 2,
            "有机化学" to 3, "无机化学" to 3, "高分子" to 3, "晶体结构" to 3, "合金" to 3,
            "聚合物" to 3, "催化剂" to 2, "电解" to 2, "官能团" to 3, "材料科学" to 3,
        ),
        CourseDomain.ENVIRONMENT to listOf(
            "环境工程" to 3, "污染" to 2, "废水" to 3, "废气" to 3, "大气污染" to 3,
            "生态" to 2, "可持续" to 2, "碳排放" to 3, "水处理" to 3, "固废" to 3, "环评" to 3,
        ),
        CourseDomain.BIOLOGY to listOf(
            "细胞" to 2, "基因" to 3, "dna" to 3, "蛋白质" to 2, "酶" to 2, "遗传" to 3,
            "进化" to 2, "光合作用" to 3, "细胞膜" to 3, "染色体" to 3, "生态系统" to 2,
        ),
        CourseDomain.MEDICINE to listOf(
            "解剖" to 3, "病理" to 3, "生理学" to 3, "药理" to 3, "临床" to 3, "免疫" to 2,
            "病毒" to 1, "细菌" to 1, "诊断" to 1, "症状" to 2, "器官" to 2, "内科" to 3, "外科" to 3,
        ),
        CourseDomain.PSYCHOLOGY to listOf(
            "心理学" to 3, "认知" to 1, "人格" to 2, "情绪" to 2, "行为主义" to 3,
            "精神分析" to 3, "条件反射" to 2, "记忆" to 1, "动机" to 2, "知觉" to 2,
        ),
        CourseDomain.ECONOMICS_MANAGEMENT to listOf(
            "经济学" to 3, "管理学" to 3, "会计" to 3, "金融" to 3, "财务" to 2, "市场营销" to 3,
            "供求" to 2, "成本" to 1, "利润" to 1, "资产负债" to 3, "投资" to 2, "利率" to 2,
            "通货膨胀" to 3, "gdp" to 3, "报表" to 2, "审计" to 3,
        ),
        CourseDomain.LAW_POLITICS to listOf(
            "法学" to 3, "宪法" to 3, "民法" to 3, "刑法" to 3, "合同法" to 3, "诉讼" to 3,
            "法条" to 2, "马克思" to 3, "马原" to 3, "思政" to 3, "辩证唯物" to 3,
            "生产关系" to 3, "社会主义" to 2, "毛概" to 3,
        ),
        CourseDomain.LANGUAGE to listOf(
            "英语" to 3, "日语" to 3, "语法" to 2, "词汇" to 2, "时态" to 3, "句型" to 2,
            "阅读理解" to 2, "听力" to 2, "口语" to 2, "grammar" to 3, "vocabulary" to 3,
            "五十音" to 3, "假名" to 3,
        ),
        CourseDomain.AI_ML to listOf(
            "机器学习" to 3, "深度学习" to 3, "神经网络" to 3, "梯度下降" to 3, "损失函数" to 3,
            "过拟合" to 3, "训练集" to 2, "卷积" to 3, "transformer" to 3, "强化学习" to 3,
        ),
    )
}
