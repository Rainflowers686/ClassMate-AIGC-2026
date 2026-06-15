package com.classmate.app.platform

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Config-loader tests use FAKE config JSON via [CaptureConfigLoader.parse] — never the real
 * config.local.json. Asserts secure parsing, redaction, placeholder rejection, and ConfigMissing.
 */
class CaptureConfigLoaderTest {

    private val loader = CaptureConfigLoader(File("does-not-exist-capture-test.json"))

    @Test fun emptyOrInvalidConfigIsNotConfigured() {
        assertFalse(loader.parse("{}").isConfigured)
        assertFalse(loader.parse("not json at all").isConfigured)
        // Missing file → ABSENT (no crash, no real-file read).
        assertFalse(loader.load().isConfigured)
    }

    @Test fun vivoCaptureBlockConfiguresAndNormalizesDomain() {
        val cfg = loader.parse(
            """{"vivoCapture":{"appId":"id12345","appKey":"key67890","baseUrl":"https://api-ai.vivo.com.cn/"}}""",
        )
        assertTrue(cfg.isConfigured)
        assertEquals("api-ai.vivo.com.cn", cfg.domain)
        assertEquals("aigcid12345", cfg.businessId())
    }

    @Test fun fallsBackToBluelmCredentials() {
        val cfg = loader.parse("""{"providers":{"bluelm":{"appId":"bid111","appKey":"bkey222"}}}""")
        assertTrue(cfg.isConfigured)
    }

    @Test fun placeholderAndBlankCountAsNotConfigured() {
        assertFalse(loader.parse("""{"vivoCapture":{"appId":"id","appKey":"YOUR_APPKEY"}}""").isConfigured)
        assertFalse(loader.parse("""{"vivoCapture":{"appId":"","appKey":"key67890"}}""").isConfigured)
    }

    @Test fun configToStringIsRedacted() {
        val cfg = loader.parse("""{"vivoCapture":{"appId":"secretId999","appKey":"secretKey999"}}""")
        val s = cfg.toString()
        assertFalse("toString must not contain appId", s.contains("secretId999"))
        assertFalse("toString must not contain appKey", s.contains("secretKey999"))
        assertTrue(s.contains("configured=true"))
    }

    @Test fun statusLabelsAreValueFree() {
        assertEquals("已配置", loader.status("""{"vivoCapture":{"appId":"id12345","appKey":"key67890"}}""").labelZh())
        assertEquals("未配置", loader.status("{}").labelZh())
        assertEquals("缺少 appKey", loader.status("""{"vivoCapture":{"appId":"id12345"}}""").labelZh())
    }
}
