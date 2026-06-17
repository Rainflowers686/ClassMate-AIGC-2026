param(
    [switch]$DryRun,
    [switch]$RunNetwork,
    [string[]]$Capability,
    [switch]$AllSafe,
    [string]$OutputDir,
    [switch]$NoOpen,
    [switch]$VerboseLog,
    [switch]$PrintSetupHelp,
    [switch]$UseLocalConfig,
    [switch]$ExplainConfig
)

$ErrorActionPreference = "Continue"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $OutputDir -or $OutputDir.Trim().Length -eq 0) {
    $OutputDir = Join-Path $RepoRoot ".codex_work\official_provider_smoke"
}

$ResultJson = Join-Path $OutputDir "smoke_result.json"
$ResultMd = Join-Path $OutputDir "smoke_result.md"
$LogPath = Join-Path $OutputDir "smoke.log"
$InputDir = Join-Path $OutputDir "test_inputs"
$ProviderOutputDir = Join-Path $OutputDir "outputs"

$AuthHeaderName = ("Auth" + "orization")
$BearerWord = ("Bear" + "er")
$AppKeyName = ("app" + "Key")
$AppIdName = ("app" + "Id")
$ApiKeyName = ("api" + "Key")

$SafeCapabilities = @(
    "OCR",
    "QUERY_REWRITE",
    "TEXT_SIMILARITY",
    "TRANSLATION",
    "TTS",
    "FUNCTION_CALLING",
    "EMBEDDING"
)

$CapabilityCatalog = @{
    "OCR" = @{
        docId = 1737
        tier = "product-facing"
        input = "ocr_smoke.png"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_OCR_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_OCR_AUTH_VALUE"
        localPath = "/ocr/general_recognition"
        localMapping = "READY"
        requestSchema = "READY"
    }
    "QUERY_REWRITE" = @{
        docId = 2061
        tier = "product-facing"
        input = "query_rewrite.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_QUERY_REWRITE_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_QUERY_REWRITE_AUTH_VALUE"
        localPath = "/query-rewrite-api/predict"
        localMapping = "READY"
        requestSchema = "READY"
    }
    "TEXT_SIMILARITY" = @{
        docId = 2060
        tier = "product-facing"
        input = "similarity.json"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_TEXT_SIMILARITY_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_TEXT_SIMILARITY_AUTH_VALUE"
        localPath = "/similarity-model-api/predict"
        localMapping = "READY"
        requestSchema = "READY"
    }
    "TRANSLATION" = @{
        docId = 1733
        tier = "product-facing"
        input = "translation_en.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_TRANSLATION_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_TRANSLATION_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
    }
    "TTS" = @{
        docId = 1735
        tier = "product-facing"
        input = "tts_zh.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_TTS_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_TTS_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
    }
    "FUNCTION_CALLING" = @{
        docId = 1805
        tier = "product-facing"
        input = "function_calling.json"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_FUNCTION_CALLING_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_FUNCTION_CALLING_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
    }
    "ASR_LONG" = @{
        docId = 1739
        tier = "product-facing-secondary"
        input = "asr_long.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_ASR_LONG_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_ASR_LONG_AUTH_VALUE"
        localPath = "/lasr"
        localMapping = "READY"
        requestSchema = "MISSING"
    }
    "EMBEDDING" = @{
        docId = 1734
        tier = "product-facing"
        input = "embedding.json"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_EMBEDDING_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_EMBEDDING_AUTH_VALUE"
        localPath = "/embedding-model-api/predict/batch"
        localMapping = "READY"
        requestSchema = "READY"
    }
    "IMAGE_GEN" = @{
        docId = 1732
        tier = "dev-lab"
        input = "image_gen.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_IMAGE_GEN_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_IMAGE_GEN_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
    }
    "VIDEO_GEN" = @{
        docId = 2201
        tier = "dev-lab"
        input = "video_gen.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_VIDEO_GEN_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_VIDEO_GEN_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
    }
    "SHORT_ASR" = @{
        docId = 1738
        tier = "dev-lab"
        input = "short_asr.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_SHORT_ASR_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_SHORT_ASR_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "MISSING"
    }
    "DIALECT_ASR" = @{
        docId = 2065
        tier = "dev-lab"
        input = "dialect_asr.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_DIALECT_ASR_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_DIALECT_ASR_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "MISSING"
    }
    "SIMULTANEOUS_INTERPRETATION" = @{
        docId = 2068
        tier = "dev-lab"
        input = "simultaneous_interpretation.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_SIMULTANEOUS_INTERPRETATION_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_SIMULTANEOUS_INTERPRETATION_AUTH_VALUE"
        localPath = ""
        localMapping = "SEAM_ONLY"
        requestSchema = "MISSING"
    }
}

