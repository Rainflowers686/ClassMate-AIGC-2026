package com.classmate.app.ui.screens.review

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPracticePanelTextTest {

    @Test
    fun reviewPracticePanelUsesChineseNeedMorePracticeCopy() {
        val sourceFile = listOf(
            File("src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt"),
        ).first { it.exists() }
        val source = sourceFile.readText()

        assertTrue(source.contains("需要多练"))
        assertTrue(source.contains("找练习"))
        assertTrue(source.contains("复制搜索词"))
        assertTrue(source.contains("打开外部搜索"))
        assertFalse(source.contains("需要例题"))
        assertFalse(source.contains("Need more examples"))
        assertFalse(source.contains("Find practice"))
        assertFalse(source.contains("Search query"))
        assertFalse(source.contains("Open external search"))
    }
}
