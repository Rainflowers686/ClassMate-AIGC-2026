param(
    [switch]$DryRun,
    [switch]$RunNetwork,
    [string[]]$Capability,
    [switch]$AllSafe,
    [string]$OutputDir,
    [switch]$NoOpen,
    [switch]$VerboseLog
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
    "OCR" = @{ docId = 1737; tier = "product-facing"; input = "ocr_smoke.png" }
    "QUERY_REWRITE" = @{ docId = 2061; tier = "product-facing"; input = "query_rewrite.txt" }
    "TEXT_SIMILARITY" = @{ docId = 2060; tier = "product-facing"; input = "similarity.json" }
    "TRANSLATION" = @{ docId = 1733; tier = "product-facing"; input = "translation_en.txt" }
    "TTS" = @{ docId = 1735; tier = "product-facing"; input = "tts_zh.txt" }
    "FUNCTION_CALLING" = @{ docId = 1805; tier = "product-facing"; input = "function_calling.json" }
    "ASR_LONG" = @{ docId = 1739; tier = "product-facing-secondary"; input = "asr_long.wav" }
    "EMBEDDING" = @{ docId = 1734; tier = "product-facing"; input = "embedding.json" }
    "IMAGE_GEN" = @{ docId = 1732; tier = "dev-lab"; input = "image_gen.txt" }
    "VIDEO_GEN" = @{ docId = 2201; tier = "dev-lab"; input = "video_gen.txt" }
    "SHORT_ASR" = @{ docId = 1738; tier = "dev-lab"; input = "short_asr.wav" }
    "DIALECT_ASR" = @{ docId = 2065; tier = "dev-lab"; input = "dialect_asr.wav" }
    "SIMULTANEOUS_INTERPRETATION" = @{ docId = 2068; tier = "dev-lab"; input = "simultaneous_interpretation.wav" }
}

if (-not $RunNetwork) {
    $DryRun = $true
}

function Write-SmokeLog($Message) {
    $line = "$(Get-Date -Format o) $Message"
    Add-Content -LiteralPath $LogPath -Value (Redact-Text $line)
    if ($VerboseLog) {
        Write-Host (Redact-Text $Message)
    }
}

function Redact-Text($Text) {
    if ($null -eq $Text) { return "" }
    $redacted = [string]$Text
    $sensitiveNames = @(
        ("Auth" + "orization"),
        ("Bear" + "er"),
        ("app" + "Key"),
        ("api" + "Key"),
        ("app" + "Id"),
        "token",
        "cookie"
    )
    foreach ($name in $sensitiveNames) {
        $redacted = $redacted -replace "(?i)$([regex]::Escape($name))\s*[:=]\s*[^,\s;]+", "$name=<redacted>"
    }
    $authValue = [Environment]::GetEnvironmentVariable("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE")
    if ($authValue -and $authValue.Length -gt 0) {
        $redacted = $redacted.Replace($authValue, "<redacted>")
    }
    return $redacted
}

