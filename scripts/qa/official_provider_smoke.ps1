param(
    [switch]$DryRun,
    [switch]$RunNetwork,
    [string[]]$Capability = @(),
    [switch]$AllSafe
)

$ErrorActionPreference = "Continue"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $RepoRoot

$OutDir = Join-Path $RepoRoot ".codex_work\official_provider_smoke"
$ResultJson = Join-Path $OutDir "smoke_result.json"
$ResultMd = Join-Path $OutDir "smoke_result.md"

$Known = @{
    "OCR" = @{ DocId = 1737; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "ASR_LONG" = @{ DocId = 1739; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "TRANSLATION" = @{ DocId = 1733; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "TTS" = @{ DocId = 1735; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "QUERY_REWRITE" = @{ DocId = 2061; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "TEXT_SIMILARITY" = @{ DocId = 2060; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "EMBEDDING" = @{ DocId = 1734; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $false }
    "FUNCTION_CALLING" = @{ DocId = 1805; Tier = "product-facing"; Mode = "dry-run"; AllSafe = $true }
    "IMAGE_GENERATION" = @{ DocId = 1732; Tier = "dev-lab"; Mode = "smoke-only"; AllSafe = $false }
    "VIDEO_GENERATION" = @{ DocId = 2201; Tier = "dev-lab"; Mode = "smoke-only"; AllSafe = $false }
    "SHORT_ASR" = @{ DocId = 1738; Tier = "dev-lab"; Mode = "smoke-only"; AllSafe = $false }
    "LONG_DICTATION" = @{ DocId = 1740; Tier = "dev-lab"; Mode = "smoke-only"; AllSafe = $false }
    "DIALECT_ASR" = @{ DocId = 2065; Tier = "dev-lab"; Mode = "smoke-only"; AllSafe = $false }
    "SIMULTANEOUS_INTERPRETATION" = @{ DocId = 2068; Tier = "dev-lab"; Mode = "smoke-only"; AllSafe = $false }
}

$Excluded = @("VOICE_CLONE", "AUDIO_CLONE", "LBS", "POI", "GEOCODING", "GEO_POI")

function Redact-Text([string]$Text) {
    if ($null -eq $Text) { return "" }
    $t = $Text
    $t = $t -replace (("Auth" + "orization") + "\s*:\s*\S+"), "<redacted-header>"
    $t = $t -replace (("Bear" + "er") + "\s+[A-Za-z0-9._\-]+"), "<redacted-token>"
    $t = $t -replace (("app" + "Key") + "\s*[:=]\s*\S+"), "<redacted-key>"
    $t = $t -replace (("api" + "Key") + "\s*[:=]\s*\S+"), "<redacted-key>"
    return $t
}

function New-Result([string]$Name, [string]$Status, [string]$Message, [int]$DocId = 0, [string]$Tier = "") {
    [pscustomobject]@{
        capability = $Name
        docId = $DocId
        tier = $Tier
        status = $Status
        message = (Redact-Text $Message)
        networkSent = $false
    }
}

$DefaultDryRun = -not $RunNetwork
if (-not $DryRun -and $DefaultDryRun) { $DryRun = $true }

$selected = @()
if ($AllSafe) {
    $selected = $Known.GetEnumerator() | Where-Object { $_.Value.AllSafe } | ForEach-Object { $_.Key }
} elseif ($Capability.Count -gt 0) {
    $selected = $Capability | ForEach-Object { $_.Trim().ToUpperInvariant() } | Where-Object { $_ }
} else {
    $selected = $Known.GetEnumerator() | Where-Object { $_.Value.Tier -eq "product-facing" } | ForEach-Object { $_.Key }
}

$results = New-Object System.Collections.Generic.List[object]

$configExists = Test-Path (Join-Path $RepoRoot "config.local.json")
$aarExists = Test-Path (Join-Path $RepoRoot "app\libs\llm-sdk-release.aar")

foreach ($cap in $selected) {
    if ($Excluded -contains $cap) {
        $results.Add((New-Result $cap "SKIP_EXCLUDED" "Excluded capability. No smoke is allowed." 0 "excluded"))
        continue
    }
    if (-not $Known.ContainsKey($cap)) {
        $results.Add((New-Result $cap "WARN_UNKNOWN" "Unknown capability name. No network request was sent."))
        continue
    }

    $meta = $Known[$cap]
    if ($RunNetwork) {
        $results.Add((New-Result $cap "NETWORK_NOT_WIRED" "Network smoke requires a future app-side runner. This script did not send a request." $meta.DocId $meta.Tier))
    } else {
        $status = if ($configExists) { "DRY_RUN_CONFIG_PRESENT" } else { "DRY_RUN_CONFIG_MISSING" }
        $results.Add((New-Result $cap $status "Dry-run only. Config content was not read; provider should return ConfigMissing when unconfigured." $meta.DocId $meta.Tier))
    }
}

$summary = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("s")
    repo = $RepoRoot
    dryRun = [bool]$DryRun
    runNetwork = [bool]$RunNetwork
    configLocalExists = [bool]$configExists
    aarExists = [bool]$aarExists
    selectedCount = $results.Count
    excludedNames = $Excluded
    results = $results
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$summary | ConvertTo-Json -Depth 8 | Set-Content -Path $ResultJson -Encoding UTF8

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Official Provider Smoke Result")
$md.Add("")
$md.Add("- DryRun: $($summary.dryRun)")
$md.Add("- RunNetwork: $($summary.runNetwork)")
$md.Add("- config.local.json exists: $($summary.configLocalExists) (content not read)")
$md.Add("- AAR exists: $($summary.aarExists) (content not read)")
$md.Add("")
$md.Add("| Capability | docId | tier | status | message |")
$md.Add("|---|---:|---|---|---|")
foreach ($r in $results) {
    $md.Add("| $($r.capability) | $($r.docId) | $($r.tier) | $($r.status) | $($r.message) |")
}
$md | Set-Content -Path $ResultMd -Encoding UTF8

Write-Host "Official provider smoke harness"
Write-Host "Mode: $(if ($RunNetwork) { "Network requested but not wired" } else { "DryRun" })"
Write-Host "Output: $ResultJson"
foreach ($r in $results) {
    Write-Host ("{0}: {1}" -f $r.capability, $r.status)
}

if (($results | Where-Object { $_.status -eq "WARN_UNKNOWN" }).Count -gt 0) {
    exit 1
}
exit 0
