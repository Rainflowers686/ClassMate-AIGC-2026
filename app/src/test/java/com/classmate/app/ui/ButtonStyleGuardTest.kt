package com.classmate.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the restrained, consistent button SYSTEM (not a theme rewrite): a small set of semantic
 * buttons that all share height / radius / single-line text / disabled handling, so the visual
 * hierarchy is clear (primary stands out, secondary is quiet, destructive is careful, experimental is
 * tagged) and long Chinese/English labels never break the layout.
 */
class ButtonStyleGuardTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText()
            ?: error("missing $rel")

    private val source by lazy { read("app/src/main/java/com/classmate/app/ui/components/CommonUi.kt") }

    private val semanticButtons = listOf(
        "fun PrimaryButton(",
        "fun SecondaryButton(",
        "fun TertiaryButton(",
        "fun DestructiveButton(",
        "fun ExperimentalButton(",
    )

    @Test
    fun theSemanticButtonSetExists() {
        semanticButtons.forEach { fn ->
            assertTrue("missing semantic button: $fn", source.contains(fn))
        }
    }

    @Test
    fun allButtonsShareHeightRadiusAndSingleLineText() {
        // One occurrence per button (5): consistent tap target, radius, and overflow-safe label.
        assertTrue("buttons must share 52dp height", source.split("height(52.dp)").size - 1 >= semanticButtons.size)
        assertTrue(
            "buttons must share the theme button radius",
            source.split("RoundedCornerShape(ClassMateTheme.shapes.buttonRadius)").size - 1 >= semanticButtons.size,
        )
        assertTrue(
            "buttons must use single-line text so long zh/en labels do not break",
            source.split("ClassMateSingleLineText(text").size - 1 >= semanticButtons.size,
        )
    }

    @Test
    fun everyButtonHasDisabledHandling() {
        // Each composable takes `enabled` so disabled buttons are not clickable.
        assertTrue("every button needs an enabled param", source.split("enabled: Boolean = true").size - 1 >= semanticButtons.size)
    }

    @Test
    fun destructiveIsCarefulNotAScreamingRed() {
        // Destructive uses the muted errorContainer (careful), never a raw primary-weight red fill.
        val destructiveBlock = source.substringAfter("fun DestructiveButton(").substringBefore("fun ExperimentalButton(")
        assertTrue("destructive should use errorContainer (careful tone)", destructiveBlock.contains("errorContainer"))
    }

    @Test
    fun experimentalIsTaggedAndNotPrimaryWeight() {
        val expBlock = source.substringAfter("fun ExperimentalButton(").substringBefore("fun GradientHeroPanel(")
        assertTrue("experimental should carry a badge", expBlock.contains("badge"))
        assertTrue("experimental should use the warning accent, not primary", expBlock.contains("warning"))
    }
}
