package com.classmate.app.qa

import com.classmate.core.official.CapabilityReadinessRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps docs/current/official_18_capability_l3_readiness.md honest and in sync with the code registry:
 * it must render every golden-standard item, mark experimental capabilities off-by-default, document the
 * auto local-rule fallback, NOT over-claim "all 18 verified on device", and never leak a secret.
 */
class CapabilityReadinessDocTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing $rel")

    private val doc by lazy { read("docs/current/official_18_capability_l3_readiness.md") }

    @Test
    fun docRendersEveryGoldenStandardItem() {
        CapabilityReadinessRegistry.goldenStandard.forEach { item ->
            assertTrue("readiness doc missing golden-standard item: $item", doc.contains(item))
        }
    }

    @Test
    fun docDocumentsHonestBoundaries() {
        assertTrue(doc.contains("默认关闭"))
        assertTrue(doc.contains("本地基础整理"))
        assertTrue(doc.contains("仍需真机") || doc.contains("待真机抽测"))
        assertTrue("must keep experimental + fallback honesty", doc.contains("不伪造") || doc.contains("不伪装"))
    }

    @Test
    fun docDoesNotOverclaimAllAbilitiesDeviceVerified() {
        listOf(
            "18 项全部真机跑通",
            "18 项全部真机",
            "全部真机跑通",
            "所有能力真机跑通",
            "18 项全部完成",
        ).forEach { banned ->
            assertFalse("readiness doc must not over-claim: $banned", doc.contains(banned))
        }
    }

    @Test
    fun docNeverLeaksASecret() {
        // No raw credential material in the doc.
        assertFalse(doc.contains("Authorization:"))
        assertFalse(doc.contains("AppKey="))
        assertFalse(doc.contains("Bearer "))
    }
}
