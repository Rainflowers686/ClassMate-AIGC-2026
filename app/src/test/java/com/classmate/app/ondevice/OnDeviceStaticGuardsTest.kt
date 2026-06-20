package com.classmate.app.ondevice

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Static source guards for the reflection-only on-device bridge: no direct vivo SDK import, no legacy
 * stats-carrying onComplete signature, the AAR stays gitignored, and (Stage 8A-2.2) the manifest
 * declares the FUNCTIONAL permissions normal features need while keeping unrelated dangerous
 * permissions out.
 */
class OnDeviceStaticGuardsTest {

    // Needles are assembled from fragments so this guard file itself never contains the contiguous
    // forbidden literals — keeping naive grep-based preflight audits free of false positives.
    private val vivoImportNeedle = "import com.vivo." + "llmsdk"
    private val wrongSdkPackage = "com.blue." + "lm.sdk"
    private val legacyOnComplete = Regex("onComplete" + """\s*\([^)]*""" + "Llm" + "Stats")

    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun mainSources(): List<File> = buildList {
        addAll(ktFiles(firstExisting("src/main", "app/src/main")))
        addAll(ktFiles(firstExisting("../core/src/main", "core/src/main")))
    }

    @Test
    fun noDirectVivoLlmsdkImportInAppOrCoreSource() {
        val offenders = mainSources().filter { it.readText().contains(vivoImportNeedle) }
        assertTrue("Direct vivo SDK import found in: $offenders", offenders.isEmpty())
    }

    @Test
    fun noLegacyOnCompleteWithStatsOrWrongSdkPackage() {
        val offenders = mainSources().filter { f ->
            val t = f.readText()
            legacyOnComplete.containsMatchIn(t) || t.contains(wrongSdkPackage)
        }
        assertTrue("Forbidden legacy SDK usage in: $offenders", offenders.isEmpty())
    }

    @Test
    fun manifestDeclaresFunctionalPermissionsForOnDeviceModelAndImport() {
        val manifest = firstExisting("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml").readText()

        // Stage 8A-2.2: functional-first — these are REQUIRED for normal features.
        listOf(
            "android.permission.INTERNET",
            "android.permission.RECORD_AUDIO",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.CAMERA",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "mediatek.permission.ACCESS_APU_SYS",
        ).forEach { assertTrue("Manifest must declare $it", manifest.contains(it)) }

        // Legacy storage perms must be present AND capped at API 32.
        assertTrue("READ_EXTERNAL_STORAGE must be capped at 32", cappedAt(manifest, "READ_EXTERNAL_STORAGE", 32))
        assertTrue("WRITE_EXTERNAL_STORAGE must be capped at 32", cappedAt(manifest, "WRITE_EXTERNAL_STORAGE", 32))
        // Camera hardware declared optional so camera-less devices still install.
        assertTrue(
            "camera uses-feature must be required=false",
            Regex("""<uses-feature\s+android:name="android\.hardware\.camera"\s+android:required="false"\s*/>""")
                .containsMatchIn(manifest),
        )

        // Optional native libraries declared required=false.
        assertTrue(nativeLibOptional(manifest, "libdmabufheap.so"))
        assertTrue(nativeLibOptional(manifest, "libvcap_npu_network_v1.so"))

        // Unrelated permissions stay OUT (functional-first != everything). Bluetooth permissions
        // stay out because ClassMate has no real Bluetooth device feature.
        listOf(
            "CONTACTS", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
            "READ_SMS", "SEND_SMS", "READ_PHONE_STATE", "CALL_PHONE", "READ_CALL_LOG",
            "BODY_SENSORS", "BLUETOOTH_CONNECT", "BLUETOOTH_ADMIN", "BLUETOOTH_SCAN",
        ).forEach { assertFalse("Manifest must NOT contain $it", manifest.contains(it)) }
    }

    private fun cappedAt(manifest: String, perm: String, maxSdk: Int): Boolean =
        Regex("""<uses-permission\s+android:name="android\.permission\.$perm"\s+android:maxSdkVersion="$maxSdk"\s*/>""")
            .containsMatchIn(manifest)

    private fun nativeLibOptional(manifest: String, lib: String): Boolean =
        Regex("""<uses-native-library\s+android:name="${Regex.escape(lib)}"\s+android:required="false"\s*/>""")
            .containsMatchIn(manifest)

    @Test
    fun onDeviceSdkAarIsGitIgnored() {
        val gitignore = firstExisting("../.gitignore", ".gitignore").readText()
        assertTrue("app/libs/*.aar must stay gitignored", gitignore.contains("app/libs/*.aar"))
    }
}