function Ensure-Directories {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
    New-Item -ItemType Directory -Force -Path $ProviderOutputDir | Out-Null
    Set-Content -LiteralPath $LogPath -Value "Official provider smoke log $(Get-Date -Format o)"
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
        $brush = [System.Drawing.Brushes]::Black
        $graphics.DrawString("ClassMate OCR smoke 2026", $font, $brush, 30, 90)
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        $graphics.Dispose()
        $bitmap.Dispose()
    } catch {
        $fallbackPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        [IO.File]::WriteAllBytes($path, [Convert]::FromBase64String($fallbackPng))
        Write-SmokeLog "WARN OCR image generation fallback used: $($_.Exception.Message)"
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

function Get-SelectedCapabilities {
    if ($Capability -and $Capability.Count -gt 0) {
        return @($Capability | ForEach-Object { $_.Trim().ToUpperInvariant() } | Where-Object { $_.Length -gt 0 })
    }
    if ($AllSafe -or $DryRun) {
        return $SafeCapabilities
    }
    return $SafeCapabilities
}

function Get-EnvOrEmpty($Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ($null -eq $value) { return "" }
    return $value
}

function Get-NetworkConfig($CapabilityName) {
    $cap = $CapabilityName.ToUpperInvariant()
    $url = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_${cap}_URL"
    $authValue = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_${cap}_AUTH_VALUE"
    if (-not $authValue) {
        $authValue = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE"
    }
    $authHeader = Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_AUTH_HEADER"
    if (-not $authHeader) {
        $authHeader = ("Auth" + "orization")
    }
    [PSCustomObject]@{
        Url = $url
        AuthHeader = $authHeader
        AuthValue = $authValue
        AllowNoAuth = ((Get-EnvOrEmpty "CLASSMATE_PROVIDER_SMOKE_ALLOW_NO_AUTH") -eq "1")
    }
}

function New-RequestPayload($CapabilityName) {
    switch ($CapabilityName) {
        "OCR" {
            $imagePath = Join-Path $InputDir "ocr_smoke.png"
            return @{
                imageBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($imagePath))
                note = "ClassMate OCR smoke test image"
            }
        }
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
            $inputName = $CapabilityCatalog[$CapabilityName].input
            $inputPath = Join-Path $InputDir $inputName
            if (Test-Path -LiteralPath $inputPath) {
                return @{ input = Get-Content -LiteralPath $inputPath -Raw }
            }
            return @{ input = "ClassMate provider smoke input" }
        }
    }
}

function New-SmokeResult($CapabilityName, $Mode, $Status, $Configured, $RequestSent, $SanitizedStatus, $SanitizedError, $OutputPath, $DurationMs) {
    $catalog = $CapabilityCatalog[$CapabilityName]
    [PSCustomObject]@{
        capability = $CapabilityName
        docId = if ($catalog) { $catalog.docId } else { $null }
        tier = if ($catalog) { $catalog.tier } else { "unknown" }
        mode = $Mode
        status = $Status
        configured = $Configured
        requestSent = $RequestSent
        sanitizedStatus = Redact-Text $SanitizedStatus
        sanitizedError = Redact-Text $SanitizedError
        outputPath = $OutputPath
        durationMs = $DurationMs
        timestamp = (Get-Date -Format o)
    }
}

function Invoke-CapabilitySmoke($CapabilityName) {
    $started = Get-Date
    if (-not $CapabilityCatalog.ContainsKey($CapabilityName)) {
        return New-SmokeResult $CapabilityName "DRY_RUN" "SKIPPED_NOT_ALLOWED" "unknown" $false "Capability is not in the allowed smoke catalog." "" "" 0
    }

    $catalog = $CapabilityCatalog[$CapabilityName]
    $inputPath = Join-Path $InputDir $catalog.input
    $audioCaps = @("ASR_LONG", "SHORT_ASR", "DIALECT_ASR", "SIMULTANEOUS_INTERPRETATION")
    if (($audioCaps -contains $CapabilityName) -and -not (Test-Path -LiteralPath $inputPath)) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        $mode = if ($RunNetwork) { "NETWORK" } else { "DRY_RUN" }
        return New-SmokeResult $CapabilityName $mode "SKIPPED_NO_INPUT" "unknown" $false "No non-sensitive test audio was provided." "" "" $elapsed
    }

    if (-not $RunNetwork) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult $CapabilityName "DRY_RUN" "DRY_RUN_READY" "unknown" $false "No network request was sent." "" "" $elapsed
    }

    $network = Get-NetworkConfig $CapabilityName
    if (-not $network.Url -or ((-not $network.AuthValue) -and (-not $network.AllowNoAuth))) {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult $CapabilityName "NETWORK" "SKIPPED_CONFIG_MISSING" $false $false "Endpoint or auth environment variables are missing." "" "" $elapsed
    }

    try {
        $headers = @{}
        if ($network.AuthValue) {
            $headers[$network.AuthHeader] = $network.AuthValue
        }
        $payload = New-RequestPayload $CapabilityName
        $json = $payload | ConvertTo-Json -Depth 12
        $safeName = $CapabilityName.ToString().ToLowerInvariant()
        $outputPath = Join-Path $ProviderOutputDir "$($safeName)_response.txt"
        Write-SmokeLog "$CapabilityName sending sanitized network request."
        $response = Invoke-WebRequest -Uri $network.Url -Method Post -Headers $headers -ContentType "application/json" -Body $json -TimeoutSec 45 -UseBasicParsing
        $statusLine = "HTTP $($response.StatusCode)"
        $content = [string]$response.Content
        if ($CapabilityName -eq "TTS") {
            $outputPath = Join-Path $ProviderOutputDir "tts_response.bin"
            [IO.File]::WriteAllBytes($outputPath, [Text.Encoding]::UTF8.GetBytes($content))
        } else {
            Set-Content -LiteralPath $outputPath -Value (Redact-Text $content)
        }
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult $CapabilityName "NETWORK" "PASS" $true $true $statusLine "" $outputPath $elapsed
    } catch {
        $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
        return New-SmokeResult $CapabilityName "NETWORK" "FAIL" $true $true "Network request failed." $_.Exception.Message "" $elapsed
    }
}

