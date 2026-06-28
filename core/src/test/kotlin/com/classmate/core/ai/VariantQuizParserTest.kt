package com.classmate.core.ai

import com.classmate.core.practice.isAnswerableQuiz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-4: the model's variant-quiz JSON must become real, answerable practice items — answers normalized,
 * bad questions filtered, source preserved, and never crash on malformed input.
 */
class VariantQuizParserTest {

    private val resolver: (String) -> String = { title -> if (title.isBlank()) "kp_default" else "kp_${title.hashCode()}" }

    @Test
    fun parsesFencedJsonIntoAnswerableItemsWithNormalizedAnswers() {
        val raw = """
            这是为你生成的题目：
            ```json
            {"questions":[
              {"stem":"F=ma 中 a 表示？","type":"single_choice",
               "options":[{"id":"A","text":"加速度"},{"id":"B","text":"质量"}],
               "answer":"A","explanation":"a 是加速度","knowledgePointTitle":"牛顿第二定律","difficulty":"basic"},
              {"stem":"力越大加速度越大（同质量）","type":"true_false",
               "options":[{"id":"A","text":"正确"},{"id":"B","text":"错误"}],
               "answer":"正确","explanation":"同质量下成正比","knowledgePointTitle":"牛顿第二定律"}
            ]}
            ```
        """.trimIndent()
        val items = VariantQuizParser.parse(raw, AiExecutionSource.CLOUD, "var_c1_1", resolver)
        assertEquals(2, items.size)
        assertTrue(items.all { it.isAnswerableQuiz() })
        // "A" resolves to the 加速度 option.
        assertEquals("加速度", items[0].options.first { it.correct }.text)
        // "正确" resolves to the 正确 option.
        assertEquals("正确", items[1].options.first { it.correct }.text)
        // Source + KP binding preserved.
        assertEquals(AiExecutionSource.CLOUD, items[0].source)
        assertEquals("kp_${"牛顿第二定律".hashCode()}", items[0].knowledgePointId)
    }

    @Test
    fun malformedJsonYieldsEmptyListNotCrash() {
        assertTrue(VariantQuizParser.parse("抱歉我无法生成", AiExecutionSource.CLOUD, "x", resolver).isEmpty())
        assertTrue(VariantQuizParser.parse("{not valid json", AiExecutionSource.CLOUD, "x", resolver).isEmpty())
        assertTrue(VariantQuizParser.parse("", AiExecutionSource.CLOUD, "x", resolver).isEmpty())
    }

    @Test
    fun questionsWithoutCorrectAnswerOrEnoughOptionsAreFiltered() {
        val raw = """
            {"questions":[
              {"stem":"无答案题","options":[{"id":"A","text":"x"},{"id":"B","text":"y"}],"answer":"","knowledgePointTitle":"K"},
              {"stem":"单选项题","options":[{"id":"A","text":"only"}],"answer":"A","knowledgePointTitle":"K"},
              {"stem":"好题","options":[{"id":"A","text":"对"},{"id":"B","text":"错"}],"answer":"A","knowledgePointTitle":"K"}
            ]}
        """.trimIndent()
        val items = VariantQuizParser.parse(raw, AiExecutionSource.ON_DEVICE, "p", resolver)
        assertEquals(1, items.size)
        assertEquals("好题", items[0].question)
        assertEquals(AiExecutionSource.ON_DEVICE, items[0].source)
    }
}
