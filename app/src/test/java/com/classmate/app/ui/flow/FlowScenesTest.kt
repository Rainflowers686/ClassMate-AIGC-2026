package com.classmate.app.ui.flow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Flow ambience strip is a visual preview only. These tests pin the honesty contract so a
 * future change can't quietly start implying real audio is recorded or played.
 */
class FlowScenesTest {

    @Test
    fun catalogHasTheExpectedScenesWithUniqueIds() {
        assertEquals(5, FlowScenes.all.size)
        val ids = FlowScenes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        listOf("rain_meadow", "window_rain", "night_desk", "white_noise", "morning_cafe").forEach {
            assertNotNull("missing scene $it", FlowScenes.byId(it))
        }
    }

    @Test
    fun disclaimerIsHonestAboutNoRealAudio() {
        val text = FlowScenes.DISCLAIMER
        // Must say it is a visual preview and that nothing is recorded/played.
        assertTrue(text.contains("视觉预览"))
        assertTrue(text.contains("当前不包含真实音频资源"))
        assertTrue(text.contains("不会"))
        // Must NOT claim audio is actually playing or being recorded for real.
        assertFalse(text.contains("正在播放"))
        assertFalse(text.contains("已接入"))
    }

    @Test
    fun mixerChannelsArePresentButLabelledAsPlaceholders() {
        assertTrue(FlowScenes.mixerChannels.isNotEmpty())
    }
}