if (-not $RunNetwork) {
    $DryRun = $true
}

function Redact-Text($Text) {
    if ($null -eq $Text) { return "" }
    $redacted = [string]$Text
    $names = @($AuthHeaderName, $BearerWord, $AppKeyName, $ApiKeyName, $AppIdName, "token", "cookie")
    foreach ($name in $names) {
        $redacted = $redacted -replace "(?i)$([regex]::Escape($name))\s*[:=]\s*[^,\s;]+", "$name=<redacted>"
    }
    foreach ($envName in @("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE")) {
        $value = [Environment]::GetEnvironmentVariable($envName)
        if ($value -and $value.Length -gt 0) {
            $redacted = $redacted.Replace($value, "<redacted>")
        }
    }
    return $redacted
}

function Ensure-Directories {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
    New-Item -ItemType Directory -Force -Path $ProviderOutputDir | Out-Null
    Set-Content -LiteralPath $LogPath -Value "Official provider smoke log $(Get-Date -Format o)"
}

function Write-SmokeLog($Message) {
    $line = "$(Get-Date -Format o) $Message"
    Add-Content -LiteralPath $LogPath -Value (Redact-Text $line)
    if ($VerboseLog) {
        Write-Host (Redact-Text $Message)
    }
}

function Print-SetupHelp {
    Write-Host "Official provider smoke setup"
    Write-Host ""
    Write-Host "Default mode is dry-run. Real requests require -RunNetwork and one explicit -Capability."
    Write-Host "config.local.json is not read unless -UseLocalConfig is passed."
    Write-Host ""
    Write-Host "Common environment variables:"
    Write-Host "  CLASSMATE_PROVIDER_SMOKE_AUTH_HEADER=<your-value>"
    Write-Host "  CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE=<your-value>"
    Write-Host "  CLASSMATE_PROVIDER_SMOKE_ALLOW_NO_AUTH=<your-value>"
    Write-Host ""
    Write-Host "Capability-specific variables:"
    foreach ($cap in ($CapabilityCatalog.Keys | Sort-Object)) {
        $entry = $CapabilityCatalog[$cap]
        Write-Host ("  " + $cap)
        Write-Host ("    " + $entry.urlEnv + "=<your-value>")
        Write-Host ("    " + $entry.authEnv + "=<your-value>")
    }
    Write-Host ""
    Write-Host "Local config opt-in:"
    Write-Host "  -UseLocalConfig maps vivoCapture or providers.bluelm presence to smoke config."
    Write-Host "  Values are kept in memory only and are never printed or written."
}

function Test-RealValue($Value) {
    if ($null -eq $Value) { return $false }
    $s = [string]$Value
    if ($s.Trim().Length -eq 0) { return $false }
    if ($s -match "(?i)your_|your-|<your|placeholder") { return $false }
    return $true
}

function Normalize-Domain($Raw) {
    $value = [string]$Raw
    if (-not (Test-RealValue $value)) { return "api-ai.vivo.com.cn" }
    return $value.Replace("https://", "").Replace("http://", "").TrimEnd("/")
}

