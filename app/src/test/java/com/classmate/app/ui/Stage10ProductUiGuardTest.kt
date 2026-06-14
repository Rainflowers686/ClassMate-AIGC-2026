package com.classmate.app.ui

import com.classmate.app.ui.theme.ThemeOption
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 10 guard: the P0 surfaces were rebuilt on a NEW product layout language (tinted canvas +
 * big-title hero + grouped-inset rows + one dominant command + folded secondary), not the rejected
 * 9A/9B/9C card stacks. This proves, structurally:
 *   1. the product layer (ProductUi.kt) exists with its core building blocks;
 *   2. every rebuilt P0 page is wrapped in ProductCanvas + ProductScaffold + ProductHero;
 *   3. the old surgery / premium skeletons (CommandHero, PrimaryActionDock, SubtleBackgroundLayer,
 *      ExpandableSection, PageHero, ClassMatePageScaffold) no longer appear on the rebuilt pages;
 *   4. the honest 云端蓝心 / 端侧蓝心 / 安全占位 vocabulary still holds and no over-claim copy leaks.
 */
class Stage10ProductUiGuardTest {

    private fun firstExisting(vararg c: String): File =
        c.map { File(it) }.firstOrNull { it.exists() } ?: File(c.first())

    private fun read(rel: String): String = firstExisting("src/main/$rel", "app/src/main/$rel").readText()

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun mainSources(): List<File> = buildList {
        addAll(ktFiles(firstExisting("src/main", "app/src/main")))
        addAll(ktFiles(firstExisting("../core/src/main", "core/src/main")))
    }

    private fun screen(name: String): String = read("java/com/classmate/app/ui/screens/$name")

    // The P0 pages rebuilt on the product layout language. Sub-pages keep a titled scaffold but
    // share the product card language (QuietCard); the tab roots fully adopt canvas + hero.
    private val canvasHeroPages = listOf(
        "home/HomeScreen.kt",
        "importcourse/ImportCourseScreen.kt",
        "course/CourseDetailScreen.kt",
        "history/HistoryScreen.kt",
        "settings/SettingsScreen.kt",
        "review/ReviewPlanScreen.kt",
    )

    // ---- the product layer exists ---------------------------------------------------------------

    @Test
    fun productLayerExistsWithCoreBuildingBlocks() {
        val s = read("java/com/classmate/app/ui/product/ProductUi.kt")
        listOf(
            "fun ProductCanvas", "fun ProductScaffold", "fun ProductHero", "fun ProductSectionTitle",
            "fun PrimaryCommand", "fun QuietCard", "fun GroupedList", "data class ProductRow",
            "fun StatStrip", "fun ProviderPathStrip", "fun ProductCollapse", "fun Modifier.productPress",
        ).forEach { assertTrue("missing product building block: $it", s.contains(it)) }
    }

    // ---- every rebuilt P0 page uses the new layout language --------------------------------------

    @Test
    fun rebuiltPagesUseCanvasScaffoldAndHero() {
        canvasHeroPages.forEach { rel ->
            val s = screen(rel)
            assertTrue("$rel missing ProductCanvas", s.contains("ProductCanvas"))
            assertTrue("$rel missing ProductScaffold", s.contains("ProductScaffold"))
            assertTrue("$rel missing ProductHero", s.contains("ProductHero"))
        }
    }

    @Test
    fun homeHasOneDominantCommandAndGroupedInputDeck() {
        val s = screen("home/HomeScreen.kt")
        assertTrue(s.contains("PrimaryCommand"))   // single dominant CTA, not equal-weight entries
        assertTrue(s.contains("GroupedList"))       // grouped-inset quick-input rows, not stacked cards
        assertTrue(s.contains("StatStrip"))         // lightweight stat band
        assertTrue(s.contains("ProviderPathStrip")) // honest provider path, not a paragraph
    }

    @Test
    fun importIsADraftWorkspaceWithGroupedRowsAndFoldedSecondary() {
        val s = screen("importcourse/ImportCourseScreen.kt")
        assertTrue(s.contains("GroupedList"))      // primary inputs as grouped rows
        assertTrue(s.contains("ProductCollapse"))  // 更多导入方式 folded
        assertTrue(s.contains("ImageDraftCard"))   // 8E multimodal draft preserved
    }

    @Test
    fun courseDetailLeadsWithVisualMapAndActionDock() {
        val s = screen("course/CourseDetailScreen.kt")
        assertTrue(s.contains("KnowledgePathNode")) // connected node path (visual learning map)
        assertTrue(s.contains("LearningActionDock")) // segmented action dock, not a button pile
        assertTrue(s.contains("ProductCollapse"))    // 课堂记录 / 资料来源 folded
        assertTrue(s.contains("SourceBadge"))
    }

    // ---- the rejected skeletons are gone from the rebuilt pages ----------------------------------

    @Test
    fun oldSkeletonsAreAbsentFromRebuiltPages() {
        val forbiddenStructure = listOf(
            "CommandHero", "PrimaryActionDock", "SourceStatusStrip", "ImportMethodTile",
            "SubtleBackgroundLayer", "ExpandableSection", "PageHero(", "ClassMatePageScaffold",
            "InputMethodCard", "ProviderPathPill",
        )
        canvasHeroPages.forEach { rel ->
            val s = screen(rel)
            forbiddenStructure.forEach { skeleton ->
                assertTrue("$rel still references old skeleton: $skeleton", !s.contains(skeleton))
            }
        }
    }

    // ---- honest positioning + vocabulary --------------------------------------------------------

    @Test
    fun focusStaysDefaultThemeAfterRebuild() {
        assertEquals(ThemeOption.FOCUS, ThemeOption.Default)
        assertEquals(3, ThemeOption.entries.size)
    }

    @Test
    fun honestProviderVocabularyHoldsOnRebuiltPages() {
        // Home states the full honest path; Course/Review surface 端侧蓝心 without raw provider ids.
        assertTrue(screen("home/HomeScreen.kt").contains("云端蓝心 → 端侧蓝心 → 安全占位"))
        assertTrue(screen("course/CourseDetailScreen.kt").contains("端侧蓝心"))
    }

    @Test
    fun noForbiddenOverClaimCopyInMainSource() {
        val forbidden = listOf(
            Regex("LocalRule 可用"),
            Regex("本地规则兜底"),
            Regex("规则智能"),
            Regex("本地规则分析"),
            Regex("""LocalRule.*智能"""),
            Regex("""LocalRule.*兜底"""),
            Regex("多模态替代 ?OCR"),
            Regex("自动 ?OCR 完成"),
            Regex("DeepSeek 复赛主路径"),
            Regex("Compatible 复赛主路径"),
        )
        val offenders = mainSources().filter { f -> forbidden.any { it.containsMatchIn(f.readText()) } }
        assertTrue("Forbidden/over-claim copy remains in: $offenders", offenders.isEmpty())
    }
}
