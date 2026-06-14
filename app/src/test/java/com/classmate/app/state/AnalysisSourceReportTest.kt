package com.classmate.app.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8D Phase 0: the analysis source breakdown must never hide an on-device failure behind the
 * cloud code, and must label the final source honestly (云端蓝心 / 端侧蓝心 / 安全占位).
 */
class AnalysisSourceReportTest {

    @Test
    fun cloudSuccessIsCloudSource() {
        val r = AnalysisSourceReport.of("OK", onDeviceAttempted = false, onDeviceReason = null, analysisSucceeded = true)
        assertEquals(AnalysisSourceReport.SOURCE_CLOUD, r.finalSource)
    }

    @Test
    fun cloudFailThenOnDeviceAcceptedIsOnDeviceSource() {
        val r = AnalysisSourceReport.of("CONFIG_MISSING", onDeviceAttempted = true, onDeviceReason = "ACCEPTED", analysisSucceeded = true)
        assertEquals(AnalysisSourceReport.SOURCE_ON_DEVICE, r.finalSource)
        assertTrue(r.onDeviceAttempted)
    }

    @Test
    fun bothFailIsSafetyPlaceholderAndKeepsOnDeviceReason() {
        val r = AnalysisSourceReport.of("CONFIG_MISSING", onDeviceAttempted = true, onDeviceReason = "INVALID_JSON", analysisSucceeded = false)
        assertEquals(AnalysisSourceReport.SOURCE_PLACEHOLDER, r.finalSource)
        // The on-device reason is preserved (not collapsed into the cloud code).
        assertEquals("CONFIG_MISSING", r.cloudStatus)
        assertEquals("INVALID_JSON", r.onDeviceReason)
    }

    @Test
    fun reasonCodesMapToHonestChinese() {
        assertEquals("未尝试", AnalysisSourceReport.onDeviceReasonZh(null))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("INVALID_JSON").contains("JSON"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("VALIDATION_FAILED").contains("校验"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("UNAVAILABLE").contains("不可用"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("ACCEPTED").contains("落库"))
    }

    @Test
    fun adviceIsCauseSpecificNeverBlanketPermissionHint() {
        // Stage 8D-2: permission advice ONLY for PERMISSION_MISSING; file advice ONLY for files.
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("PERMISSION_MISSING").contains("授予模型目录访问权限"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("MODEL_FILES_MISSING").contains("/sdcard/1225"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("INIT_FAILED").contains("INIT_FAILED"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("GENERATE_FAILED").contains("GENERATE_FAILED"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("TIMEOUT").contains("超时"))
        assertTrue(AnalysisSourceReport.onDeviceReasonZh("SDK_MISSING").contains("SDK"))

        // The old bug: every failure showed "请检查模型目录授权". Now only PERMISSION_MISSING may say so.
        listOf("UNAVAILABLE", "INVALID_JSON", "VALIDATION_FAILED", "GENERATE_FAILED", "TIMEOUT", "INIT_FAILED").forEach {
            assertFalse(
                "$it must not carry the permission hint",
                AnalysisSourceReport.onDeviceReasonZh(it).contains("模型目录授权"),
            )
        }
    }
}
