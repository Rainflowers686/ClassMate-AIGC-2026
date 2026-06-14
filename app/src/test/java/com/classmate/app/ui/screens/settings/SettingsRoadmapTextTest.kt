package com.classmate.app.ui.screens.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRoadmapTextTest {

    @Test
    fun roadmapUsesHonestCapabilityBuckets() {
        val source = readSettingsSource()

        assertTrue(source.contains("已支持"))
        assertTrue(source.contains("实验模式"))
        assertTrue(source.contains("待接入"))
        assertTrue(source.contains("暂缓"))
        assertTrue(source.contains("云端蓝心分析"))
        assertTrue(source.contains("手动 OCR 资料流"))
        assertTrue(source.contains("系统实时转写 ASR"))
        assertTrue(source.contains("vivo ASR provider"))
        assertTrue(source.contains("vivo OCR provider"))
    }

    @Test
    fun roadmapDoesNotOverclaimFutureAudioOrOcrCapabilities() {
        val source = readSettingsSource()
        listOf(
            "真实 ASR 已完全支持",
            "真实 OCR 已完全支持",
            "声纹识别已支持",
            "已完成声纹身份识别",
            "自动爬取平台视频",
        ).forEach { phrase ->
            assertFalse(source.contains(phrase))
        }
    }

    private fun readSettingsSource(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
        ).first { it.exists() }.readText()
}
