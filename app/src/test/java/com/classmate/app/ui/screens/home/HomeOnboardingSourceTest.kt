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
}
