package com.classmate.core.capture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Capture guards (task §10.D): honest user-visible wording, no hardcoded/credential leaks in the capture
 * providers, the on-device qwen thinking guard still holds, and .codex_work / AAR stay out of git.
 */
class CaptureGuardTest {

    private fun firstExisting(vararg c: String): File = c.map { File(it) }.firstOrNull { it.exists() } ?: File(c.first())

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun coreMain(): File = firstExisting("src/main/kotlin", "core/src/main/kotlin")
    private fun appMain(): File = firstExisting("../app/src/main/java", "app/src/main/java")
    private fun captureDir(): File = firstExisting("src/main/kotlin/com/classmate/core/capture", "core/src/main/kotlin/com/classmate/core/capture")

    private fun mainSources(): List<File> = ktFiles(coreMain()) + ktFiles(appMain())

    @Test fun noForbiddenUserVisibleWording() {
        val forbidden = listOf(
            "本地规则兜底", "LocalRule 可用", "LocalRule 智能", "LocalRule 兜底", "本地规则分析",
            "端侧结果 LOCAL_FALLBACK", "多模态替代 OCR", "多模态替代OCR", "自动 OCR 完成", "自动OCR完成",
            "DeepSeek 复赛主路径", "Compatible 复赛主路径", "已完成实时 ASR", "已完成实时ASR",
            "自动听课", "替代听脑",
        )
        val offenders = mainSources().filter { f -> forbidden.any { f.readText().contains(it) } }
        assertTrue("Forbidden wording in: $offenders", offenders.isEmpty())
    }

    @Test fun captureProvidersDoNotHardcodeCredentials() {
        // No real app id / app key literal, and no Bearer token literal (only interpolated "Bearer $...").
        val keyLiteral = Regex("""(appKey|app_key|AppKey|appId|app_id|AppId)\s*[=:]\s*"[^"$\n]{8,}"""")
        val bearerLiteral = Regex("""Bearer\s+[A-Za-z0-9._-]{8,}""")
        val offenders = ktFiles(captureDir()).filter { f ->
            val t = f.readText()
            keyLiteral.containsMatchIn(t) || bearerLiteral.containsMatchIn(t)
        }
        assertTrue("Capture provider appears to hardcode a credential: $offenders", offenders.isEmpty())
    }

    @Test fun qwenThinkingGuardStillHolds() {
        val hit = ktFiles(coreMain()).any { it.readText().contains("enable_thinking") }
        assertTrue("qwen enable_thinking guard must remain in core source", hit)
    }

    @Test fun codexWorkAndAarStayGitIgnored() {
        val gitignore = firstExisting("../.gitignore", ".gitignore").readText()
        assertTrue(".codex_work must be gitignored", gitignore.contains(".codex_work"))
        assertTrue("AAR must be gitignored", gitignore.contains("app/libs/*.aar"))
    }
}
