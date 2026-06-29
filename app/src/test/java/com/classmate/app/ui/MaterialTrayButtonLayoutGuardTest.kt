package com.classmate.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-3: 资料篮 / import buttons must never collapse to "...". The OCR-kind selector previously put 4
 * weighted buttons in one row, squeezing every label to an ellipsis on a phone. This guards the layout
 * (wrap, not 4-in-a-row) and that the key tray buttons keep real labels.
 */
class MaterialTrayButtonLayoutGuardTest {

    private val source: String =
        listOf(
            File("app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
            File("src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
        ).firstOrNull { it.exists() }?.readText(Charsets.UTF_8) ?: error("ImportCourseScreen.kt missing")

    @Test
    fun noButtonLabelIsRenderedAsAnEllipsis() {
        assertFalse(source.contains("PrimaryButton(\"...\""))
        assertFalse(source.contains("SecondaryButton(\"...\""))
        assertFalse(source.contains("text = \"...\""))
        assertFalse(source.contains("\"…\""))
    }

    @Test
    fun keyTrayButtonLabelsArePresentAndReadable() {
        listOf("加入本节课资料", "下一步：分析设置", "选择文件（只记录元数据）", "上一步").forEach {
            assertTrue("missing tray button label: $it", source.contains(it))
        }
    }

    @Test
    fun ocrKindSelectorWrapsInsteadOfFourSqueezedButtonsInOneRow() {
        // Must lay the kind buttons out 2-per-row (chunked), never all four at weight(1f) in a single Row.
        assertTrue("kind selector should wrap (chunked)", source.contains("OcrImportKind.entries.chunked("))
    }
}
