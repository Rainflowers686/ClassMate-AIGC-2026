package com.classmate.app.platform

import com.classmate.core.provider.BlueLMDiagnosticReport
import com.classmate.core.provider.BlueLMDiagnosticStage
import com.classmate.core.provider.BlueLMDiagnosticStatus
import com.classmate.core.provider.BlueLMDiagnosticSubtype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OfficialProviderDiagnosticsTest {

    @Test
    fun blueLmDiagnosticCategoriesAreUserSafe() {
        assertEquals(
            ProviderDryRunCategory.SUCCESS,
            OfficialProviderDiagnostics.fromBlueLm(report(BlueLMDiagnosticStatus.OK)).category,
        )
        assertEquals(
            ProviderDryRunCategory.SKIP_MISSING_CONFIG,
            OfficialProviderDiagnostics.fromBlueLm(report(subtype = BlueLMDiagnosticSubtype.CONFIG_MISSING)).category,
        )
        assertEquals(
            ProviderDryRunCategory.AUTH_FAILED,
            OfficialProviderDiagnostics.fromBlueLm(report(subtype = BlueLMDiagnosticSubtype.HTTP_401)).category,
        )
        assertEquals(
            ProviderDryRunCategory.NETWORK_FAILED,
            OfficialProviderDiagnostics.fromBlueLm(report(subtype = BlueLMDiagnosticSubtype.UNKNOWN_HOST)).category,
        )
        assertEquals(
            ProviderDryRunCategory.TIMEOUT,
            OfficialProviderDiagnostics.fromBlueLm(report(subtype = BlueLMDiagnosticSubtype.SOCKET_TIMEOUT)).category,
        )
        assertEquals(
            ProviderDryRunCategory.PARSE_ERROR,
            OfficialProviderDiagnostics.fromBlueLm(report(subtype = BlueLMDiagnosticSubtype.PARSE_ERROR)).category,
        )
        assertEquals(
            ProviderDryRunCategory.EMPTY_RESPONSE,
            OfficialProviderDiagnostics.fromBlueLm(report(subtype = BlueLMDiagnosticSubtype.EMPTY_RESPONSE)).category,
        )
    }

    @Test
    fun captureDiagnosticsSkipWhenCredentialsAreMissing() {
        val missing = OfficialProviderDiagnostics.fromCaptureConfig(
            capability = "官方 OCR",
            status = CaptureConfigStatus(configured = false, hasAppId = false, hasAppKey = false),
            configuredMessage = "ready",
            missingMessage = "missing",
        )

        assertEquals(ProviderDryRunCategory.SKIP_MISSING_CONFIG, missing.category)
        assertFalse(missing.liveRequestAttempted)
        assertEquals("MISSING_APP_ID_APP_KEY", missing.safeCode)
    }

    @Test
    fun captureDiagnosticsClassifyLongAsrWithoutAudioAsSkipped() {
        val result = OfficialProviderDiagnostics.fromCaptureConfig(
            capability = "官方长语音转写",
            status = CaptureConfigStatus(configured = true, hasAppId = true, hasAppKey = true),
            configuredMessage = "ready",
            missingMessage = "missing",
            noAudio = true,
        )

        assertEquals(ProviderDryRunCategory.SKIPPED_NO_AUDIO, result.category)
        assertFalse(result.liveRequestAttempted)
    }

    @Test
    fun dryRunResultDoesNotContainCredentialValues() {
        val result = ProviderDryRunResult(
            capability = "蓝心云端大模型",
            category = ProviderDryRunCategory.AUTH_FAILED,
            messageZh = "鉴权失败",
            configured = true,
            liveRequestAttempted = true,
            safeCode = "HTTP_401",
        )

        assertFalse(result.toString().contains("unit-sensitive-value"))
        assertFalse(result.displayLine().contains("unit-sensitive-value"))
    }

    private fun report(
        status: BlueLMDiagnosticStatus = BlueLMDiagnosticStatus.FAIL,
        subtype: BlueLMDiagnosticSubtype? = null,
    ): BlueLMDiagnosticReport =
        BlueLMDiagnosticReport(
            status = status,
            stage = if (status == BlueLMDiagnosticStatus.OK) null else BlueLMDiagnosticStage.HTTP,
            subtype = subtype,
            latencyMs = 10,
            httpStatus = if (subtype?.name?.startsWith("HTTP_") == true) 401 else null,
            requestIdNameUsed = "request_id",
        )
}
