param(
    [switch]$UseLocalConfig,
    [switch]$RunNetwork,
    [string]$AudioPath,
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $OutputDir -or $OutputDir.Trim().Length -eq 0) {
    $OutputDir = Join-Path $RepoRoot ".codex_work\provider_live_smoke"
}

function Write-Result($Name, $Status, $Detail) {
    Write-Host ("{0}: {1} {2}" -f $Name, $Status, $Detail)
}

Write-Host "ClassMate official provider live smoke (safe, redacted)"
Write-Host "Default mode does not inspect local credential files and does not run network requests."

Write-Result "BlueLM" "SKIP" "local credentials missing or not inspected; run app Developer Settings -> official service dry-run for the minimal live prompt"

if (-not $UseLocalConfig) {
    Write-Result "Official realtime ASR" "SKIP" "local credentials missing or not inspected"
    Write-Result "Official long ASR" "SKIP" "local credentials missing or not inspected; no audio submitted"
    Write-Result "Official TTS" "SKIP" "local credentials missing or not inspected"
    Write-Result "OCR" "READY" "manual fallback available; official config not inspected"
    exit 0
}

$SmokeScript = Join-Path $PSScriptRoot "official_provider_smoke.ps1"
if (-not (Test-Path -LiteralPath $SmokeScript)) {
    Write-Result "Official provider smoke" "FAIL" "official_provider_smoke.ps1 not found"
    exit 1
}

$capabilities = @("OCR", "ASR_LONG", "TTS")
$smokeArgs = @(
    "-DryRun",
    "-UseLocalConfig",
    "-NoOpen",
    "-Capability"
) + $capabilities + @(
    "-OutputDir",
    $OutputDir
)

if ($RunNetwork) {
    $smokeArgs = @("-RunNetwork", "-UseLocalConfig", "-NoOpen", "-Capability") + $capabilities + @("-OutputDir", $OutputDir)
}

if ($AudioPath -and (Test-Path -LiteralPath $AudioPath)) {
    Write-Result "Official long ASR" "READY" "audio path supplied; delegated smoke keeps logs redacted"
} else {
    Write-Result "Official long ASR" "SKIP" "no audio path supplied; readiness only"
}

& powershell -ExecutionPolicy Bypass -File $SmokeScript @smokeArgs
if ($LASTEXITCODE -ne 0) {
    Write-Result "Official provider smoke" "FAIL" "delegated smoke failed; inspect redacted output under $OutputDir"
    exit $LASTEXITCODE
}

Write-Result "Official provider smoke" "DONE" "see redacted artifacts under $OutputDir"
