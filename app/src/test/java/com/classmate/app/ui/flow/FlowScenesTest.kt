package com.classmate.app.ui.flow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Flow ambience strip uses bundled local loops. These tests pin the honesty contract so a
 * future change can't quietly imply recording, upload, or runtime generation.
 */
class FlowScenesTest {

    @Test
    fun catalogHasTheExpectedScenesWithUniqueIds() {
        assertEquals(6, FlowScenes.all.size)
        val ids = FlowScenes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        listOf("rain", "forest", "ocean", "stream", "cafe", "night_crickets").forEach {
            assertNotNull("missing scene $it", FlowScenes.byId(it))
        }
    }

    @Test
    fun disclaimerIsHonestAboutLocalPlaybackOnly() {
        val text = FlowScenes.DISCLAIMER
        assertTrue(text.contains("内置授权循环素材"))
        assertTrue(text.contains("本地播放"))
        assertTrue(text.contains("不录音"))
        assertTrue(text.contains("不上传"))
        assertTrue(text.contains("不使用实时生成"))
    }

    @Test
    fun mixerChannelsReflectPlaybackControls() {
        assertTrue(FlowScenes.mixerChannels.isNotEmpty())
        assertTrue(FlowScenes.mixerChannels.contains("音量"))
    }
}
