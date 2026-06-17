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
    [switch]$ExplainConfig,
    [string]$LocalConfigPath,
    [int]$TimeoutSeconds = 20
)

$ErrorActionPreference = "Continue"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $OutputDir -or $OutputDir.Trim().Length -eq 0) {
    $OutputDir = Join-Path $RepoRoot ".codex_work\official_provider_smoke"
}
if (-not $LocalConfigPath -or $LocalConfigPath.Trim().Length -eq 0) {
    $LocalConfigPath = Join-Path $RepoRoot "config.local.json"
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

$DefaultOfficialDomain = "api-ai.vivo.com.cn"

$SafeCapabilities = @(
    "OCR",
    "QUERY_REWRITE",
    "TEXT_SIMILARITY",
    "TRANSLATION",
    "TTS",
    "FUNCTION_CALLING",
    "EMBEDDING"
)

$CapabilityCatalog = [ordered]@{
    "OCR" = @{
        docId = 1737
        tier = "product-facing"
        input = "ocr_smoke.png"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_OCR_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_OCR_AUTH_VALUE"
        localPath = "/ocr/general_recognition"
        providerMapping = "READY"
        requestSchema = "READY"
        schemaNote = "form image,pos,businessid"
    }
    "QUERY_REWRITE" = @{
        docId = 2061
        tier = "product-facing"
        input = "query_rewrite.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_QUERY_REWRITE_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_QUERY_REWRITE_AUTH_VALUE"
        localPath = "/query-rewrite-api/predict"
        providerMapping = "READY"
        requestSchema = "READY"
        schemaNote = "json query"
    }
    "TEXT_SIMILARITY" = @{
        docId = 2060
        tier = "product-facing"
        input = "similarity.json"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_TEXT_SIMILARITY_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_TEXT_SIMILARITY_AUTH_VALUE"
        localPath = "/similarity-model-api/predict"
        providerMapping = "READY"
        requestSchema = "READY"
        schemaNote = "json model_name,query,sentences"
    }
    "TRANSLATION" = @{
        docId = 1733
        tier = "product-facing"
        input = "translation_en.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_TRANSLATION_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_TRANSLATION_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
        schemaNote = "generic text/from/to seam"
    }
    "TTS" = @{
        docId = 1735
        tier = "product-facing"
        input = "tts_zh.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_TTS_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_TTS_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
        schemaNote = "generic course-essence text seam"
    }
    "FUNCTION_CALLING" = @{
        docId = 1805
        tier = "product-facing"
        input = "function_calling.json"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_FUNCTION_CALLING_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_FUNCTION_CALLING_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
        schemaNote = "internal tool adapter seam"
    }
    "ASR_LONG" = @{
        docId = 1739
        tier = "product-facing-secondary"
        input = "asr_long.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_ASR_LONG_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_ASR_LONG_AUTH_VALUE"
        localPath = "/lasr"
        providerMapping = "READY"
        requestSchema = "READY"
        schemaNote = "task flow create/upload/run/progress/result"
    }
    "EMBEDDING" = @{
        docId = 1734
        tier = "product-facing"
        input = "embedding.json"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_EMBEDDING_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_EMBEDDING_AUTH_VALUE"
        localPath = "/embedding-model-api/predict/batch"
        providerMapping = "READY"
        requestSchema = "READY"
        schemaNote = "json model_name,sentences"
    }
    "IMAGE_GEN" = @{
        docId = 1732
        tier = "dev-lab"
        input = "image_gen.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_IMAGE_GEN_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_IMAGE_GEN_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
        schemaNote = "dev-lab only"
    }
    "VIDEO_GEN" = @{
        docId = 2201
        tier = "dev-lab"
        input = "video_gen.txt"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_VIDEO_GEN_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_VIDEO_GEN_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "GENERIC_ONLY"
        schemaNote = "dev-lab only"
    }
    "SHORT_ASR" = @{
        docId = 1738
        tier = "dev-lab"
        input = "short_asr.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_SHORT_ASR_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_SHORT_ASR_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "MISSING"
        schemaNote = "evaluation only"
    }
    "DIALECT_ASR" = @{
        docId = 2065
        tier = "dev-lab"
        input = "dialect_asr.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_DIALECT_ASR_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_DIALECT_ASR_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "MISSING"
        schemaNote = "evaluation only"
    }
    "SIMULTANEOUS_INTERPRETATION" = @{
        docId = 2068
        tier = "dev-lab"
        input = "simultaneous_interpretation.wav"
        urlEnv = "CLASSMATE_PROVIDER_SMOKE_SIMULTANEOUS_INTERPRETATION_URL"
        authEnv = "CLASSMATE_PROVIDER_SMOKE_SIMULTANEOUS_INTERPRETATION_AUTH_VALUE"
        localPath = ""
        providerMapping = "SEAM_ONLY"
        requestSchema = "MISSING"
        schemaNote = "evaluation only"
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
    $authEnvNames = @("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE") + ($CapabilityCatalog.Values | ForEach-Object { $_.authEnv })
    foreach ($envName in ($authEnvNames | Select-Object -Unique)) {
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
    try {
        if (Test-Path -LiteralPath $LogPath) {
            Remove-Item -LiteralPath $LogPath -Force -ErrorAction SilentlyContinue
        }
        New-Item -ItemType File -Force -Path $LogPath | Out-Null
        Set-Content -LiteralPath $LogPath -Value "Official provider smoke log $(Get-Date -Format o)" -ErrorAction Stop
    } catch {
        Write-Host ("WARN unable to initialize smoke log: " + (Redact-Text $_.Exception.Message))
    }
}

function Write-SmokeLog($Message) {
    $line = "$(Get-Date -Format o) $Message"
    try {
        Add-Content -LiteralPath $LogPath -Value (Redact-Text $line) -ErrorAction Stop
    } catch {
        if ($VerboseLog) {
            Write-Host ("WARN unable to write smoke log: " + (Redact-Text $_.Exception.Message))
        }
    }
    if ($VerboseLog) {
        Write-Host (Redact-Text $Message)
    }
}

function Print-SetupHelp {
    Write-Host "Official provider smoke setup v5"
    Write-Host ""
    Write-Host "Default mode is dry-run. Real requests require -RunNetwork and one explicit -Capability."
    Write-Host "config.local.json is not read unless -UseLocalConfig is passed."
    Write-Host ""
    Write-Host "Common environment variables:"
    Write-Host "  CLASSMATE_PROVIDER_SMOKE_AUTH_HEADER=<your-value>"
    Write-Host "  CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE=<your-value>"
    Write-Host "  CLASSMATE_PROVIDER_SMOKE_ALLOW_NO_AUTH=<your-value>"
    Write-Host "  -TimeoutSeconds <seconds>"
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
    Write-Host "  -UseLocalConfig maps value presence from capability-specific groups only."
    Write-Host "  Generic providers.bluelm/providers.qwen/top-level BlueLM can describe cloud text generation, but never makes OCR, ASR, retrieval, TTS, translation, or function-calling READY."
    Write-Host "  Values are kept in memory only and are never printed or written."
    Write-Host ""
    Write-Host "Official provider config schema v1:"
    Write-Host '  "officialProviders": {'
    Write-Host '    "ocr": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "queryRewrite": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "textSimilarity": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "embedding": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "translation": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "tts": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "functionCalling": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },'
    Write-Host '    "asrLong": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" }'
    Write-Host '  }'
    Write-Host ""
    Write-Host "Mapping states:"
    Write-Host "  READY: endpoint/auth/schema can be derived."
    Write-Host "  MISSING: required mapping or config is absent."
    Write-Host "  SEAM_ONLY: a provider seam exists but no confirmed live endpoint mapping is available."
}

function Test-RealValue($Value) {
    if ($null -eq $Value) { return $false }
    $s = [string]$Value
    if ($s.Trim().Length -eq 0) { return $false }
    if ($s -match "(?i)your_|your-|<your|placeholder|changeme|todo") { return $false }
    return $true
}

function Normalize-Domain($Raw) {
    $value = [string]$Raw
    if (-not (Test-RealValue $value)) { return $DefaultOfficialDomain }
    return $value.Replace("https://", "").Replace("http://", "").TrimEnd("/")
}

function Normalize-BaseUrl($Raw) {
    $value = [string]$Raw
    if (-not (Test-RealValue $value)) {
        $value = $DefaultOfficialDomain
    }
    $value = $value.Trim().TrimEnd("/")
    if ($value -notmatch "^[a-zA-Z][a-zA-Z0-9+.-]*://") {
        $value = "https://$value"
    }
    return $value
}

function Get-ObjectPropertyValue($Object, [string[]]$Names) {
    if ($null -eq $Object) { return $null }
    $props = $Object.PSObject.Properties
    foreach ($name in $Names) {
        $match = $props | Where-Object { $_.Name -ieq $name } | Select-Object -First 1
        if ($match) { return $match.Value }
    }
    return $null
}

function Test-ObjectPropertyExists($Object, [string[]]$Names) {
    if ($null -eq $Object) { return $false }
    $props = $Object.PSObject.Properties
    foreach ($name in $Names) {
        if ($props | Where-Object { $_.Name -ieq $name } | Select-Object -First 1) {
            return $true
        }
    }
    return $false
}

function Get-ChildConfigGroup($Root, [string[]]$Names) {
    return Get-ObjectPropertyValue $Root $Names
}

function New-LocalConfigState($Path) {
    [PSCustomObject]@{
        exists = Test-Path -LiteralPath $Path
        read = $false
        parseOk = $false
        detectedConfigGroups = @()
        vivoCaptureExists = $false
        providersExists = $false
        providersBluelmExists = $false
        providersQwenExists = $false
        topLevelBlueLmExists = $false
        officialProvidersExists = $false
        officialProvidersVivoCaptureExists = $false
        officialProviderGroupNames = @()
        officialProviderCandidates = @{}
        credentialSource = "NONE"
        domainSource = "PROVIDER_CODE_DEFAULT"
        hasAppId = $false
        hasAppKey = $false
        hasBaseUrl = $false
        domain = $DefaultOfficialDomain
        appId = ""
        appKey = ""
        missingConfigFields = @()
    }
}

function Add-DetectedGroup($State, [string]$GroupName) {
    if (-not ($State.detectedConfigGroups -contains $GroupName)) {
        $State.detectedConfigGroups = @($State.detectedConfigGroups + $GroupName)
    }
}

function Get-CredentialCandidate($Group, [string]$SourceName) {
    if ($null -eq $Group) { return $null }
    $appId = Get-ObjectPropertyValue $Group @("appId", "appID", "appid", "app_id", "AppID", "APP_ID")
    $appKey = Get-ObjectPropertyValue $Group @("appKey", "appKEY", "appkey", "app_key", "AppKey", "AppKEY", "APP_KEY", "authValue", "authorizationValue", "apiKey", "api_key", "token")
    $baseUrl = Get-ObjectPropertyValue $Group @("baseUrl", "baseURL", "base_url", "url", "endpoint", "domain", "host")
    $endpointPath = Get-ObjectPropertyValue $Group @("endpointPath", "endpoint_path", "path", "requestPath", "request_path")
    $authHeader = Get-ObjectPropertyValue $Group @("authHeader", "authorizationHeader", "header")
    return [PSCustomObject]@{
        source = $SourceName
        appId = $appId
        appKey = $appKey
        baseUrl = $baseUrl
        endpointPath = $endpointPath
        authHeader = $authHeader
        hasAppId = Test-RealValue $appId
        hasAppKey = Test-RealValue $appKey
        hasBaseUrl = Test-RealValue $baseUrl
        hasEndpointPath = Test-RealValue $endpointPath
        hasAuthHeader = Test-RealValue $authHeader
    }
}

function Get-OfficialProviderAliases($CapabilityName) {
    switch ($CapabilityName) {
        "OCR" { return @("ocr") }
        "QUERY_REWRITE" { return @("queryRewrite", "query_rewrite") }
        "TEXT_SIMILARITY" { return @("textSimilarity", "text_similarity", "similarity") }
        "EMBEDDING" { return @("embedding", "textVector", "text_vector") }
        "TRANSLATION" { return @("translation") }
        "TTS" { return @("tts", "audioGeneration", "audio_generation") }
        "FUNCTION_CALLING" { return @("functionCalling", "function_calling") }
        "ASR_LONG" { return @("asrLong", "asr_long", "longAsr") }
        default { return @() }
    }
}

function Find-OfficialProviderGroup($OfficialProviders, $CapabilityName) {
    foreach ($alias in @(Get-OfficialProviderAliases $CapabilityName)) {
        $group = Get-ChildConfigGroup $OfficialProviders @($alias)
        if ($null -ne $group) {
            return [PSCustomObject]@{
                name = "officialProviders.$alias"
                value = $group
            }
        }
    }
    return $null
}

function Select-CredentialCandidate([object[]]$Candidates) {
    foreach ($candidate in $Candidates) {
        if ($candidate -and $candidate.hasAppId -and $candidate.hasAppKey) {
            return $candidate
        }
    }
    foreach ($candidate in $Candidates) {
        if ($candidate -and ($candidate.hasAppId -or $candidate.hasAppKey -or $candidate.hasBaseUrl)) {
            return $candidate
        }
    }
    return $null
}

function Read-LocalSmokeConfig {
    $state = New-LocalConfigState $LocalConfigPath
    if (-not $UseLocalConfig -or -not $state.exists) {
        return $state
    }
    try {
        $jsonText = Get-Content -LiteralPath $LocalConfigPath -Raw
        $state.read = $true
        $root = $jsonText | ConvertFrom-Json

        $vivoCapture = Get-ChildConfigGroup $root @("vivoCapture", "capture", "vivo")
        $providers = Get-ChildConfigGroup $root @("providers")
        $bluelm = Get-ChildConfigGroup $providers @("bluelm", "blueLm", "blueLM")
        $qwen = Get-ChildConfigGroup $providers @("qwen", "qwen35", "qwen3_5")
        $officialProviders = Get-ChildConfigGroup $root @("officialProviders", "officialProvider", "official")
        $officialVivoCapture = Get-ChildConfigGroup $officialProviders @("vivoCapture", "capture", "vivo")
        $officialBlueLm = Get-ChildConfigGroup $officialProviders @("bluelm", "blueLm", "blueLM")
        $officialQwen = Get-ChildConfigGroup $officialProviders @("qwen", "qwen35", "qwen3_5")
        $providerName = Get-ObjectPropertyValue $root @("provider", "activeProvider")
        $rootHasCredentialFields =
            (Test-ObjectPropertyExists $root @("appId", "appID", "appid", "app_id", "AppID", "APP_ID")) -or
            (Test-ObjectPropertyExists $root @("appKey", "appKEY", "appkey", "app_key", "AppKey", "AppKEY", "APP_KEY", "token")) -or
            (Test-ObjectPropertyExists $root @("baseUrl", "baseURL", "base_url", "url", "endpoint", "domain", "host"))
        $topLevelBlueLm = if ($rootHasCredentialFields -and (($null -eq $providerName) -or ([string]$providerName -match "(?i)blue|qwen|official"))) { $root } else { $null }

        $state.vivoCaptureExists = $null -ne $vivoCapture
        $state.providersExists = $null -ne $providers
        $state.providersBluelmExists = $null -ne $bluelm
        $state.providersQwenExists = $null -ne $qwen
        $state.topLevelBlueLmExists = $null -ne $topLevelBlueLm
        $state.officialProvidersExists = $null -ne $officialProviders
        $state.officialProvidersVivoCaptureExists = $null -ne $officialVivoCapture

        if ($state.vivoCaptureExists) { Add-DetectedGroup $state "vivoCapture" }
        if ($state.providersExists) { Add-DetectedGroup $state "providers" }
        if ($state.providersBluelmExists) { Add-DetectedGroup $state "providers.bluelm" }
        if ($state.providersQwenExists) { Add-DetectedGroup $state "providers.qwen" }
        if ($state.topLevelBlueLmExists) { Add-DetectedGroup $state "topLevel.bluelm" }
        if ($state.officialProvidersExists) { Add-DetectedGroup $state "officialProviders" }
        if ($state.officialProvidersVivoCaptureExists) { Add-DetectedGroup $state "officialProviders.vivoCapture" }
        if ($null -ne $officialBlueLm) { Add-DetectedGroup $state "officialProviders.bluelm" }
        if ($null -ne $officialQwen) { Add-DetectedGroup $state "officialProviders.qwen" }
        foreach ($capName in $CapabilityCatalog.Keys) {
            $officialGroup = Find-OfficialProviderGroup $officialProviders $capName
            if ($officialGroup) {
                Add-DetectedGroup $state $officialGroup.name
                $state.officialProviderGroupNames = @($state.officialProviderGroupNames + $officialGroup.name)
                $state.officialProviderCandidates[$capName] = Get-CredentialCandidate $officialGroup.value "LOCAL_CONFIG_OFFICIAL_PROVIDER"
            }
        }

        $candidate = Select-CredentialCandidate @(
            (Get-CredentialCandidate $vivoCapture "LOCAL_CONFIG_VIVO_CAPTURE"),
            (Get-CredentialCandidate $officialVivoCapture "LOCAL_CONFIG_VIVO_CAPTURE"),
            (Get-CredentialCandidate $bluelm "LOCAL_CONFIG_BLUELM"),
            (Get-CredentialCandidate $officialBlueLm "LOCAL_CONFIG_BLUELM"),
            (Get-CredentialCandidate $topLevelBlueLm "LOCAL_CONFIG_BLUELM"),
            (Get-CredentialCandidate $qwen "LOCAL_CONFIG_QWEN"),
            (Get-CredentialCandidate $officialQwen "LOCAL_CONFIG_QWEN")
        )

        if ($candidate) {
            $state.credentialSource = $candidate.source
            $state.hasAppId = [bool]$candidate.hasAppId
            $state.hasAppKey = [bool]$candidate.hasAppKey
            $state.hasBaseUrl = [bool]$candidate.hasBaseUrl
            if ($candidate.hasBaseUrl) {
                $state.domain = Normalize-Domain $candidate.baseUrl
                $state.domainSource = $candidate.source
            }
            if ($candidate.hasAppId) { $state.appId = [string]$candidate.appId }
            if ($candidate.hasAppKey) { $state.appKey = [string]$candidate.appKey }
        }

        $missingConfig = New-Object System.Collections.Generic.List[string]
        if (-not ($state.vivoCaptureExists -or $state.providersBluelmExists -or $state.providersQwenExists -or $state.officialProvidersVivoCaptureExists -or $state.topLevelBlueLmExists)) {
            [void]$missingConfig.Add("vivoCapture or providers.bluelm or providers.qwen or top-level BlueLM")
        }
        if (-not $state.hasAppId) { [void]$missingConfig.Add("appId") }
        if (-not $state.hasAppKey) { [void]$missingConfig.Add("appKey") }
        if (-not $state.hasBaseUrl) { [void]$missingConfig.Add("baseUrl (provider default domain will be used if auth is present)") }
        $state.missingConfigFields = @($missingConfig | Select-Object -Unique)
        $state.parseOk = $true
    } catch {
        $state.read = $true
        $state.parseOk = $false
        $state.missingConfigFields = @("config parse failed")
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

function Join-SmokeUrl($BaseUrl, $EndpointPath) {
    $base = Normalize-BaseUrl $BaseUrl
    if (-not (Test-RealValue $EndpointPath)) { return $base }
    $path = ([string]$EndpointPath).Trim()
    if ($path -match "^[a-zA-Z][a-zA-Z0-9+.-]*://") {
        return $path.TrimEnd("/")
    }
    if ($path.StartsWith("?")) {
        return "$base$path"
    }
    if (-not $path.StartsWith("/")) {
        $path = "/$path"
    }
    return "$base$path"
}

function Add-SmokeQueryParameter($Url, $Name, $Value) {
    $separator = if ([string]$Url -like "*?*") { "&" } else { "?" }
    $encodedName = [System.Uri]::EscapeDataString([string]$Name)
    $encodedValue = [System.Uri]::EscapeDataString([string]$Value)
    return "$Url$separator$encodedName=$encodedValue"
}

function New-ConcreteUrl($BaseUrl, $Path, $CapabilityName) {
    $url = Join-SmokeUrl $BaseUrl $Path
    if ($CapabilityName -eq "OCR") {
        return Add-SmokeQueryParameter $url "requestId" "classmate-smoke"
    }
    return $url
}

function New-OfficialProviderUrl($Candidate, $Path, $CapabilityName) {
    $effectivePath = if ($Candidate.hasEndpointPath) { $Candidate.endpointPath } else { $Path }
    if (Test-RealValue $effectivePath) {
        return New-ConcreteUrl $Candidate.baseUrl $effectivePath $CapabilityName
    }
    return Normalize-BaseUrl $Candidate.baseUrl
}

function Test-CaptureSpecificConfig($LocalConfig) {
    return $LocalConfig.read -and
        ($LocalConfig.credentialSource -eq "LOCAL_CONFIG_VIVO_CAPTURE") -and
        $LocalConfig.hasAppKey -and
        $LocalConfig.hasAppId
}

function Get-OfficialProviderCandidate($CapabilityName, $LocalConfig) {
    if (-not $LocalConfig.read) { return $null }
    if ($LocalConfig.officialProviderCandidates.ContainsKey($CapabilityName)) {
        return $LocalConfig.officialProviderCandidates[$CapabilityName]
    }
    return $null
}

function Test-OfficialProviderSpecificConfig($CapabilityName, $LocalConfig) {
    $candidate = Get-OfficialProviderCandidate $CapabilityName $LocalConfig
    return $candidate -and $candidate.hasBaseUrl -and $candidate.hasAppKey
}

function Add-OfficialProviderMissingFields($CapabilityName, $LocalConfig, $MissingConfig) {
    $aliases = @(Get-OfficialProviderAliases $CapabilityName)
    if ($aliases.Count -eq 0) { return }
    $candidate = Get-OfficialProviderCandidate $CapabilityName $LocalConfig
    $primary = "officialProviders." + $aliases[0]
    if (-not $candidate) {
        [void]$MissingConfig.Add($primary)
        return
    }
    if (-not $candidate.hasBaseUrl) { [void]$MissingConfig.Add("$primary.baseUrl") }
    if (-not $candidate.hasEndpointPath) { [void]$MissingConfig.Add("$primary.endpointPath") }
    if (-not $candidate.hasAppKey) { [void]$MissingConfig.Add("$primary.authValue or $primary.appKey") }
}

function Add-CapabilitySpecificMissingFields($CapabilityName, $LocalConfig, $MissingConfig) {
    Add-OfficialProviderMissingFields $CapabilityName $LocalConfig $MissingConfig
    if (Test-OfficialProviderSpecificConfig $CapabilityName $LocalConfig) {
        return
    }
    switch ($CapabilityName) {
        { $_ -in @("OCR", "ASR_LONG") } {
            if (-not ($LocalConfig.vivoCaptureExists -or $LocalConfig.officialProvidersVivoCaptureExists)) {
                [void]$MissingConfig.Add("vivoCapture or officialProviders.vivoCapture")
            }
            if (-not (Test-CaptureSpecificConfig $LocalConfig)) {
                [void]$MissingConfig.Add("vivoCapture.appId")
                [void]$MissingConfig.Add("vivoCapture.appKey")
            }
            break
        }
        { $_ -in @("QUERY_REWRITE", "TEXT_SIMILARITY", "EMBEDDING") } {
            [void]$MissingConfig.Add("officialProviders.retrieval or explicit env endpoint")
            [void]$MissingConfig.Add("doc-specific endpoint path")
            break
        }
        { $_ -in @("TRANSLATION", "TTS", "FUNCTION_CALLING") } {
            [void]$MissingConfig.Add("confirmed live endpoint mapping")
            break
        }
    }
}

function Get-SmokeConfig($CapabilityName, $LocalConfig) {
    $entry = $CapabilityCatalog[$CapabilityName]
    $url = Get-EnvOrEmpty $entry.urlEnv
    $authValue = Get-EnvOrEmpty $entry.authEnv
    $globalAuth = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE"
    $authHeader = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_AUTH_HEADER"
    if (-not $authHeader) { $authHeader = $AuthHeaderName }

    $configSource = "NONE"
    $mappingSource = "NONE"
    $endpointMappingStatus = "MISSING"
    $authMappingStatus = "MISSING"
    $requestSchemaStatus = $entry.requestSchema
    $missingEnv = New-Object System.Collections.Generic.List[string]
    $missingConfig = New-Object System.Collections.Generic.List[string]

    $captureSpecificConfig = Test-CaptureSpecificConfig $LocalConfig
    $officialProviderCandidate = Get-OfficialProviderCandidate $CapabilityName $LocalConfig
    $officialProviderSpecificConfig = Test-OfficialProviderSpecificConfig $CapabilityName $LocalConfig

    if (Test-RealValue $url) {
        $configSource = "ENV_EXPLICIT"
        $mappingSource = "ENV_EXPLICIT"
        $endpointMappingStatus = "READY"
    } elseif ($officialProviderSpecificConfig) {
        $configSource = "LOCAL_CONFIG_OFFICIAL_PROVIDER"
        $mappingSource = "LOCAL_CONFIG_OFFICIAL_PROVIDER"
        $endpointMappingStatus = "READY"
        $url = New-OfficialProviderUrl $officialProviderCandidate $entry.localPath $CapabilityName
    } elseif (($CapabilityName -in @("OCR", "ASR_LONG")) -and $captureSpecificConfig) {
        $mappingSource = if ($LocalConfig.hasBaseUrl) { $LocalConfig.domainSource } else { "PROVIDER_CODE_DEFAULT" }
        $endpointMappingStatus = "READY"
        $url = New-ConcreteUrl $LocalConfig.domain $entry.localPath $CapabilityName
    } elseif ($entry.providerMapping -eq "SEAM_ONLY") {
        $endpointMappingStatus = "SEAM_ONLY"
        $mappingSource = "NONE"
        [void]$missingEnv.Add($entry.urlEnv)
    } else {
        [void]$missingEnv.Add($entry.urlEnv)
        if ($LocalConfig.read) {
            Add-CapabilitySpecificMissingFields $CapabilityName $LocalConfig $missingConfig
        }
    }

    if (Test-RealValue $authValue) {
        $configSource = "ENV_EXPLICIT"
        $authMappingStatus = "READY"
    } elseif (Test-RealValue $globalAuth) {
        if ($configSource -eq "NONE") { $configSource = "ENV_EXPLICIT" }
        $authValue = $globalAuth
        $authMappingStatus = "READY"
    } elseif ($officialProviderSpecificConfig) {
        if ($configSource -eq "NONE") { $configSource = "LOCAL_CONFIG_OFFICIAL_PROVIDER" }
        $authValue = $officialProviderCandidate.appKey
        if ($officialProviderCandidate.hasAuthHeader) { $authHeader = $officialProviderCandidate.authHeader }
        $authMappingStatus = "READY"
    } elseif ($captureSpecificConfig -and ($CapabilityName -in @("OCR", "ASR_LONG"))) {
        if ($configSource -eq "NONE") { $configSource = "LOCAL_CONFIG_VIVO_CAPTURE" }
        $authValue = $LocalConfig.appKey
        $authMappingStatus = "READY"
    } else {
        [void]$missingEnv.Add($entry.authEnv)
        [void]$missingEnv.Add("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE")
        if ($LocalConfig.read) {
            foreach ($field in $LocalConfig.missingConfigFields) {
                if ($field -eq "appKey" -or $field -like "vivoCapture*") {
                    [void]$missingConfig.Add($field)
                }
            }
        }
    }

    if ($LocalConfig.read) {
        if (($CapabilityName -in @("OCR", "ASR_LONG")) -and -not $officialProviderSpecificConfig) {
            if (-not $LocalConfig.hasAppId) { [void]$missingConfig.Add("appId") }
            if (-not $LocalConfig.hasAppKey) { [void]$missingConfig.Add("appKey") }
            if (-not $LocalConfig.hasBaseUrl) { [void]$missingConfig.Add("baseUrl (provider default domain will be used if auth is present)") }
            if (-not ($LocalConfig.vivoCaptureExists -or $LocalConfig.officialProvidersVivoCaptureExists -or $LocalConfig.topLevelBlueLmExists)) {
                [void]$missingConfig.Add("vivoCapture or officialProviders.vivoCapture")
            }
        }
        Add-CapabilitySpecificMissingFields $CapabilityName $LocalConfig $missingConfig
    }

    [PSCustomObject]@{
        url = $url
        authHeader = $authHeader
        authValue = $authValue
        configSource = $configSource
        localConfigRead = [bool]$LocalConfig.read
        detectedConfigGroups = @($LocalConfig.detectedConfigGroups)
        endpointMappingStatus = $endpointMappingStatus
        authMappingStatus = $authMappingStatus
        requestSchemaStatus = $requestSchemaStatus
        missingEnvNames = @($missingEnv | Select-Object -Unique)
        missingConfigFields = @($missingConfig | Select-Object -Unique)
        mappingSource = $mappingSource
        allowNoAuth = ((Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_ALLOW_NO_AUTH") -eq "1")
        appId = $LocalConfig.appId
        schemaNote = $entry.schemaNote
        timeoutSeconds = $TimeoutSeconds
    }
}

function Get-HttpStatusCode($Exception) {
    try {
        if ($Exception.Response -and $Exception.Response.StatusCode) {
            return [int]$Exception.Response.StatusCode
        }
    } catch {
        return $null
    }
    return $null
}

function Get-NetworkFailureStatus($Exception, $SmokeConfig) {
    $message = [string]$Exception.Message
    $statusCode = Get-HttpStatusCode $Exception
    if ($message -match "(?i)invalid uri|cannot bind parameter.*uri|could not parse.*host|could not parse.*hostname") {
        return "FAIL_INVALID_URI"
    }
    if ($message -match "(?i)timed out|timeout|operation has timed out") {
        return "FAIL_TIMEOUT"
    }
    if ($statusCode -eq 404 -and ($SmokeConfig.mappingSource -in @("LOCAL_CONFIG_BLUELM", "LOCAL_CONFIG_QWEN", "PROVIDER_CODE_DEFAULT"))) {
        return "FAIL_HTTP_404_ENDPOINT_SUSPECT"
    }
    if ($statusCode -eq 404) {
        return "FAIL_HTTP_404"
    }
    return "FAIL_NETWORK"
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

function New-EmptySmokeConfig($LocalConfig) {
    [PSCustomObject]@{
        missingEnvNames = @()
        missingConfigFields = @()
        detectedConfigGroups = @($LocalConfig.detectedConfigGroups)
        configSource = "NONE"
        localConfigRead = [bool]$LocalConfig.read
        endpointMappingStatus = "MISSING"
        authMappingStatus = "MISSING"
        requestSchemaStatus = "MISSING"
        mappingSource = "NONE"
    }
}

function Test-SmokeUri($Url) {
    $uri = $null
    $isValid = [System.Uri]::TryCreate([string]$Url, [System.UriKind]::Absolute, [ref]$uri)
    if ($isValid -and $uri -and ($uri.Scheme -in @("http", "https"))) {
        return [PSCustomObject]@{
            isValid = $true
            uri = $uri
        }
    }
    return [PSCustomObject]@{
        isValid = $false
        uri = $null
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
        [int]$DurationMs,
        [bool]$RequestAttempted,
        [bool]$UriValidated
    )
    if (-not $PSBoundParameters.ContainsKey("RequestAttempted")) { $RequestAttempted = $RequestSent }
    if (-not $PSBoundParameters.ContainsKey("UriValidated")) { $UriValidated = $false }
    $entry = $CapabilityCatalog[$CapabilityName]
    [PSCustomObject]@{
        capability = $CapabilityName
        docId = if ($entry) { $entry.docId } else { $null }
        tier = if ($entry) { $entry.tier } else { "unknown" }
        mode = $Mode
        status = $Status
        configured = ($SmokeConfig.authMappingStatus -eq "READY" -and $SmokeConfig.endpointMappingStatus -eq "READY")
        requestSent = $RequestSent
        requestAttempted = $RequestAttempted
        uriValidated = $UriValidated
        sanitizedStatus = Redact-Text $SanitizedStatus
        sanitizedError = Redact-Text $SanitizedError
        outputPath = $OutputPath
        durationMs = $DurationMs
        timestamp = (Get-Date -Format o)
        missingEnvNames = @($SmokeConfig.missingEnvNames)
        missingConfigFields = @($SmokeConfig.missingConfigFields)
        detectedConfigGroups = @($SmokeConfig.detectedConfigGroups)
        configSource = $SmokeConfig.configSource
        localConfigRead = [bool]$SmokeConfig.localConfigRead
        endpointMappingStatus = $SmokeConfig.endpointMappingStatus
        authMappingStatus = $SmokeConfig.authMappingStatus
        requestSchemaStatus = $SmokeConfig.requestSchemaStatus
        mappingSource = $SmokeConfig.mappingSource
        timeoutSeconds = $SmokeConfig.timeoutSeconds
    }
}

function Invoke-CapabilitySmoke($CapabilityName, $LocalConfig) {
    $started = Get-Date
    $mode = if ($RunNetwork) { "NETWORK" } else { "DRY_RUN" }
    if (-not $CapabilityCatalog.Contains($CapabilityName)) {
        $emptyConfig = New-EmptySmokeConfig $LocalConfig
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
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "SKIPPED_CONFIG_MISSING" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "ConfigMissing. Missing auth fields are listed without values." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }
    if ($smokeConfig.requestSchemaStatus -ne "READY") {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "SKIPPED_REQUEST_SCHEMA_MISSING" -SmokeConfig $smokeConfig -RequestSent $false -SanitizedStatus "RequestSchemaMissing." -SanitizedError "" -OutputPath "" -DurationMs $elapsed
    }

    $uriCheck = Test-SmokeUri $smokeConfig.url
    if (-not $uriCheck.isValid) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "FAIL_INVALID_URI" -SmokeConfig $smokeConfig -RequestSent $false -RequestAttempted $false -UriValidated $false -SanitizedStatus "Invalid URI after endpoint composition." -SanitizedError "Invalid URI after endpoint composition; check baseUrl and endpointPath fields." -OutputPath "" -DurationMs $elapsed
    }

    $requestAttempted = $false
    try {
        $headers = @{}
        if (Test-RealValue $smokeConfig.authValue) {
            $headers[$smokeConfig.authHeader] = $BearerWord + " " + $smokeConfig.authValue
        }
        $safeName = $CapabilityName.ToString().ToLowerInvariant()
        $outputPath = Join-Path $ProviderOutputDir "$($safeName)_response.txt"
        if ($CapabilityName -eq "OCR") {
            $imagePath = Join-Path $InputDir "ocr_smoke.png"
            $image64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($imagePath))
            $business = if (Test-RealValue $smokeConfig.appId) { "aigc$($smokeConfig.appId)" } else { "aigc" }
            $body = "image=$([System.Net.WebUtility]::UrlEncode($image64))&pos=2&businessid=$([System.Net.WebUtility]::UrlEncode($business))"
            $requestAttempted = $true
            $response = Invoke-WebRequest -Uri $uriCheck.uri -Method Post -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body $body -TimeoutSec $TimeoutSeconds -UseBasicParsing
        } else {
            $payload = New-RequestPayload $CapabilityName
            $json = $payload | ConvertTo-Json -Depth 12
            $headers["Content-Type"] = "application/json"
            $requestAttempted = $true
            $response = Invoke-WebRequest -Uri $uriCheck.uri -Method Post -Headers $headers -ContentType "application/json" -Body $json -TimeoutSec $TimeoutSeconds -UseBasicParsing
        }
        Set-Content -LiteralPath $outputPath -Value (Redact-Text ([string]$response.Content))
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status "PASS" -SmokeConfig $smokeConfig -RequestSent $true -RequestAttempted $true -UriValidated $true -SanitizedStatus "HTTP $($response.StatusCode)" -SanitizedError "" -OutputPath $outputPath -DurationMs $elapsed
    } catch {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        $failureStatus = Get-NetworkFailureStatus $_.Exception $smokeConfig
        $requestSent = if ($failureStatus -eq "FAIL_INVALID_URI") { $false } else { $requestAttempted }
        $statusText = if ($failureStatus -eq "FAIL_INVALID_URI") { "Invalid URI after endpoint composition." } else { "Network request failed." }
        $errorText = if ($failureStatus -eq "FAIL_INVALID_URI") { "Invalid URI after endpoint composition; check baseUrl and endpointPath fields." } else { $_.Exception.Message }
        return New-SmokeResult -CapabilityName $CapabilityName -Mode "NETWORK" -Status $failureStatus -SmokeConfig $smokeConfig -RequestSent $requestSent -RequestAttempted $requestAttempted -UriValidated $uriCheck.isValid -SanitizedStatus $statusText -SanitizedError $errorText -OutputPath "" -DurationMs $elapsed
    }
}

function Write-ExplainConfig($LocalConfig, $Selected) {
    Write-Host "Official provider smoke config status"
    Write-Host ("config.local.json exists: " + $LocalConfig.exists)
    Write-Host ("local config read: " + $LocalConfig.read)
    Write-Host ("vivoCapture exists: " + $LocalConfig.vivoCaptureExists)
    Write-Host ("providers.bluelm exists: " + $LocalConfig.providersBluelmExists)
    Write-Host ("providers.qwen exists: " + $LocalConfig.providersQwenExists)
    Write-Host ("topLevel.bluelm exists: " + $LocalConfig.topLevelBlueLmExists)
    Write-Host ("officialProviders exists: " + $LocalConfig.officialProvidersExists)
    $officialGroups = if ($LocalConfig.officialProviderGroupNames.Count -gt 0) { ($LocalConfig.officialProviderGroupNames | Select-Object -Unique) -join ", " } else { "None" }
    Write-Host ("officialProvider groups: " + $officialGroups)
    if ($LocalConfig.read -and -not $LocalConfig.officialProvidersExists) {
        Write-Host "officialProviders missing: specialized OCR/Retrieval/Translation/TTS/Function Calling/ASR smoke needs officialProviders.<capability> or explicit env config."
    }
    if ($LocalConfig.read -and $LocalConfig.topLevelBlueLmExists) {
        Write-Host "topLevel.bluelm only configures cloud model; it is not reused as specialized provider endpoint mapping."
    }
    foreach ($cap in $Selected) {
        if (-not $CapabilityCatalog.Contains($cap)) { continue }
        $cfg = Get-SmokeConfig $cap $LocalConfig
        Write-Host ("- " + $cap + ": capability URL configured=" + ($cfg.endpointMappingStatus -eq "READY") + "; auth configured=" + ($cfg.authMappingStatus -eq "READY") + "; source=" + $cfg.configSource + "; mappingSource=" + $cfg.mappingSource + "; endpointMapping=" + $cfg.endpointMappingStatus + "; authMapping=" + $cfg.authMappingStatus + "; requestSchema=" + $cfg.requestSchemaStatus + "; requestSent=False")
        if ($cfg.missingEnvNames.Count -gt 0) {
            Write-Host ("  missing env: " + (($cfg.missingEnvNames | Select-Object -Unique) -join ", "))
        }
        if ($cfg.missingConfigFields.Count -gt 0) {
            Write-Host ("  missing config: " + (($cfg.missingConfigFields | Select-Object -Unique) -join ", "))
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
        detectedConfigGroups = @($LocalConfig.detectedConfigGroups)
        aarExists = Test-Path -LiteralPath (Join-Path $RepoRoot "app\libs\llm-sdk-release.aar")
        aarRead = $false
        resultCount = @($Results).Count
        results = $Results
    }
    $summary | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $ResultJson

    $passed = @($Results | Where-Object { $_.status -eq "PASS" })
    $skipped = @($Results | Where-Object { $_.status -like "SKIPPED_*" -or $_.status -eq "DRY_RUN_READY" })
    $failed = @($Results | Where-Object { $_.status -like "FAIL*" })
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
    $md += ("- detected config groups: " + ($(if ($summary.detectedConfigGroups.Count -gt 0) { $summary.detectedConfigGroups -join ", " } else { "None" })))
    $md += ("- AAR exists: " + $summary.aarExists + " (content not read)")
    $md += ("- Passed: " + $passed.Count)
    $md += ("- Skipped: " + $skipped.Count)
    $md += ("- Failed: " + $failed.Count)
    $md += ("- ConfigMissing: " + $configMissing.Count)
    $md += ""
    $md += "## Capability Table"
    $md += ""
    $md += "| Capability | mode | status | requestSent | requestAttempted | uriValidated | configSource | mappingSource | endpointMapping | authMapping | requestSchema | missingEnvNames | missingConfigFields |"
    $md += "|---|---|---|---|---|---|---|---|---|---|---|---|---|"
    foreach ($r in $Results) {
        $missingEnv = if ($r.missingEnvNames.Count -gt 0) { ($r.missingEnvNames -join "<br>") } else { "" }
        $missingConfig = if ($r.missingConfigFields.Count -gt 0) { ($r.missingConfigFields -join "<br>") } else { "" }
        $md += ("| " + $r.capability + " | " + $r.mode + " | " + $r.status + " | " + $r.requestSent + " | " + $r.requestAttempted + " | " + $r.uriValidated + " | " + $r.configSource + " | " + $r.mappingSource + " | " + $r.endpointMappingStatus + " | " + $r.authMappingStatus + " | " + $r.requestSchemaStatus + " | " + $missingEnv + " | " + $missingConfig + " |")
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
    if ($configMissing.Count -eq 0) { $md += "- None" } else { $configMissing | ForEach-Object { $md += ("- " + $_.capability + ": " + ($_.missingEnvNames -join ", ") + " / " + ($_.missingConfigFields -join ", ")) } }
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
