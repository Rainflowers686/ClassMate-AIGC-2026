package com.classmate.app.practice

import com.classmate.core.practice.PracticeSearchEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSearchLauncherTest {

    // The launcher hands a public results URL to the system browser via ACTION_VIEW. Per the repo's
    // unit-test convention (cf. ExportIntentFactoryTest) we assert the pure target, not a real Intent
    // (android.content.Intent/Uri are not available in plain JVM unit tests).
    @Test
    fun searchTargetIsExternalHttpsResultsPage() {
        val link = PracticeSearchEngine.primaryLink("C++", "const 引用")
        assertTrue(link.url.startsWith("https://"))
        assertFalse(link.url.contains("localhost", ignoreCase = true))
        assertFalse(link.url.contains("127.0.0.1"))
    }

    @Test
    fun clipboardFallbackUsesSafeQueryOnly() {
        val link = PracticeSearchEngine.primaryLink("大学物理 appKey", "楞次定律 prompt")
        val text = PracticeSearchLauncher.clipboardText(link)

        assertTrue(text.contains("楞次定律"))
        listOf("Authorization", "Bearer", "appKey", "apiKey", "prompt", "messages").forEach {
            assertFalse(text.contains(it, ignoreCase = true))
        }
    }
}
