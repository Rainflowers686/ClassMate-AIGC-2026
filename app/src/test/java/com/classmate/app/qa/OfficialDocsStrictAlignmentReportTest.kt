package com.classmate.app.qa

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialDocsStrictAlignmentReportTest {
    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun readWorkspace(path: String): String =
        firstExisting(path, "../$path").readText()

    @Test
    fun strictAlignmentReportCoversIncludedOfficialCapabilitiesAndExcludedBoundary() {
        val report = readWorkspace("docs/current/official_docs_strict_alignment_report.md")

        listOf(
            "1745",
            "1805",
            "1732",
            "2201",
            "1737",
            "1733",
            "1734",
            "2060",
            "2061",
            "1738",
            "1740",
            "1739",
            "2065",
            "2068",
            "1735",
            "1802",
            "1804",
            "1803",
        ).forEach { docId ->
            assertTrue("report missing docId $docId", report.contains(docId))
        }

        assertTrue(report.contains("Voice clone / 声音复刻"))
        assertTrue(report.contains("LBS / POI / 地理编码"))
        assertTrue(report.contains("Excluded from product and smoke"))
    }

    @Test
    fun strictAlignmentReportRecordsQwenGuardAndConservativeSmokeRules() {
        val report = readWorkspace("docs/current/official_docs_strict_alignment_report.md")

        assertTrue(report.contains("qwen3.5-plus"))
        assertTrue(report.contains("enable_thinking=false"))
        assertTrue(report.contains("Guard is retained"))
        assertTrue(report.contains("Generic qwen/BlueLM cloud model config is only valid for text generation"))
        assertTrue(report.contains("Query Rewrite Hang"))
        assertTrue(report.contains("-TimeoutSeconds"))
    }

    @Test
    fun smokeSetupDocumentsV4ConservativeMapping() {
        val setup = readWorkspace("docs/current/official_provider_smoke_setup.md")

        assertTrue(setup.contains("Official Provider Smoke Setup v4"))
        assertTrue(setup.contains("Generic cloud model config does not mean"))
        assertTrue(setup.contains("LOCAL_CONFIG_BLUELM") && setup.contains("not valid mapping sources"))
        assertTrue(setup.contains("FAIL_TIMEOUT"))
        assertTrue(setup.contains("FAIL_HTTP_404_ENDPOINT_SUSPECT"))
    }
}
