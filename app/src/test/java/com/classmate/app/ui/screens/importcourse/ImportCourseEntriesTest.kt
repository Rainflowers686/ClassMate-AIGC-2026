package com.classmate.app.ui.screens.importcourse

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The import center exposes text, image, photo, OCR, and transcript entries with current product
 * wording, and never presents external-provider diagnostics as the ordinary user path.
 */
class ImportCourseEntriesTest {

    private fun source(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
        ).first { it.exists() }.readText()

    private fun strings(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/i18n/Strings.kt"),
            File("app/src/main/java/com/classmate/app/ui/i18n/Strings.kt"),
        ).first { it.exists() }.readText()

    @Test
    fun importCenterExposesTextImageAndPhotoEntries() {
        val s = source()
        val strings = strings()
        assertTrue(s.contains("文本粘贴"))
        assertTrue(s.contains("图片学习输入"))
        assertTrue(s.contains("拍照学习输入"))
        assertTrue(s.contains("官方 OCR 按配置启用"))
        assertTrue(s.contains("用户确认后进入知识结构大纲"))
        // Honest: image input is a draft, not OCR.
        assertTrue(s.contains("不替代 OCR") || strings.contains("不替代 OCR"))
        assertFalse(s.contains("自动 OCR 完成"))
        assertFalse(s.contains("多模态替代 OCR"))
    }

    @Test
    fun importCenterDoesNotClaimExternalModelMainPath() {
        val s = source()
        assertFalse(s.contains("DeepSeek"))
        assertFalse(s.contains("Compatible Demo"))
    }
}
