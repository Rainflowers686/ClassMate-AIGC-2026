package com.classmate.app.glossary

data class CourseTerm(
    val subject: String,
    val term: String,
    val aliases: List<String>,
    val shortDefinition: String,
    val priority: Int,
)

object CourseGlossary {
    const val DEFAULT_SUBJECT = "高等数学"

    val terms: List<CourseTerm> = buildList {
        addTerms("高等数学", listOf(
            t("数项级数", listOf("级数"), "数列各项依次相加形成的无穷和式", 5),
            t("部分和", listOf("Sn", "前 n 项和"), "级数前 n 项之和，用于定义收敛", 5),
            t("收敛", listOf("级数收敛"), "部分和数列趋向有限极限", 5),
            t("发散", listOf("级数发散"), "部分和数列没有有限极限", 5),
            t("必要条件", listOf("通项趋零"), "级数收敛必有通项趋于零", 4),
            t("几何级数", listOf("等比级数"), "公比绝对值小于 1 时收敛的典型级数", 5),
            t("p 级数", listOf("p-series"), "求和 1/n^p 的典型判别参照", 5),
            t("比较判别法", listOf("直接比较"), "用已知级数夹逼判断正项级数敛散", 4),
            t("极限比较判别法", listOf("极限比较"), "通过通项比值极限判断同敛散", 4),
            t("比值判别法", listOf("达朗贝尔判别法"), "考察相邻项比值极限", 4),
            t("根值判别法", listOf("柯西判别法"), "考察通项绝对值的 n 次根极限", 4),
            t("交错级数", listOf("正负交错级数"), "通项符号正负交替的级数", 3),
            t("莱布尼茨判别法", listOf("交错级数判别法"), "交错级数单调趋零时收敛", 3),
            t("绝对收敛", listOf("绝对值级数收敛"), "取绝对值后的级数也收敛", 4),
            t("条件收敛", listOf("非绝对收敛"), "原级数收敛但绝对值级数发散", 4),
        ))
        addTerms("大学物理", listOf(
            t("磁通量", listOf("磁通"), "磁感应强度穿过面积的通量", 5),
            t("法拉第定律", listOf("电磁感应定律"), "感应电动势等于磁通量变化率的负值", 5),
            t("楞次定律", listOf("感应电流方向"), "感应电流方向阻碍磁通量变化", 5),
            t("感应电动势", listOf("电动势"), "电磁感应中产生的电势差", 5),
            t("感应电流", listOf("感生电流"), "闭合回路中由感应电动势产生的电流", 4),
            t("动生电动势", listOf("运动电动势"), "导体切割磁感线产生的电动势", 4),
            t("洛伦兹力", listOf("磁场力"), "运动电荷在磁场中受到的力", 4),
            t("自感", listOf("自感现象"), "线圈自身电流变化引起自身感应电动势", 4),
            t("互感", listOf("互感现象"), "一个线圈电流变化引起另一线圈感应电动势", 4),
            t("电感", listOf("自感系数"), "描述线圈储存磁场能量能力的量", 4),
            t("磁场能", listOf("电感能量"), "电感中与电流平方相关的能量", 3),
            t("右手定则", listOf("方向判定"), "判断动生电动势或受力方向的方法", 3),
            t("能量守恒", listOf("能量转换"), "机械能、电能和热能之间的转换约束", 4),
            t("变压器", listOf("互感应用"), "基于互感改变交流电压的装置", 3),
            t("焦耳热", listOf("电阻热"), "电流通过电阻时产生的热量", 3),
        ))
        addTerms("离散数学", listOf(
            t("二元关系", listOf("关系"), "笛卡尔积上的子集", 5),
            t("笛卡尔积", listOf("有序对集合"), "两个集合元素组成有序对的集合", 4),
            t("关系矩阵", listOf("矩阵表示"), "用 0/1 矩阵表示关系", 4),
            t("关系图", listOf("有向图表示"), "用点和有向边表示关系", 3),
            t("自反", listOf("自反性"), "每个元素都与自身相关", 5),
            t("对称", listOf("对称性"), "a 相关 b 则 b 相关 a", 5),
            t("反对称", listOf("反对称性"), "互相关时两元素必须相等", 5),
            t("传递", listOf("传递性"), "a 相关 b 且 b 相关 c 推出 a 相关 c", 5),
            t("等价关系", listOf("等价"), "自反、对称、传递的关系", 5),
            t("等价类", listOf("商集元素"), "与某元素等价的所有元素集合", 4),
            t("划分", listOf("集合划分"), "互不相交且并为全集的子集族", 4),
            t("偏序", listOf("偏序关系"), "自反、反对称、传递的关系", 5),
            t("全序", listOf("线序"), "任意两个元素都可比较的偏序", 3),
            t("哈斯图", listOf("Hasse 图"), "省略自反边和传递边的偏序图", 4),
            t("极大元", listOf("maximal element"), "没有更大元素压住的元素", 3),
            t("最大元", listOf("greatest element"), "大于等于所有元素的元素", 4),
        ))
        addTerms("C++", listOf(
            t("指针", listOf("pointer"), "保存对象地址的变量", 5),
            t("引用", listOf("reference"), "对象的别名", 5),
            t("取地址", listOf("& 运算符"), "取得对象内存地址", 4),
            t("解引用", listOf("* 运算符"), "访问指针指向的对象", 4),
            t("空指针", listOf("nullptr"), "不指向有效对象的指针", 5),
            t("野指针", listOf("未初始化指针"), "值不可预测的危险指针", 5),
            t("悬空指针", listOf("dangling pointer"), "指向已失效对象的指针", 5),
            t("值传递", listOf("pass by value"), "复制实参到形参", 4),
            t("指针传递", listOf("pass by pointer"), "通过地址访问或修改实参", 4),
            t("引用传递", listOf("pass by reference"), "形参作为实参别名", 4),
            t("const 引用", listOf("常量引用"), "避免复制且不修改对象", 4),
            t("生命周期", listOf("object lifetime"), "对象从构造到销毁的有效期间", 5),
            t("动态分配", listOf("new/delete"), "运行时申请和释放内存", 3),
            t("RAII", listOf("资源获取即初始化"), "用对象生命周期管理资源", 4),
            t("智能指针", listOf("unique_ptr", "shared_ptr"), "自动管理动态对象所有权的工具", 4),
        ))
        addTerms("马原", listOf(
            t("实践", listOf("实践活动"), "人能动改造客观世界的社会性物质活动", 5),
            t("认识", listOf("认知"), "主体对客体的反映和把握", 5),
            t("感性认识", listOf("感觉", "知觉", "表象"), "直接具体生动的认识阶段", 4),
            t("理性认识", listOf("概念", "判断", "推理"), "间接抽象概括的认识阶段", 4),
            t("真理", listOf("正确认识"), "对客观事物及规律的正确反映", 5),
            t("真理客观性", listOf("客观真理"), "真理内容不依主观意愿改变", 4),
            t("真理具体性", listOf("具体真理"), "真理在一定条件和范围内成立", 4),
            t("绝对真理", listOf("绝对性"), "认识能够正确反映客观世界的方面", 3),
            t("相对真理", listOf("相对性"), "具体认识受历史条件限制的方面", 3),
            t("实践标准", listOf("检验真理标准"), "实践是检验真理的唯一标准", 5),
            t("生产实践", listOf("物质生产"), "最基本的实践形式", 4),
            t("科学实验", listOf("实验实践"), "探索和验证规律的重要实践形式", 3),
            t("社会历史性", listOf("历史条件"), "实践和认识受社会历史条件制约", 3),
            t("认识飞跃", listOf("从感性到理性"), "由经验上升到规律性把握", 4),
            t("再实践", listOf("实践认识循环"), "认识回到实践中检验和发展", 4),
        ))
        addTerms("AI/机器学习", listOf(
            t("机器学习", listOf("ML"), "模型从数据中学习规律的方法", 5),
            t("训练集", listOf("train set"), "用于学习模型参数的数据", 5),
            t("验证集", listOf("validation set"), "用于选择模型和调参的数据", 4),
            t("测试集", listOf("test set"), "用于最终评估泛化能力的数据", 5),
            t("特征", listOf("feature"), "模型输入的数据表示", 4),
            t("标签", listOf("label"), "监督学习中的目标输出", 4),
            t("损失函数", listOf("loss"), "衡量预测与真实差距的函数", 5),
            t("梯度下降", listOf("gradient descent"), "沿梯度方向更新参数的优化方法", 4),
            t("学习率", listOf("learning rate"), "控制参数更新步长的超参数", 4),
            t("过拟合", listOf("overfitting"), "训练集好但新数据表现差", 5),
            t("欠拟合", listOf("underfitting"), "模型连训练规律也未学好", 4),
            t("泛化能力", listOf("generalization"), "模型在新数据上的表现能力", 5),
            t("正则化", listOf("regularization"), "限制模型复杂度以缓解过拟合", 4),
            t("准确率", listOf("accuracy"), "分类预测正确比例", 3),
            t("召回率", listOf("recall"), "正类中被找回的比例", 3),
            t("F1 值", listOf("F1 score"), "精确率和召回率的调和平均", 3),
        ))
    }

