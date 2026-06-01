package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueLMDiagnosticRunnerTest {

    @Test
    fun successUsesMinimalBlueLmRequestAndSafeReport() {
        var callCount = 0
        val runner = BlueLMDiagnosticRunner(
            transport = HttpTransport { url, headers, body, _ ->
                callCount++
                assertEquals("https://api-ai.vivo.com.cn/v1/chat/completions?request_id=diag-req", url)
                assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
                assertEquals("fake-app-id", headers["app_id"])
                assertEquals("application/json; charset=utf-8", headers["Content-Type"])

                val root = Json.parseToJsonElement(body) as JsonObject
                val messages = root["messages"] as JsonArray
                assertEquals("Doubao-Seed-2.0-mini", (root["model"] as JsonPrimitive).content)
                assertEquals(false, (root["stream"] as JsonPrimitive).content.toBoolean())
                assertEquals(32, (root["max_tokens"] as JsonPrimitive).content.toInt())
                assertEquals("user", ((messages[0] as JsonObject)["role"] as JsonPrimitive).content)

                TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""")
            },
            requestIdFactory = { "diag-req" },
            clock = incrementingClock(),
        )

        val report = runner.run(fakeBlueLmConfig())

        assertEquals(1, callCount)
        assertEquals(BlueLMDiagnosticStatus.OK, report.status)
        assertEquals(200, report.httpStatus)
        assertEquals("request_id", report.requestIdNameUsed)
        assertEquals("OK", report.contentPreview)
        assertEquals(2, report.contentLength)
        assertFalse(report.toString().contains("fake-app-key-for-tests"))
        assertFalse(report.safeLines().joinToString().contains("Authorization"))
        assertFalse(report.safeLines().joinToString().contains("请只回复 OK"))
    }

    @Test
    fun reasoningContentIsMetadataOnly() {
        val runner = BlueLMDiagnosticRunner(
            transport = HttpTransport { _, _, _, _ ->
                TransportResponse(200, """{"choices":[{"message":{"content":"OK","reasoning_content":"hidden reasoning"}}]}""")
            },
            requestIdFactory = { "diag-req" },
            clock = incrementingClock(),
        )

        val report = runner.run(fakeBlueLmConfig())

        assertEquals(BlueLMDiagnosticStatus.OK, report.status)
        assertTrue(report.reasoningPresent)
        assertEquals("hidden reasoning".length, report.reasoningLength)
        assertFalse(report.toString().contains("hidden reasoning"))
        assertFalse(report.safeLines().joinToString().contains("hidden reasoning"))
    }

    @Test
    fun socketTimeoutMapsToSocketTimeoutSubtype() {
        val runner = BlueLMDiagnosticRunner(
            transport = HttpTransport { _, _, _, _ -> throw SocketTimeoutException("fake timeout") },
            requestIdFactory = { "diag-req" },
            clock = incrementingClock(),
        )

        val report = runner.run(fakeBlueLmConfig())

        assertEquals(BlueLMDiagnosticStatus.FAIL, report.status)
        assertEquals(BlueLMDiagnosticSubtype.SOCKET_TIMEOUT, report.subtype)
        assertFalse(report.toString().contains("fake timeout"))
    }

    @Test
    fun sslExceptionMapsToTlsSsl() {
        val runner = BlueLMDiagnosticRunner(
            transport = HttpTransport { _, _, _, _ -> throw SSLException("fake ssl") },
            requestIdFactory = { "diag-req" },
            clock = incrementingClock(),
        )

        val report = runner.run(fakeBlueLmConfig())

        assertEquals(BlueLMDiagnosticStage.TLS, report.stage)
        assertEquals(BlueLMDiagnosticSubtype.SSL, report.subtype)
        assertFalse(report.toString().contains("fake ssl"))
    }

    @Test
    fun missingAppIdHttp401MapsToSafeSubtype() {
        val runner = BlueLMDiagnosticRunner(
            transport = HttpTransport { _, _, _, _ ->
                TransportResponse(401, """{"message":"app_id is required"}""")
            },
            requestIdFactory = { "diag-req" },
            clock = incrementingClock(),
        )

        val report = runner.run(fakeBlueLmConfig())

        assertEquals(BlueLMDiagnosticStatus.FAIL, report.status)
        assertEquals(BlueLMDiagnosticStage.HTTP, report.stage)
        assertEquals(BlueLMDiagnosticSubtype.APP_ID_HEADER_MISSING, report.subtype)
        assertEquals(401, report.httpStatus)
        assertFalse(report.toString().contains("app_id is required"))
    }

    @Test
    fun unconfiguredCredentialReturnsConfigMissingWithoutHttpCall() {
        var callCount = 0
        val runner = BlueLMDiagnosticRunner(
            transport = HttpTransport { _, _, _, _ -> callCount++; TransportResponse(200, "{}") },
            requestIdFactory = { "diag-req" },
            clock = incrementingClock(),
        )

        val nullReport = runner.run(null)
        val noCredentialReport = runner.run(
            ProviderConfig(kind = ProviderKind.BLUELM, enabled = true, credential = Credential.None),
        )

        // Diagnostic must fail before any network I/O when there is no real credential.
        assertEquals(0, callCount)
        assertEquals(BlueLMDiagnosticStatus.FAIL, nullReport.status)
        assertEquals(BlueLMDiagnosticSubtype.CONFIG_MISSING, nullReport.subtype)
        assertEquals(BlueLMDiagnosticStatus.FAIL, noCredentialReport.status)
        assertEquals(BlueLMDiagnosticSubtype.CONFIG_MISSING, noCredentialReport.subtype)
    }

    private fun fakeBlueLmConfig() = ProviderConfig(
        kind = ProviderKind.BLUELM,
        enabled = true,
        baseUrl = "https://api-ai.vivo.com.cn/v1",
        model = "Doubao-Seed-2.0-mini",
        credential = Credential.BlueLm("fake-app-id", "fake-app-key-for-tests"),
        timeoutMs = 30_000,
    )

    private fun incrementingClock(): () -> Long {
        var now = 0L
        return { now += 10L; now }
    }
}
