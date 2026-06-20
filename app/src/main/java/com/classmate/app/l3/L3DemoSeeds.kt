package com.classmate.app.l3

object L3DemoSeeds {
    const val lessonTitle: String = "L3 演示课：电磁感应"

    val lessonText: String = """
        电磁感应描述磁通量变化时闭合回路中产生感应电动势的现象。法拉第定律指出，感应电动势的大小与磁通量变化率成正比。
        
        楞次定律说明感应电流的方向总是阻碍引起它的磁通量变化，因此可以用能量守恒来理解方向判断。
        
        复习时要把磁通量、变化率、回路方向和能量守恒放在同一个问题里分析，不能只背公式。
    """.trimIndent()

    val questionBankMarkdown: String = """
        Q: 法拉第定律主要描述什么关系？
        A. 感应电动势与磁通量变化率的关系
        B. 电阻与温度的关系
        C. 电荷量与时间的关系
        D. 光强与距离的关系
        Answer: A
        Explanation: 法拉第定律说明感应电动势大小与磁通量变化率成正比。
        
        Q: 楞次定律用于判断什么？
        A. 电流热效应
        B. 感应电流方向
        C. 电阻大小
        D. 电容充放电速度
        Answer: B
        Explanation: 楞次定律用于判断感应电流方向，并体现阻碍磁通量变化。
        
        Q: 做电磁感应题时，哪一种做法更可靠？
        A. 只背公式
        B. 同时分析磁通量变化、方向和能量守恒
        C. 忽略回路方向
        D. 不看题目条件
        Answer: B
        Explanation: 电磁感应题需要把公式、方向判断和物理意义结合起来。
    """.trimIndent()
}