    val subjects: List<String> = terms.map { it.subject }.distinct()

    fun termsFor(subject: String): List<CourseTerm> = terms.filter { it.subject == subject }

    fun countFor(subject: String): Int = termsFor(subject).size

    /**
     * Bounded (<= [max]) DISPLAY-ONLY hint of glossary terms (or aliases) that appear in [text], so a
     * user can eyeball OCR/ASR spelling. It NEVER rewrites the user's text and is NEVER used as
     * evidence — purely a "你可以检查这些术语" nudge. Returns canonical term names, de-duplicated.
     */
    fun matchingTerms(subject: String, text: String, max: Int = 20): List<String> {
        if (text.isBlank()) return emptyList()
        return termsFor(subject)
            .filter { term -> term.term in text || term.aliases.any { it in text } }
            .map { it.term }
            .distinct()
            .take(max.coerceAtLeast(0))
    }

    private fun MutableList<CourseTerm>.addTerms(subject: String, items: List<PartialTerm>) {
        items.forEach { item ->
            add(CourseTerm(subject, item.term, item.aliases, item.shortDefinition, item.priority))
        }
    }

    private fun t(term: String, aliases: List<String>, shortDefinition: String, priority: Int) =
        PartialTerm(term, aliases, shortDefinition, priority)

    private data class PartialTerm(
        val term: String,
        val aliases: List<String>,
        val shortDefinition: String,
        val priority: Int,
    )
}
