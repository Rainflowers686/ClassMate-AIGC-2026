package com.classmate.app.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecognitionDiagnosticsTest {

    @Test
    fun readyOnlyWhenPermissionAndRecognizer() {
        assertTrue(SpeechRecognitionReadiness(recordAudioGranted = true, recognizerAvailable = true).ready)
        assertFalse(SpeechRecognitionReadiness(recordAudioGranted = false, recognizerAvailable = true).ready)
        assertFalse(SpeechRecognitionReadiness(recordAudioGranted = true, recognizerAvailable = false).ready)
    }

    @Test
    fun guidanceTellsTheUserTheNextStep() {
        val noPerm = SpeechRecognitionReadiness(recordAudioGranted = false, recognizerAvailable = true).userGuidance()
        assertTrue("mentions the录音 permission", noPerm.contains("录音权限"))

        val noService = SpeechRecognitionReadiness(recordAudioGranted = true, recognizerAvailable = false).userGuidance()
        assertTrue("mentions enabling the service", noService.contains("语音识别服务"))
        assertTrue("offers manual fallback", noService.contains("手动") || noService.contains("导入字幕"))
        assertTrue("recording still saved", noService.contains("录音"))
    }

    @Test
    fun errorMapperGivesFriendlyChineseNotRawCodes() {
        assertTrue(SpeechRecognitionErrorMapper.message(SpeechRecognitionErrorMapper.ERROR_INSUFFICIENT_PERMISSIONS).contains("权限"))
        assertTrue(SpeechRecognitionErrorMapper.message(SpeechRecognitionErrorMapper.ERROR_NETWORK).contains("网络"))
        assertTrue(SpeechRecognitionErrorMapper.message(SpeechRecognitionErrorMapper.ERROR_NO_MATCH).contains("没有听清"))
        assertTrue(SpeechRecognitionErrorMapper.message(SpeechRecognitionErrorMapper.ERROR_LANGUAGE_NOT_SUPPORTED).contains("语言"))
        // Unknown code -> safe honest fallback, never a raw number.
        val unknown = SpeechRecognitionErrorMapper.message(9999)
        assertTrue(unknown.contains("暂不可用"))
        assertFalse(unknown.contains("9999"))
    }

    @Test
    fun diagnosticsLineIsForDevelopersOnly() {
        val line = SpeechRecognitionReadiness(recordAudioGranted = true, recognizerAvailable = false, locale = "zh-CN").diagnosticsLine()
        assertEquals("RECORD_AUDIO=granted · recognizer=unavailable · locale=zh-CN", line)
    }

    @Test
    fun systemSpeechSettingsTargetsProvideOrderedFallbacks() {
        val targets = SpeechRecognitionSettingsTargets.ordered()

        assertEquals(SpeechRecognitionSettingsTargets.ACTION_VOICE_INPUT_SETTINGS, targets.first().action)
        assertTrue(targets.any { it.action == SpeechRecognitionSettingsTargets.ACTION_INPUT_METHOD_SETTINGS })
        assertTrue(targets.any { it.action == SpeechRecognitionSettingsTargets.ACTION_APPLICATION_DETAILS_SETTINGS && it.requiresAppPackageUri })
        assertEquals(SpeechRecognitionSettingsTargets.ACTION_SETTINGS, targets.last().action)
        assertTrue(SpeechRecognitionSettingsTargets.unavailableGuidance().contains("未提供系统语音识别服务"))
        assertTrue(SpeechRecognitionSettingsTargets.unavailableGuidance().contains("仅录音"))
        assertTrue(SpeechRecognitionSettingsTargets.unavailableGuidance().contains("手动粘贴"))
    }
}