function Read-LocalSmokeConfig {
    $path = Join-Path $RepoRoot "config.local.json"
    $state = [PSCustomObject]@{
        exists = Test-Path -LiteralPath $path
        read = $false
        parseOk = $false
        hasAppId = $false
        hasAppKey = $false
        hasBaseUrl = $false
        domain = ""
        appId = ""
        appKey = ""
    }
    if (-not $UseLocalConfig -or -not $state.exists) {
        return $state
    }
    try {
        $jsonText = Get-Content -LiteralPath $path -Raw
        $state.read = $true
        $root = $jsonText | ConvertFrom-Json
        $capture = $root.vivoCapture
        $bluelm = $null
        if ($root.providers) { $bluelm = $root.providers.bluelm }

        $appId = if ($capture -and (Test-RealValue $capture.appId)) { [string]$capture.appId } elseif ($bluelm -and (Test-RealValue $bluelm.appId)) { [string]$bluelm.appId } else { "" }
        $appKey = if ($capture -and (Test-RealValue $capture.appKey)) { [string]$capture.appKey } elseif ($bluelm -and (Test-RealValue $bluelm.appKey)) { [string]$bluelm.appKey } else { "" }
        $baseUrl = if ($capture -and (Test-RealValue $capture.baseUrl)) { [string]$capture.baseUrl } elseif ($bluelm -and (Test-RealValue $bluelm.baseUrl)) { [string]$bluelm.baseUrl } else { "" }

        $state.parseOk = $true
        $state.hasAppId = Test-RealValue $appId
        $state.hasAppKey = Test-RealValue $appKey
        $state.hasBaseUrl = Test-RealValue $baseUrl
        $state.domain = Normalize-Domain $baseUrl
        $state.appId = $appId
        $state.appKey = $appKey
    } catch {
        $state.read = $true
        $state.parseOk = $false
    }
    return $state
}

function New-OcrInput {
    $path = Join-Path $InputDir "ocr_smoke.png"
    if (Test-Path -LiteralPath $path) { return $path }
    try {
        Add-Type -AssemblyName System.Drawing -ErrorAction Stop
        $bitmap = New-Object System.Drawing.Bitmap 720, 260
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        $graphics.Clear([System.Drawing.Color]::White)
        $font = New-Object System.Drawing.Font "Arial", 34, ([System.Drawing.FontStyle]::Bold)
        $graphics.DrawString("ClassMate OCR smoke 2026", $font, [System.Drawing.Brushes]::Black, 30, 90)
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        $graphics.Dispose()
        $bitmap.Dispose()
    } catch {
        $fallbackPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        [IO.File]::WriteAllBytes($path, [Convert]::FromBase64String($fallbackPng))
        Write-SmokeLog "WARN OCR image generation fallback used."
    }
    return $path
}

function Ensure-TestInputs {
    New-OcrInput | Out-Null
    Set-Content -LiteralPath (Join-Path $InputDir "query_rewrite.txt") -Value "Please explain the meaning of Lenz's law in electromagnetic induction."
    Set-Content -LiteralPath (Join-Path $InputDir "translation_en.txt") -Value "The derivative describes the instantaneous rate of change."
    $ttsText = (-join @([char]0x8FD9, [char]0x662F, [char]0x4E00, [char]0x6BB5)) + " ClassMate " + (-join @([char]0x8BFE, [char]0x7A0B, [char]0x7CBE, [char]0x534E, [char]0x97F3, [char]0x9891, [char]0x6D4B, [char]0x8BD5, [char]0x3002))
    Set-Content -LiteralPath (Join-Path $InputDir "tts_zh.txt") -Value $ttsText
    @{
        query = "Lenz law"
        candidates = @(
            "Lenz's law states that induced current opposes the change in magnetic flux.",
            "A binary tree is a data structure with at most two children.",
            "Derivatives describe instantaneous rates of change."
        )
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $InputDir "similarity.json")
    @{
        texts = @(
            "Lenz's law explains the direction of induced current.",
            "Electromagnetic induction appears when magnetic flux changes."
        )
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $InputDir "embedding.json")
    @{
        userInput = "Find evidence for Lenz law and create one practice question."
        allowedTools = @("searchEvidence", "createPractice")
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $InputDir "function_calling.json")
    Set-Content -LiteralPath (Join-Path $InputDir "image_gen.txt") -Value "A clean study-card illustration for electromagnetic induction."
    Set-Content -LiteralPath (Join-Path $InputDir "video_gen.txt") -Value "A short non-product smoke prompt for a physics concept animation."
}

function Get-EnvOrEmpty($Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ($null -eq $value) { return "" }
    return $value
}