function Write-Results {
    param([object[]]$Results)

    $summary = [PSCustomObject]@{
        generatedAt = (Get-Date -Format o)
        dryRun = [bool]$DryRun
        runNetwork = [bool]$RunNetwork
        configLocalExists = Test-Path -LiteralPath (Join-Path $RepoRoot "config.local.json")
        configLocalRead = $false
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
    $md += ("- config.local.json exists: " + $summary.configLocalExists + " (content not read)")
    $md += ("- AAR exists: " + $summary.aarExists + " (content not read)")
    $md += ("- Passed: " + $passed.Count)
    $md += ("- Skipped: " + $skipped.Count)
    $md += ("- Failed: " + $failed.Count)
    $md += ("- ConfigMissing: " + $configMissing.Count)
    $md += ""
    $md += "## Capability Table"
    $md += ""
    $md += "| Capability | docId | mode | tier | status | requestSent | sanitizedStatus | outputPath | durationMs |"
    $md += "|---|---:|---|---|---|---|---|---|---:|"
    foreach ($r in $Results) {
        $md += ("| " + $r.capability + " | " + $r.docId + " | " + $r.mode + " | " + $r.tier + " | " + $r.status + " | " + $r.requestSent + " | " + $r.sanitizedStatus + " | " + $r.outputPath + " | " + $r.durationMs + " |")
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
    if ($configMissing.Count -eq 0) { $md += "- None" } else { $configMissing | ForEach-Object { $md += ("- " + $_.capability) } }
    $md += ""
    $md += "## Next Action"
    if ($RunNetwork) {
        $md += "- Review PASS / FAIL entries. Re-run one capability at a time with sanitized test inputs."
    } else {
        $md += "- Network smoke was not executed. Use -RunNetwork with one -Capability only after explicit authorization and environment configuration."
    }
    $md | Set-Content -LiteralPath $ResultMd
}

Set-Location $RepoRoot
Ensure-Directories
Ensure-TestInputs

$configPath = Join-Path $RepoRoot "config.local.json"
$configExists = Test-Path -LiteralPath $configPath
Write-SmokeLog "config.local.json exists: $configExists; content not read."
Write-SmokeLog "AAR exists: $(Test-Path -LiteralPath (Join-Path $RepoRoot 'app\libs\llm-sdk-release.aar')); content not read."

if ($Capability -and $Capability.Count -gt 0) {
    $selected = @($Capability | ForEach-Object { $_.Trim().ToUpperInvariant() } | Where-Object { $_.Length -gt 0 })
} else {
    $selected = @($SafeCapabilities)
}
$results = New-Object System.Collections.Generic.List[object]
foreach ($cap in $selected) {
    if ($null -eq $cap -or $cap.ToString().Trim().Length -eq 0) { continue }
    $capName = $cap.ToUpperInvariant()
    $result = Invoke-CapabilitySmoke $capName
    [void]$results.Add($result)
}

Write-Results -Results $results.ToArray()

Write-Host "Official provider smoke harness"
Write-Host "Mode: $(if ($RunNetwork) { 'NETWORK' } else { 'DRY_RUN' })"
Write-Host "Output: $ResultJson"
foreach ($result in $results) {
    Write-Host "$($result.capability): $($result.status)"
}

if (-not $NoOpen -and $RunNetwork -and $VerboseLog) {
    Write-Host "Results written to $OutputDir"
}
