package com.classmate.app.exporting

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportProviderConfigTest {
    @Test
    fun manifestContainsFileProviderAndLegacyCappedStorage() {
        val manifest = readText("src/main/AndroidManifest.xml")

        // The modern export path is FileProvider + MediaStore/SAF (still present).
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("\${applicationId}.fileprovider"))
        // Stage 8A-2.2: legacy storage perms exist for compatibility but are capped at API 32.
        assertTrue(
            Regex("""<uses-permission\s+android:name="android\.permission\.WRITE_EXTERNAL_STORAGE"\s+android:maxSdkVersion="32"\s*/>""")
                .containsMatchIn(manifest),
        )
    }

    @Test
    fun fileProviderPathsExposeOnlyExportShareCache() {
        val paths = readText("src/main/res/xml/classmate_file_paths.xml")
        val pathAttributes = Regex("path=\"([^\"]*)\"")
            .findAll(paths)
            .joinToString("\n") { it.groupValues[1] }

        assertTrue(paths.contains("export_share/"))
        listOf("config", "local.properties", "secret", "build/", "apk/", "aab/", ".gradle").forEach {
            assertFalse(pathAttributes.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun qwenThinkingGuardIsStillPresent() {
        val vendor = readText("../core/src/main/kotlin/com/classmate/core/provider/VendorIo.kt")
        val diagnostic = readText("../core/src/main/kotlin/com/classmate/core/provider/BlueLMDiagnostic.kt")

        assertTrue(vendor.contains("enable_thinking"))
        assertTrue(vendor.contains("qwen3.5-plus"))
        assertTrue(diagnostic.contains("enable_thinking"))
    }

    private fun readText(path: String): String {
        val direct = Paths.get(path)
        val fromRoot = Paths.get("app").resolve(path)
        val resolved: Path = when {
            Files.exists(direct) -> direct
            Files.exists(fromRoot) -> fromRoot
            else -> direct
        }
        return String(Files.readAllBytes(resolved), Charsets.UTF_8)
    }
}