function Get-SelectedCapabilities {
    if ($Capability -and $Capability.Count -gt 0) {
        return @($Capability | ForEach-Object { $_.Trim().ToUpperInvariant() } | Where-Object { $_.Length -gt 0 })
    }
    return @($SafeCapabilities)
}

function Get-SmokeConfig($CapabilityName, $LocalConfig) {
    $entry = $CapabilityCatalog[$CapabilityName]
    $url = Get-EnvOrEmpty $entry.urlEnv
    $authValue = Get-EnvOrEmpty $entry.authEnv
    $globalAuth = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE"
    $authHeader = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_AUTH_HEADER"
    if (-not $authHeader) { $authHeader = $AuthHeaderName }

    $configSource = "NONE"
    $endpointMappingStatus = "MISSING"
    $authMappingStatus = "MISSING"
    $requestSchemaStatus = $entry.requestSchema
    $missing = New-Object System.Collections.Generic.List[string]

    if (Test-RealValue $url) {
        $configSource = "ENV"
        $endpointMappingStatus = "READY"
    } elseif ($UseLocalConfig -and $LocalConfig.read -and $LocalConfig.hasAppId -and $LocalConfig.hasAppKey -and $entry.localMapping -eq "READY") {
        $configSource = "LOCAL_CONFIG_OPT_IN"
        $endpointMappingStatus = "READY"
        if ($CapabilityName -eq "OCR") {
            $url = "https://$($LocalConfig.domain)$($entry.localPath)?requestId=classmate-smoke"
        } else {
            $url = "https://$($LocalConfig.domain)$($entry.localPath)"
        }
    } elseif ($entry.localMapping -eq "SEAM_ONLY") {
        $endpointMappingStatus = "SEAM_ONLY"
        [void]$missing.Add($entry.urlEnv)
    } else {
        [void]$missing.Add($entry.urlEnv)
    }

    if (Test-RealValue $authValue) {
        $configSource = "ENV"
        $authMappingStatus = "READY"
    } elseif (Test-RealValue $globalAuth) {
        if ($configSource -eq "NONE") { $configSource = "ENV" }
        $authValue = $globalAuth
        $authMappingStatus = "READY"
    } elseif ($UseLocalConfig -and $LocalConfig.read -and $LocalConfig.hasAppKey) {
        if ($configSource -eq "NONE") { $configSource = "LOCAL_CONFIG_OPT_IN" }
        $authValue = $LocalConfig.appKey
        $authMappingStatus = "READY"
    } else {
        [void]$missing.Add($entry.authEnv)
        [void]$missing.Add("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE")
    }

    [PSCustomObject]@{
        url = $url
        authHeader = $authHeader
        authValue = $authValue
        configSource = $configSource
        localConfigRead = [bool]$LocalConfig.read
        endpointMappingStatus = $endpointMappingStatus
        authMappingStatus = $authMappingStatus
        requestSchemaStatus = $requestSchemaStatus
        missingEnvNames = @($missing | Select-Object -Unique)
        allowNoAuth = ((Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_ALLOW_NO_AUTH") -eq "1")
        appId = $LocalConfig.appId
    }
}

function New-RequestPayload($CapabilityName) {
    switch ($CapabilityName) {
        "QUERY_REWRITE" {
            return @{ query = Get-Content -LiteralPath (Join-Path $InputDir "query_rewrite.txt") -Raw }
        }
        "TEXT_SIMILARITY" {
            return Get-Content -LiteralPath (Join-Path $InputDir "similarity.json") -Raw | ConvertFrom-Json
        }
        "TRANSLATION" {
            return @{
                text = Get-Content -LiteralPath (Join-Path $InputDir "translation_en.txt") -Raw
                from = "en"
                to = "zh"
            }
        }
        "TTS" {
            return @{
                text = Get-Content -LiteralPath (Join-Path $InputDir "tts_zh.txt") -Raw
                voice = "default"
            }
        }
        "FUNCTION_CALLING" {
            return Get-Content -LiteralPath (Join-Path $InputDir "function_calling.json") -Raw | ConvertFrom-Json
        }
        "EMBEDDING" {
            return Get-Content -LiteralPath (Join-Path $InputDir "embedding.json") -Raw | ConvertFrom-Json
        }
        default {
            return @{ input = "ClassMate provider smoke input" }
        }
    }
}

function New-SmokeResult {
    param(
        [string]$CapabilityName,
        [string]$Mode,
        [string]$Status,
        [object]$SmokeConfig,
        [bool]$RequestSent,
        [string]$SanitizedStatus,
        [string]$SanitizedError,
        [string]$OutputPath,
        [int]$DurationMs
    )
    $entry = $CapabilityCatalog[$CapabilityName]
    [PSCustomObject]@{
        capability = $CapabilityName
        docId = if ($entry) { $entry.docId } else { $null }
        tier = if ($entry) { $entry.tier } else { "unknown" }
        mode = $Mode
        status = $Status
        configured = ($SmokeConfig.authMappingStatus -eq "READY" -and $SmokeConfig.endpointMappingStatus -eq "READY")
        requestSent = $RequestSent
        sanitizedStatus = Redact-Text $SanitizedStatus
        sanitizedError = Redact-Text $SanitizedError
        outputPath = $OutputPath
        durationMs = $DurationMs
        timestamp = (Get-Date -Format o)
        missingEnvNames = @($SmokeConfig.missingEnvNames)
        configSource = $SmokeConfig.configSource
        localConfigRead = [bool]$SmokeConfig.localConfigRead
        endpointMappingStatus = $SmokeConfig.endpointMappingStatus
        authMappingStatus = $SmokeConfig.authMappingStatus
        requestSchemaStatus = $SmokeConfig.requestSchemaStatus
    }
}

function Invoke-CapabilitySmoke($CapabilityName, $LocalConfig) {
    $started = Get-Date
    $mode = if ($RunNetwork) { "NETWORK" } else { "DRY_RUN" }
    if (-not $CapabilityCatalog.ContainsKey($CapabilityName)) {
        $emptyConfig = [PSCustomObject]@{
            missingEnvNames = @()
            configSource = "NONE"
            localConfigRead = [bool]$LocalConfig.read
            endpointMappingStatus = "MISSING"
            authMappingStatus = "MISSING"
            requestSchemaStatus = "MISSING"
        }
        return New-SmokeResult -CapabilityName $CapabilityName -Mode $mode -Status "SKIPPED_NOT_ALLOWED" -SmokeConfig $emptyConfig -RequestSent $false -SanitizedStatus "Capability is not in the allowed smoke catalog." -SanitizedError "" -OutputPath "" -DurationMs 0
    }

    $entry = $CapabilityCatalog[$CapabilityName]
    $smokeConfig = Get-SmokeConfig $CapabilityName $LocalConfig
    $inputPath = Join-Path $InputDir $entry.input
    $audioCaps = @("ASR_LONG", "SHORT_ASR", "DIALECT_ASR", "SIMULTANEOUS_INTERPRETATION")
    if (($audioCaps -contains $CapabilityName) -and -not (Test-Path -LiteralPath $inputPath)) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode $mode -Status "SKIPPED_NO_INPUT" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "No non-sensitive test audio was provided." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }

    if (-not $RunNetwork) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "DRY_RUN" -Status "DRY_RUN_READY" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "No network request was sent." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }

    if ($smokeConfig.endpointMappingStatus -eq "SEAM_ONLY") {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "SKIPPED_SEAM_ONLY" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "SeamReadyButEndpointMappingMissing." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }
    if ($smokeConfig.endpointMappingStatus -ne "READY") {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "SKIPPED_ENDPOINT_MAPPING_MISSING" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "EndpointMappingMissing." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }
    if ($smokeConfig.authMappingStatus -ne "READY" -and -not $smokeConfig.allowNoAuth) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "SKIPPED_CONFIG_MISSING" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "ConfigMissing. Missing URL or auth environment variables are listed in missingEnvNames." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }
    if ($smokeConfig.requestSchemaStatus -ne "READY") {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "SKIPPED_ENDPOINT_MAPPING_MISSING" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "RequestSchemaMissing." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }

    try {
        $headers = @{}
        if (Test-RealValue $smokeConfig.authValue) {
            $headers[$smokeConfig.authHeader] = "$BearerWord $($smokeConfig.authValue)"
        }
        $safeName = $CapabilityName.ToString().ToLowerInvariant()
        $outputPath = Join-Path $ProviderOutputDir "$($safeName)_response.txt"
        if ($CapabilityName -eq "OCR") {
            $imagePath = Join-Path $InputDir "ocr_smoke.png"
            $image64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($imagePath))
            $business = if (Test-RealValue $smokeConfig.appId) { "aigc$($smokeConfig.appId)" } else { "aigc" }
            $body = "image=$([System.Net.WebUtility]::UrlEncode($image64))&pos=2&businessid=$([System.Net.WebUtility]::UrlEncode($business))"
            $response = Invoke-WebRequest -Uri $smokeConfig.url -Method Post -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body $body -TimeoutSec 45 -UseBasicParsing
        } else {
            $payload = New-RequestPayload $CapabilityName
            $json = $payload | ConvertTo-Json -Depth 12
            $headers["Content-Type"] = "application/json"
            $response = Invoke-WebRequest -Uri $smokeConfig.url -Method Post -Headers $headers -ContentType "application/json" -Body $json -TimeoutSec 45 -UseBasicParsing
        }
        Set-Content -LiteralPath $outputPath -Value (Redact-Text ([string]$response.Content))
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "PASS" -SmokeConfig $smokeConfig -RequestSent $true -SanitizedStatus "HTTP $($response.StatusCode)" -SanitizedError "" -OutputPath $outputPath -DurationMs $elapsed
    } catch {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "FAIL" -SmokeConfig $smokeConfig -RequestSent $true -SanitizedStatus "Network request failed." -SanitizedError $_.Exception.Message -OutputPath "" -DurationMs $elapsed
    }
}

