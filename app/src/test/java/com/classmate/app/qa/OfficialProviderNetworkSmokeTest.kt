package com.classmate.app.qa

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialProviderNetworkSmokeTest {
    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun readWorkspace(path: String): String =
        firstExisting(path, "../$path").readText()

    private fun repoRoot(): File = firstExisting(".git", "../.git").parentFile ?: File(".").absoluteFile

    private fun runSmokeScript(vararg args: String, env: Map<String, String> = emptyMap()): String {
        val script = File(repoRoot(), "scripts/qa/official_provider_smoke.ps1")
        val command = listOf(
            "powershell",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            script.absolutePath,
        ) + args
        val process = ProcessBuilder(command)
            .directory(repoRoot())
            .redirectErrorStream(true)
            .apply { environment().putAll(env) }
            .start()
        assertTrue("official_provider_smoke.ps1 timed out", process.waitFor(30, TimeUnit.SECONDS))
        val output = process.inputStream.bufferedReader().readText()
        assertTrue("script failed with ${process.exitValue()}: $output", process.exitValue() == 0)
        return output
    }

    @Test
    fun smokeScriptDefaultsToDryRunAndRequiresExplicitNetworkFlag() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("[switch]\$DryRun"))
        assertTrue(script.contains("[switch]\$RunNetwork"))
        assertTrue(script.contains("[switch]\$UseLocalConfig"))
        assertTrue(script.contains("[switch]\$PrintSetupHelp"))
        assertTrue(script.contains("[switch]\$ExplainConfig"))
        assertTrue(script.contains("[string]\$LocalConfigPath"))
        assertTrue(script.contains("[int]\$TimeoutSeconds = 20"))
        assertTrue(script.contains("if (-not \$RunNetwork)"))
        assertTrue(script.contains("\$DryRun = \$true"))
        assertTrue(script.contains("No network request was sent."))
    }

    @Test
    fun genericBlueLmLocalConfigDoesNotMakeOcrOrRetrievalReady() {
        val temp = Files.createTempDirectory("classmate-smoke-generic").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText("""{"provider":"bluelm","appId":"id12345","appKey":"key67890","baseUrl":"https://api-ai.vivo.com.cn/v1"}""")

        val ocrOutput = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-Capability",
            "OCR",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
        )
        val retrievalOutput = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-Capability",
            "QUERY_REWRITE",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            File(temp, "out2").absolutePath,
        )

        assertTrue(ocrOutput.contains("topLevel.bluelm exists: True"))
        assertTrue(ocrOutput.contains("OCR: capability URL configured=False"))
        assertTrue(ocrOutput.contains("endpointMapping=MISSING"))
        assertTrue(ocrOutput.contains("missing config:"))
        assertTrue(ocrOutput.contains("vivoCapture"))
        assertTrue(retrievalOutput.contains("QUERY_REWRITE: capability URL configured=False"))
        assertTrue(retrievalOutput.contains("endpointMapping=MISSING"))
        listOf("id12345", "key67890", "api-ai.vivo.com.cn/v1").forEach {
            assertFalse("config value leaked: $it", ocrOutput.contains(it))
            assertFalse("config value leaked: $it", retrievalOutput.contains(it))
        }
    }

    @Test
    fun localConfigReadIsExplicitOptInOnly() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")
        val localConfigBlock = script.substringAfter("function Read-LocalSmokeConfig").substringBefore("function New-OcrInput")

        assertTrue(localConfigBlock.contains("if (-not \$UseLocalConfig"))
        assertTrue(localConfigBlock.contains("Get-Content -LiteralPath \$LocalConfigPath -Raw"))
        assertTrue(script.contains("content read only with -UseLocalConfig"))
        assertTrue(script.contains("local config read"))
    }

    @Test
    fun explainConfigWithMockedVivoCaptureCanMapOcrReadyWithoutLeakingValues() {
        val temp = Files.createTempDirectory("classmate-smoke-vivo").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText("""{"vivoCapture":{"appId":"id12345","appKey":"key67890","baseUrl":"https://api-ai.vivo.com.cn/"}}""")

        val output = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
        )

        assertTrue(output.contains("local config read: True"))
        assertTrue(output.contains("vivoCapture exists: True"))
        assertTrue(output.contains("OCR: capability URL configured=True; auth configured=True"))
        assertTrue(output.contains("source=LOCAL_CONFIG_VIVO_CAPTURE"))
        assertTrue(output.contains("mappingSource=LOCAL_CONFIG_VIVO_CAPTURE"))
        assertTrue(output.contains("endpointMapping=READY"))
        assertTrue(output.contains("authMapping=READY"))
        assertFalse(output.contains("id12345"))
        assertFalse(output.contains("key67890"))
        assertFalse(output.contains("api-ai.vivo.com.cn/"))
    }

    @Test
    fun officialProvidersOcrConfigMapsOcrReadyWithoutLeakingValues() {
        val temp = Files.createTempDirectory("classmate-smoke-official-ocr").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText(
            """
            {
              "officialProviders": {
                "ocr": {
                  "enabled": true,
                  "baseUrl": "https://official-ocr.example.invalid",
                  "authHeader": "Authorization",
                  "authValue": "secret-ocr-auth"
                }
              }
            }
            """.trimIndent(),
        )

        val output = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-Capability",
            "OCR",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
        )

        assertTrue(output.contains("officialProviders exists: True"))
        assertTrue(output.contains("officialProviders.ocr"))
        assertTrue(output.contains("OCR: capability URL configured=True; auth configured=True"))
        assertTrue(output.contains("source=LOCAL_CONFIG_OFFICIAL_PROVIDER"))
        assertTrue(output.contains("mappingSource=LOCAL_CONFIG_OFFICIAL_PROVIDER"))
        assertTrue(output.contains("endpointMapping=READY"))
        assertTrue(output.contains("authMapping=READY"))
        listOf("official-ocr.example.invalid", "secret-ocr-auth").forEach {
            assertFalse("config value leaked: $it", output.contains(it))
        }
    }

    @Test
    fun officialProvidersQueryRewriteConfigMapsReadyWithoutTopLevelBlueLm() {
        val temp = Files.createTempDirectory("classmate-smoke-official-query").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText(
            """
            {
              "officialProviders": {
                "queryRewrite": {
                  "enabled": true,
                  "baseUrl": "https://official-query.example.invalid",
                  "authValue": "secret-query-auth"
                }
              }
            }
            """.trimIndent(),
        )

        val output = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-Capability",
            "QUERY_REWRITE",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
        )

        assertTrue(output.contains("officialProviders.queryRewrite"))
        assertTrue(output.contains("QUERY_REWRITE: capability URL configured=True; auth configured=True"))
        assertTrue(output.contains("source=LOCAL_CONFIG_OFFICIAL_PROVIDER"))
        assertTrue(output.contains("endpointMapping=READY"))
        assertFalse(output.contains("official-query.example.invalid"))
        assertFalse(output.contains("secret-query-auth"))
    }

    @Test
    fun explainConfigReportsMissingLocalFieldNamesOnly() {
        val temp = Files.createTempDirectory("classmate-smoke-missing").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText("""{"vivoCapture":{"appId":"id12345"}}""")

        val output = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
        )

        assertTrue(output.contains("missing config:"))
        assertTrue(output.contains("appKey"))
        assertTrue(output.contains("baseUrl"))
        assertFalse(output.contains("id12345"))
    }

    @Test
    fun envExplicitConfigOverridesLocalConfigAndDoesNotWriteValues() {
        val temp = Files.createTempDirectory("classmate-smoke-env").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText("""{"vivoCapture":{"appId":"local-id-12345","appKey":"local-key-67890","baseUrl":"https://local.example.invalid/"}}""")

        val output = runSmokeScript(
            "-DryRun",
            "-UseLocalConfig",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
            env = mapOf(
                "CLASSMATE_PROVIDER_SMOKE_OCR_URL" to "https://env.example.invalid/ocr",
                "CLASSMATE_PROVIDER_SMOKE_OCR_AUTH_VALUE" to "env-secret-value",
            ),
        )

        val resultJson = File(outputDir, "smoke_result.json").readText()
        val resultMd = File(outputDir, "smoke_result.md").readText()
        val log = File(outputDir, "smoke.log").readText()

        assertTrue(output.contains("OCR: DRY_RUN_READY"))
        assertTrue(resultJson.contains("ENV_EXPLICIT"))
        assertTrue(resultMd.contains("ENV_EXPLICIT"))
        listOf("env-secret-value", "local-key-67890", "local-id-12345", "env.example.invalid", "local.example.invalid").forEach {
            assertFalse("secret/config value leaked: $it", resultJson.contains(it))
            assertFalse("secret/config value leaked: $it", resultMd.contains(it))
            assertFalse("secret/config value leaked: $it", log.contains(it))
        }
    }

    @Test
    fun setupHelpListsEnvNamesButOnlyPlaceholderValues() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("Print-SetupHelp"))
        assertTrue(script.contains("Official provider smoke setup v5"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_OCR_URL=<your-value>") || script.contains("\$entry.urlEnv + \"=<your-value>\""))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE=<your-value>"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_QUERY_REWRITE_URL"))
        assertTrue(script.contains("officialProviders"))
        assertTrue(script.contains("\"ocr\": { \"enabled\": true, \"baseUrl\": \"<your-value>\", \"endpointPath\": \"<your-value>\""))
        assertTrue(script.contains("\"queryRewrite\""))
        assertTrue(script.contains("-TimeoutSeconds <seconds>"))
        assertFalse(script.contains("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE=sk-"))
    }

    @Test
    fun configTemplateUsesPlaceholdersAndDocumentsOfficialProviderGroups() {
        val template = readWorkspace("docs/current/official_provider_config_template.md")

        listOf(
            "\"officialProviders\"",
            "\"ocr\"",
            "\"queryRewrite\"",
            "\"textSimilarity\"",
            "\"embedding\"",
            "\"translation\"",
            "\"tts\"",
            "\"functionCalling\"",
            "\"asrLong\"",
            "\"endpointPath\"",
            "<official-ocr-base-url>",
            "<official-query-rewrite-endpoint-path>",
            "<your-auth-value>",
            "不要提交 `config.local.json`",
            "不要把 key 发给任何 AI",
            "OCR",
            "QUERY_REWRITE",
            "TEXT_SIMILARITY",
            "EMBEDDING",
        ).forEach {
            assertTrue("template missing: $it", template.contains(it))
        }

        assertFalse(
            "template contains real-looking OpenAI-style key",
            Regex("""sk-[A-Za-z0-9]{8,}""").containsMatchIn(template),
        )
        listOf("secret-auth", "api-ai.vivo.com.cn/v1", "id12345", "key67890").forEach {
            assertFalse("template contains real-looking value: $it", template.contains(it))
        }
    }

    @Test
    fun setupDocExplainsConfigReadinessAndLocalArtifactPolicy() {
        val setup = readWorkspace("docs/current/official_provider_smoke_setup.md")

        listOf(
            "official_provider_config_template.md",
            "officialProviders missing",
            "topLevel.bluelm only configures cloud model",
            "endpointMapping=READY",
            "authMapping=READY",
            "requestSchema=READY",
            "-RunNetwork -Capability OCR",
            "git ls-files .codex_work",
            "Do not commit `.codex_work`",
        ).forEach {
            assertTrue("setup doc missing: $it", setup.contains(it))
        }
    }

    @Test
    fun explainConfigGuidesOfficialProvidersWithoutLeakingValues() {
        val temp = Files.createTempDirectory("classmate-smoke-guidance").toFile()
        val config = File(temp, "config.local.json")
        val outputDir = File(temp, "out")
        config.writeText("""{"provider":"bluelm","appId":"id12345","appKey":"key67890","baseUrl":"https://api-ai.vivo.com.cn/v1"}""")

        val output = runSmokeScript(
            "-ExplainConfig",
            "-UseLocalConfig",
            "-Capability",
            "OCR",
            "-LocalConfigPath",
            config.absolutePath,
            "-OutputDir",
            outputDir.absolutePath,
        )

        assertTrue(output.contains("officialProviders missing"))
        assertTrue(output.contains("topLevel.bluelm only configures cloud model"))
        assertTrue(output.contains("officialProviders.<capability>"))
        listOf("id12345", "key67890", "api-ai.vivo.com.cn/v1").forEach {
            assertFalse("config value leaked: $it", output.contains(it))
        }
    }

    @Test
    fun allSafeExcludesLongAsrAndDevLabCapabilities() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")
        val safeBlock = script.substringAfter("\$SafeCapabilities = @(").substringBefore(")")

        listOf("OCR", "QUERY_REWRITE", "TEXT_SIMILARITY", "TRANSLATION", "TTS", "FUNCTION_CALLING", "EMBEDDING").forEach {
            assertTrue("safe capability missing: $it", safeBlock.contains("\"$it\""))
        }

        listOf("ASR_LONG", "IMAGE_GEN", "VIDEO_GEN", "SHORT_ASR", "DIALECT_ASR", "SIMULTANEOUS_INTERPRETATION").forEach {
            assertFalse("unsafe capability included in AllSafe: $it", safeBlock.contains("\"$it\""))
        }
    }

    @Test
    fun scriptWritesOnlyCodexWorkOutputsAndRedactsSensitiveTerms() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains(".codex_work\\official_provider_smoke"))
        assertTrue(script.contains("smoke_result.json"))
        assertTrue(script.contains("smoke_result.md"))
        assertTrue(script.contains("smoke.log"))
        assertTrue(script.contains("test_inputs"))
        assertTrue(script.contains("outputs"))
        assertTrue(script.contains("config.local.json"))
        assertTrue(script.contains("content not read") || script.contains("content read only with -UseLocalConfig"))

        assertFalse(script.contains("Auth" + "orization:"))
        assertFalse(script.contains("Bear" + "er "))
        assertFalse(script.contains("app" + "Key:"))
        assertFalse(script.contains("app" + "Id:"))
        assertFalse(script.contains("api" + "Key:"))
        assertFalse(script.contains("VOICE" + "_CLONE"))
        assertFalse(script.contains("GEO" + "_POI"))
        assertFalse(script.contains("CLASSMATE_PROVIDER_SMOKE_L" + "BS"))
        assertFalse(script.contains("CLASSMATE_PROVIDER_SMOKE_P" + "OI"))
    }

    @Test
    fun configMissingIsSkippedNotScriptFailure() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("SKIPPED_CONFIG_MISSING"))
        assertTrue(script.contains("missingEnvNames"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_OCR_URL"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_OCR_AUTH_VALUE"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE"))
        assertFalse(script.contains("ConfigMissing is failure"))
    }

    @Test
    fun endpointMappingAndSeamOnlyStatesAreRecorded() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("SKIPPED_ENDPOINT_MAPPING_MISSING"))
        assertTrue(script.contains("SKIPPED_SEAM_ONLY"))
        assertTrue(script.contains("FAIL_INVALID_URI"))
        assertTrue(script.contains("FAIL_TIMEOUT"))
        assertTrue(script.contains("FAIL_HTTP_404_ENDPOINT_SUSPECT"))
        assertTrue(script.contains("SeamReadyButEndpointMappingMissing"))
        assertTrue(script.contains("endpointMappingStatus"))
        assertTrue(script.contains("requestSchemaStatus"))
        assertTrue(script.contains("mappingSource"))
        assertTrue(script.contains("missingConfigFields"))
        assertTrue(script.contains("detectedConfigGroups"))
        assertTrue(script.contains("SEAM_ONLY"))
        assertTrue(script.contains("GENERIC_ONLY"))
    }

    @Test
    fun smokeScriptSafelyComposesAndValidatesProviderUrls() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        listOf(
            "function Join-SmokeUrl",
            "function Add-SmokeQueryParameter",
            "function Test-SmokeUri",
            "[System.Uri]::TryCreate",
            "Invalid URI after endpoint composition",
            "FAIL_INVALID_URI",
            "requestAttempted",
            "uriValidated",
            "requestSent = if (\$failureStatus -eq \"FAIL_INVALID_URI\") { \$false } else { \$requestAttempted }",
            "Invoke-WebRequest -Uri \$uriCheck.uri",
        ).forEach {
            assertTrue("script missing URL-safety marker: $it", script.contains(it))
        }

        assertTrue(script.contains("\$value = \$value.Trim().TrimEnd(\"/\")"))
        assertTrue(script.contains("if (-not \$path.StartsWith(\"/\"))"))
        assertTrue(script.contains("if ([string]\$Url -like \"*?*\") { \"&\" } else { \"?\" }"))
        assertTrue(script.contains("Add-SmokeQueryParameter \$url \"requestId\" \"classmate-smoke\""))
        assertFalse(script.contains("https://\$Domain\$Path?requestId=classmate-smoke"))
        assertFalse(script.contains("https://api-ai.vivo.com.cn=classmate-smoke"))
    }

    @Test
    fun dryRunResultRecordsNoRequestAttemptOrUriValidation() {
        val temp = Files.createTempDirectory("classmate-smoke-dry-uri").toFile()
        val outputDir = File(temp, "out")

        val output = runSmokeScript(
            "-DryRun",
            "-Capability",
            "OCR",
            "-OutputDir",
            outputDir.absolutePath,
        )

        val resultJson = File(outputDir, "smoke_result.json").readText()
        val resultMd = File(outputDir, "smoke_result.md").readText()

        assertTrue(output.contains("OCR: DRY_RUN_READY"))
        assertTrue(resultJson.contains("\"requestSent\":  false") || resultJson.contains("\"requestSent\": false"))
        assertTrue(resultJson.contains("\"requestAttempted\":  false") || resultJson.contains("\"requestAttempted\": false"))
        assertTrue(resultJson.contains("\"uriValidated\":  false") || resultJson.contains("\"uriValidated\": false"))
        assertTrue(resultMd.contains("requestAttempted"))
        assertTrue(resultMd.contains("uriValidated"))
    }

    @Test
    fun codexWorkIsNotTracked() {
        val repo = firstExisting(".git", "../.git").parentFile
        val process = ProcessBuilder("git", "ls-files", ".codex_work")
            .directory(repo)
            .redirectErrorStream(true)
            .start()
        assertTrue("git ls-files timed out", process.waitFor(10, TimeUnit.SECONDS))
        val output = process.inputStream.bufferedReader().readText().trim()
        assertTrue(".codex_work should not be tracked: $output", output.isEmpty())
    }

    @Test
    fun globalProviderGuardsRemainInPlace() {
        val vendorIo = readWorkspace("core/src/main/kotlin/com/classmate/core/provider/VendorIo.kt")
        val diagnostic = readWorkspace("core/src/main/kotlin/com/classmate/core/provider/BlueLMDiagnostic.kt")
        val mainFiles = firstExisting("app/src/main", "../app/src/main")
            .walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .plus(
                firstExisting("core/src/main", "../core/src/main")
                    .walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") },
            )

        assertTrue(vendorIo.contains("qwen3.5-plus"))
        assertTrue(vendorIo.contains("supportsEnableThinking"))
        assertTrue(vendorIo.contains("put(\"enable_thinking\", it)"))
        assertTrue(vendorIo.contains("reasoning_effort"))
        assertTrue(diagnostic.contains("enable_thinking"))
        assertFalse(vendorIo.contains("put(\"enable_thinking\", false)"))
        assertFalse(diagnostic.contains("put(\"enable_thinking\", false)"))
        assertFalse(mainFiles.any { it.readText().contains("import com.vivo.llmsdk") })
    }
}
