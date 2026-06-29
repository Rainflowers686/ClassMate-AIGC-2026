package com.classmate.app.ui.screens.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOnboardingSourceTest {

    @Test
    fun firstRunGuideAppearsOnlyForEmptyHistoryAndCanBeDismissed() {
        val source = readHomeSource()

        assertTrue(source.contains("三步开始"))
        assertTrue(source.contains("导入课堂资料"))
        assertTrue(source.contains("生成知识时间线"))
        assertTrue(source.contains("开始复习与练习"))
        assertTrue(source.contains("ui.history.isEmpty()"))
        assertTrue(source.contains("showFirstRunGuide"))
        assertTrue(source.contains("暂时关闭"))
    }

    @Test
    fun homeKeepsLearningCockpitAndPrimaryImportAction() {
        val source = readHomeSource()
        val product = readProductSource()

        assertTrue(source.contains("StudyCockpitCard"))
        assertTrue(source.contains("今日学习驾驶舱"))
        assertTrue(source.contains("想把哪节课变清楚？"))
        assertTrue(source.contains("整理一份新资料"))
        assertTrue(source.contains("viewModel.navigateTo(Screen.IMPORT)"))
        assertTrue(source.contains("HomeMetricStrip("))
        assertFalse(source.contains("StatStrip("))
        assertTrue(source.contains("LearningPathStep"))
        assertTrue(product.contains("color = tokens.primary"))
        assertTrue(product.contains("actionContent.copy(alpha = 0.92f)"))
        assertFalse(product.contains("cs.onPrimary.copy(alpha = 0.84f)"))
        assertFalse(product.contains("disabled", ignoreCase = true))
    }

    @Test
    fun homeMetricsUseGroupedStripInsteadOfStackedCards() {
        val source = readHomeSource()

        assertTrue(source.contains("private fun HomeMetricStrip"))
        assertTrue(source.contains("items.forEachIndexed"))
        assertTrue(source.contains(".weight(1f)"))
        assertTrue(source.contains(".clickable(onClick = click)"))
        assertTrue(source.contains("width(0.75.dp)"))
        assertFalse(source.contains("StatStrip("))
    }

    @Test
    fun homeLearningPathIsLightweightAndKeepsBottomSafePadding() {
        val source = readHomeSource()

        assertTrue(source.contains("defaultMinSize(minHeight = 48.dp)"))
        assertTrue(source.contains("size(20.dp)"))
        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains(".padding(bottom = 128.dpv())"))
        assertFalse(source.contains("isLast: Boolean"))
        assertFalse(source.contains("height(24.dp)"))
        assertFalse(source.contains("width(2.dp)"))
    }

    @Test
    fun homeDoesNotExposeDebugImportCopy() {
        val source = readHomeSource()

        assertFalse(source.contains("Debug", ignoreCase = true))
        assertFalse(source.contains("调试"))
    }

    private fun readHomeSource(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt"),
        ).first { it.exists() }.readText()

    private fun readProductSource(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/product/ProductUi.kt"),
            File("app/src/main/java/com/classmate/app/ui/product/ProductUi.kt"),
        ).first { it.exists() }.readText()
}