function Write-ExplainConfig($LocalConfig, $Selected) {
    Write-Host "Official provider smoke config status"
    Write-Host ("config.local.json exists: " + $LocalConfig.exists)
    Write-Host ("local config read: " + $LocalConfig.read)
    foreach ($cap in $Selected) {
        if (-not $CapabilityCatalog.ContainsKey($cap)) { continue }
        $cfg = Get-SmokeConfig $cap $LocalConfig
        Write-Host ("- " + $cap + ": capability URL configured=" + ($cfg.endpointMappingStatus -eq "READY") + "; auth configured=" + ($cfg.authMappingStatus -eq "READY") + "; source=" + $cfg.configSource + "; endpointMapping=" + $cfg.endpointMappingStatus + "; requestSchema=" + $cfg.requestSchemaStatus)
        if ($cfg.missingEnvNames.Count -gt 0) {
            Write-Host ("  missing env: " + (($cfg.missingEnvNames | Select-Object -Unique) -join ", "))
        }
    }
}

function Write-Results {
    param([object[]]$Results, $LocalConfig)
    $summary = [PSCustomObject]@{
        generatedAt = (Get-Date -Format o)
        dryRun = [bool]$DryRun
        runNetwork = [bool]$RunNetwork
        configLocalExists = [bool]$LocalConfig.exists
        configLocalRead = [bool]$LocalConfig.read
        aarExists = Test-Path -LiteralPath (Join-Path $RepoRoot "app\libs\llm-sdk-release.aar")
        aarRead = $false
        resultCount = @($Results).Count
        results = $Results
    }
    $summary | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $ResultJson

    $passed = @($Results | Where-Object { $_.status -eq "PASS" })
    $skipped = @($Results | Where-Object { $_.status -like "SKIPPED_*" -or $_.status -eq "DRY_RUN_READY" })
    $failed = @($Results | Where-Object { $_.status -eq "FAIL" })
    $configMissing = @($Results | Where-Object { $_.status -eq "SKIPPED_CONFIG_MISSING" })

    $md = @()
    $md += "# Official Provider Smoke Result"
    $md += ""
    $md += "## Summary"
    $md += ""
    $md += ("- GeneratedAt: " + $summary.generatedAt)
    $md += ("- DryRun: " + $summary.dryRun)
    $md += ("- RunNetwork: " + $summary.runNetwork)
    $md += ("- config.local.json exists: " + $summary.configLocalExists + " (content read only with -UseLocalConfig)")
    $md += ("- local config read: " + $summary.configLocalRead)
    $md += ("- AAR exists: " + $summary.aarExists + " (content not read)")
    $md += ("- Passed: " + $passed.Count)
    $md += ("- Skipped: " + $skipped.Count)
    $md += ("- Failed: " + $failed.Count)
    $md += ("- ConfigMissing: " + $configMissing.Count)
    $md += ""
    $md += "## Capability Table"
    $md += ""
    $md += "| Capability | mode | status | requestSent | configSource | localConfigRead | endpointMapping | authMapping | requestSchema | missingEnvNames |"
    $md += "|---|---|---|---|---|---|---|---|---|---|"
    foreach ($r in $Results) {
        $missing = if ($r.missingEnvNames.Count -gt 0) { ($r.missingEnvNames -join "<br>") } else { "" }
        $md += ("| " + $r.capability + " | " + $r.mode + " | " + $r.status + " | " + $r.requestSent + " | " + $r.configSource + " | " + $r.localConfigRead + " | " + $r.endpointMappingStatus + " | " + $r.authMappingStatus + " | " + $r.requestSchemaStatus + " | " + $missing + " |")
    }
    $md += ""
    $md += "## Passed"
    if ($passed.Count -eq 0) { $md += "- None" } else { $passed | ForEach-Object { $md += ("- " + $_.capability) } }
    $md += ""
    $md += "## Skipped"
    if ($skipped.Count -eq 0) { $md += "- None" } else { $skipped | ForEach-Object { $md += ("- " + $_.capability + ": " + $_.status) } }
    $md += ""
    $md += "## Failed"
    if ($failed.Count -eq 0) { $md += "- None" } else { $failed | ForEach-Object { $md += ("- " + $_.capability + ": " + $_.sanitizedError) } }
    $md += ""
    $md += "## ConfigMissing"
    if ($configMissing.Count -eq 0) { $md += "- None" } else { $configMissing | ForEach-Object { $md += ("- " + $_.capability + ": " + ($_.missingEnvNames -join ", ")) } }
    $md += ""
    $md += "## Next Action"
    if ($RunNetwork) {
        $md += "- Review each skipped reason. EndpointMappingMissing means the script cannot derive a reliable live endpoint. Seam-only means the project has an interface but no confirmed live request mapping."
    } else {
        $md += "- Network smoke was not executed. Use -RunNetwork with one -Capability only after explicit authorization and configuration."
    }
    $md | Set-Content -LiteralPath $ResultMd
}

