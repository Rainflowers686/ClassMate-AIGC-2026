package com.classmate.app.platform

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidManifestTest {

    @Test
    fun manifestDeclaresInternetPermissionForBlueLmCloudCall() {
        val manifest = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).first { it.exists() }.readText()

        assertTrue(manifest.contains("""<uses-permission android:name="android.permission.INTERNET" />"""))
    }
}
