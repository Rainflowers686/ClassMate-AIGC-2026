package com.classmate.app.ui.screens.importcourse

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8C Phase C/D: the import center exposes image + photo learning-input entries (with the editable
 * draft), uses honest copy, and never presents DeepSeek/Compatible as a competition main path.
 */
class ImportCourseEntriesTest {

    private fun source(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
        ).first { it.exists() }.readText()

    @Test
    fun importCenterExposesTextImageAndPhotoEntries() {
        val s = source()
        assertTrue(s.contains("文本粘贴"))
        // Stage 8E wording: honest learning-input entries + user-confirmed multimodal draft.
        assertTrue(s.contains("图片学习输入"))
        assertTrue(s.contains("拍照学习输入"))
        assertTrue(s.contains("端侧多模态理解草稿"))
        assertTrue(s.contains("用户确认后进入学习资料"))
        // Honest: image input is a draft, not OCR.
        assertTrue(s.contains("不替代 OCR"))
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
