package com.classmate.app.exporting

import com.classmate.app.data.ExportActionStatus
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Stage 5B save-failure fallback: the Downloads path must use scoped MediaStore and
 * therefore add NO dangerous storage permission, and the export status set must expose the new
 * SAVED_DOWNLOADS / INTERNAL_ONLY states.
 */
class ExportDownloadsFallbackTest {

    @Test
    fun statusEnumExposesDownloadsAndInternalOnly() {
        val names = ExportActionStatus.entries.map { it.name }.toSet()
        assertTrue("SAVED_DOWNLOADS" in names)
        assertTrue("INTERNAL_ONLY" in names)
        assertTrue("SAVED_AS" in names)
        assertTrue("SHARED" in names)
    }

    @Test
    fun downloadsExporterUsesScopedMediaStoreWithoutDangerousPermissions() {
        val source = readText("src/main/java/com/classmate/app/exporting/DownloadsExporter.kt")
        assertTrue(source.contains("MediaStore.Downloads"))
        assertTrue(source.contains("Build.VERSION.SDK_INT"))
        assertFalse(source.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse(source.contains("MANAGE_EXTERNAL_STORAGE"))
    }

    @Test
    fun manifestStoragePermissionsAreLegacyCappedNotTheExportPath() {
        val manifest = readText("src/main/AndroidManifest.xml")
        // Stage 8A-2.2: legacy storage write is present but capped at API 32; the modern export path
        // still uses scoped MediaStore Downloads / SAF / share and does NOT depend on it.
        assertTrue(
            Regex("""<uses-permission\s+android:name="android\.permission\.WRITE_EXTERNAL_STORAGE"\s+android:maxSdkVersion="32"\s*/>""")
                .containsMatchIn(manifest),
        )
    }

    @Test
    fun internalBackupTargetsAppPrivateFilesNotFileProvider() {
        val source = readText("src/main/java/com/classmate/app/exporting/ExportIntentFactory.kt")
        assertTrue(source.contains("filesDir"))
        assertTrue(source.contains("exports"))
    }

    private fun readText(path: String): String {
        val direct = Paths.get(path)
        val fromRoot = Paths.get("app").resolve(path)
        val resolved = if (Files.exists(direct)) direct else fromRoot
        return String(Files.readAllBytes(resolved), Charsets.UTF_8)
    }
}