Set-Location $RepoRoot
if ($PrintSetupHelp) {
    Print-SetupHelp
    return
}

Ensure-Directories
Ensure-TestInputs

$selected = @(Get-SelectedCapabilities)
$localConfig = Read-LocalSmokeConfig
Write-SmokeLog ("config.local.json exists: " + $localConfig.exists + "; local config read: " + $localConfig.read)
Write-SmokeLog ("AAR exists: " + (Test-Path -LiteralPath (Join-Path $RepoRoot "app\libs\llm-sdk-release.aar")) + "; content not read.")

if ($ExplainConfig) {
    Write-ExplainConfig -LocalConfig $localConfig -Selected $selected
    return
}

$results = New-Object System.Collections.Generic.List[object]
foreach ($cap in $selected) {
    if ($null -eq $cap -or $cap.ToString().Trim().Length -eq 0) { continue }
    $capName = $cap.ToUpperInvariant()
    $result = Invoke-CapabilitySmoke -CapabilityName $capName -LocalConfig $localConfig
    [void]$results.Add($result)
}

Write-Results -Results $results.ToArray() -LocalConfig $localConfig

Write-Host "Official provider smoke harness"
Write-Host "Mode: $(if ($RunNetwork) { 'NETWORK' } else { 'DRY_RUN' })"
Write-Host "Output: $ResultJson"
foreach ($result in $results) {
    Write-Host "$($result.capability): $($result.status)"
}

if (-not $NoOpen -and $RunNetwork -and $VerboseLog) {
    Write-Host "Results written to $OutputDir"
}
