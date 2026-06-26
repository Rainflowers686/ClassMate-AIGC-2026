package com.classmate.app.qa

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Project-owned docs must carry a real Chinese explanation of the CURRENT honest state (third-party
 * official raw docs may stay in their original language and are NOT checked here). Also bans
 * over-claims and secret leakage, and keeps README ↔ the 18-capability readiness doc consistent.
 */
class ProjectDocsLocalizationGuardTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing $rel")

    private val cjk = Regex("[\\u4e00-\\u9fa5]")
    private fun cjkCount(s: String): Int = cjk.findAll(s).count()

    /** Project-owned docs that must have a Chinese version/section (the team's explanation/status docs). */
    private val projectDocs = listOf(
        "README.md",
        "docs/current/project_current_status_v1_9.md",
        "docs/current/official_tool_matrix.md",
        "docs/current/official_tool_productization_matrix.md",
        "docs/current/official_18_capability_l3_readiness.md",
        "docs/current/cloud_real_device_test_plan_v1.md",
        "docs/current/cloud_device_debug_playbook_v1.md",
        "docs/current/cloud_edge_model_collaboration_v1.md",
        "docs/current/audio_asr_dialect_enhancement_v1.md",
        "docs/current/learning_study_pack_export_v1.md",
        "docs/current/bluelm_cloud_realdevice_troubleshooting.md",
        "docs/current/claude_v3_review_handoff.md",
        "docs/current/p0_claude_engineering_batch_pack.md",
        "docs/current/p2_ai_learning_experience_acceptance_run.md",
    )

    private val overClaimBans = listOf(
        "18 项全部真机跑通", "18 项全部真机", "全部真机跑通", "所有能力真机跑通", "18 项全部完成",
        "all 18 abilities verified on device", "all capabilities verified on real device",
    )

    @Test
    fun everyProjectDocHasChineseExplanation() {
        val weak = projectDocs.filter { cjkCount(read(it)) < 100 }
        assertTrue("These project docs still lack a Chinese explanation (>=100 CJK chars): $weak", weak.isEmpty())
    }

    @Test
    fun projectDocsDoNotOverclaim() {
        projectDocs.forEach { path ->
            val text = read(path)
            overClaimBans.forEach { banned ->
                assertFalse("$path over-claims: $banned", text.contains(banned))
            }
        }
    }

    @Test
    fun projectDocsNeverLeakSecrets() {
        projectDocs.forEach { path ->
            val text = read(path)
            assertFalse("$path leaks Authorization", text.contains("Authorization:"))
            assertFalse("$path leaks AppKey", text.contains("AppKey="))
            assertFalse("$path leaks bearer token", text.contains("Bearer "))
        }
    }

    @Test
    fun readmeAndReadinessDocAgreeOnHonestStatus() {
        val readme = read("README.md")
        val readiness = read("docs/current/official_18_capability_l3_readiness.md")
        // Both lead with the same honest framing.
        listOf(readme, readiness).forEach { doc ->
            assertTrue("doc must state the auto local-rule fallback", doc.contains("本地基础整理"))
            assertTrue("doc must state experimental default-off", doc.contains("默认关闭"))
        }
        assertTrue("README should point to the readiness doc", readme.contains("official_18_capability_l3_readiness.md"))
    }
}
