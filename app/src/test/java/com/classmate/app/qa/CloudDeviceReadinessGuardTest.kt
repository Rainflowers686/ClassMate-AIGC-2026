package com.classmate.app.qa

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudDeviceReadinessGuardTest {
    @Test
    fun cloudDevicePrecheckScriptBuildsSafelyWithoutReadingLocalConfigOrRunningSmoke() {
        val script = firstExisting(
            "scripts/qa/cloud_device_precheck.ps1",
            "../scripts/qa/cloud_device_precheck.ps1",
        ).readText(Charsets.UTF_8)

        listOf(
            "git diff --check",
            ":app:testDebugUnitTest",
            ":app:assembleDebug",
            "scripts\\secrets_scan\\secrets_scan.ps1",
            "app\\build\\outputs\\apk\\debug\\app-debug.apk",
            "config.local.json exists; content was not read.",
            "CLOUD DEVICE PRECHECK PASS",
        ).forEach { marker ->
            assertTrue("Missing cloud precheck marker: $marker", script.contains(marker))
        }

        assertFalse(script.contains("official_provider_smoke"))
        assertFalse(script.contains("Get-Content"))
        assertFalse(script.contains("Authorization", ignoreCase = true))
        assertFalse(script.contains("AppKey", ignoreCase = true))
        assertFalse(script.contains("adb install", ignoreCase = true))
    }

    @Test
    fun cloudRealDevicePlanCoversAllStageFiveUserPaths() {
        val plan = firstExisting(
            "docs/current/cloud_real_device_test_plan_v1.md",
            "../docs/current/cloud_real_device_test_plan_v1.md",
        ).readText(Charsets.UTF_8)

        listOf(
            "Text / Markdown Learning Loop",
            "Image / OCR Learning Loop",
            "Document / PDF Page Text Learning Loop",
            "Recording / Transcript Learning Loop",
            "WrongBook Retry Loop",
            "LearningDiagnosis Loop",
            "EvidenceDetail",
            "app\\build\\outputs\\apk\\debug\\app-debug.apk",
            "GO/NO-GO",
        ).forEach { marker ->
            assertTrue("Missing cloud device plan marker: $marker", plan.contains(marker))
        }

        assertFalse(plan.contains("automatic ASR completed", ignoreCase = true))
        assertFalse(plan.contains("PDF full native parsing complete", ignoreCase = true))
    }

    @Test
    fun testAssetsAndDebugPlaybookExistAndAvoidBinaryOrSecretRequirements() {
        val assets = firstExisting(
            "docs/current/test_assets_manifest_v1.md",
            "../docs/current/test_assets_manifest_v1.md",
        ).readText(Charsets.UTF_8)
        val playbook = firstExisting(
            "docs/current/cloud_device_debug_playbook_v1.md",
            "../docs/current/cloud_device_debug_playbook_v1.md",
        ).readText(Charsets.UTF_8)

        listOf(
            "Classroom Text",
            "Markdown Document",
            "Lesson Image",
            "PDF Page Text Sample",
            "Recording / Manual Transcript",
            "Deliberate Wrong Answer Path",
        ).forEach { marker ->
            assertTrue("Missing test asset marker: $marker", assets.contains(marker))
        }

        listOf(
            "Image / OCR Fails",
            "Document / PDF Import Fails",
            "Recording / Transcript Fails",
            "Practice / WrongBook / Review Does Not Update",
            "EvidenceDetail Cannot Open",
            "APK Install / Permission Failure",
        ).forEach { marker ->
            assertTrue("Missing debug playbook marker: $marker", playbook.contains(marker))
        }

        val combined = assets + "\n" + playbook
        assertTrue(combined.contains("Do not commit binary assets"))
        assertTrue(combined.contains("Do not include"))
        assertFalse(combined.contains("config.local.json content"))
        assertFalse(combined.contains("Authorization header value", ignoreCase = true))
    }

    @Test
    fun stageThreeAndFourUiExposeDeviceReachableFallbackTextAndEvidenceActions() {
        val importScreen = firstExisting(
            "app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt",
            "../app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt",
        ).readText(Charsets.UTF_8)
        val evidenceScreen = firstExisting(
            "app/src/main/java/com/classmate/app/ui/screens/evidence/EvidenceDetailScreen.kt",
            "../app/src/main/java/com/classmate/app/ui/screens/evidence/EvidenceDetailScreen.kt",
        ).readText(Charsets.UTF_8)
        val reviewScreen = firstExisting(
            "app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt",
            "../app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt",
        ).readText(Charsets.UTF_8)
        val practiceScreen = firstExisting(
            "app/src/main/java/com/classmate/app/ui/screens/practice/PracticeSessionScreen.kt",
            "../app/src/main/java/com/classmate/app/ui/screens/practice/PracticeSessionScreen.kt",
        ).readText(Charsets.UTF_8)

        listOf(
            "图片已保存为 evidence asset",
            "添加该页文本并生成学习闭环",
            "确认转写",
        ).forEach { marker -> assertTrue("Import UI marker missing: $marker", importScreen.contains(marker)) }

        listOf(
            "图片预览暂不可用",
            "当前保留转写证据，播放定位待真机验证",
            "证据资产缺失，但保留文本证据",
        ).forEach { marker -> assertTrue("Evidence UI marker missing: $marker", evidenceScreen.contains(marker)) }

        listOf(
            "学习诊断",
            "查看证据",
            "重练这题",
            "复习相关知识点",
        ).forEach { marker -> assertTrue("Review UI marker missing: $marker", reviewScreen.contains(marker)) }

        listOf(
            "本题考点",
            "错题状态",
            "掌握状态",
            "查看来源证据",
        ).forEach { marker -> assertTrue("Practice UI marker missing: $marker", practiceScreen.contains(marker)) }
    }

    private fun firstExisting(vararg candidates: String): File =
        candidates.map(::File).firstOrNull { it.exists() } ?: File(candidates.first())
}
