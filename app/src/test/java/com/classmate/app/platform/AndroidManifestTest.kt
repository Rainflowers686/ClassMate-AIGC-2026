package com.classmate.app.platform

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidManifestTest {

    private fun manifest(): String = listOf(
        File("src/main/AndroidManifest.xml"),
        File("app/src/main/AndroidManifest.xml"),
    ).first { it.exists() }.readText()

    @Test
    fun manifestDeclaresInternetPermissionForBlueLmCloudCall() {
        assertTrue(manifest().contains("""<uses-permission android:name="android.permission.INTERNET" />"""))
    }

    @Test
    fun liveAsrStillDeclaresRecordAudio() {
        // RECORD_AUDIO remains for the experimental live ASR mode.
        assertTrue(manifest().contains("android.permission.RECORD_AUDIO"))
    }

    @Test
    fun storagePermissionsAreFunctionalAndLegacyCapped() {
        // Stage 8A-2.2 functional-first: all-files access unblocks the official on-device model dir;
        // legacy read/write storage is present but capped at API 32 (modern paths use MediaStore/SAF).
        val manifest = manifest()
        assertTrue(manifest.contains("android.permission.MANAGE_EXTERNAL_STORAGE"))
        assertTrue(cappedAt32(manifest, "READ_EXTERNAL_STORAGE"))
        assertTrue(cappedAt32(manifest, "WRITE_EXTERNAL_STORAGE"))
    }

    @Test
    fun bluetoothPermissionsAreNotRequestedWithoutARealBluetoothFeature() {
        val manifest = manifest()
        assertFalse(manifest.contains("android.permission.BLUETOOTH_CONNECT"))
        assertFalse(manifest.contains("android.permission.BLUETOOTH_ADMIN"))
        assertFalse(manifest.contains("android.permission.BLUETOOTH\""))
    }

    private fun cappedAt32(manifest: String, perm: String): Boolean =
        Regex("""<uses-permission\s+android:name="android\.permission\.$perm"\s+android:maxSdkVersion="32"\s*/>""")
            .containsMatchIn(manifest)
}
