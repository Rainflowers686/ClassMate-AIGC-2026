param(
    [switch]$DryRun,
    [switch]$Zip,
    [switch]$Open
)

$ErrorActionPreference = "Continue"
$Repo = "D:\Edge Download\AIGC\ClassMate"
$ExternalProofAssets = "D:\Edge Download\AIGC\ClassMate_proof_assets\stage8a2_ondevice_20260606"

function Ok($Message) { Write-Host "[OK] $Message" -ForegroundColor Green }
function Warn($Message) { Write-Host "[WARN] $Message" -ForegroundColor Yellow }

function Copy-IfExists($Source, $DestDir) {
    $full = Join-Path $Repo $Source
    if (Test-Path -LiteralPath $full) {
        $dest = Join-Path $DestDir (Split-Path $Source -Leaf)
        if ($DryRun) {
            Ok "Would copy $Source -> $dest"
        } else {
            Copy-Item -LiteralPath $full -Destination $dest -Force
            Ok "Copied $Source"
        }
    } else {
        Warn "Missing optional file: $Source"
    }
}

if ((Get-Location).Path -ne $Repo) {
    Warn "Current directory is $((Get-Location).Path); switching to $Repo for read-only packaging."
    Set-Location $Repo
}

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outRoot = Join-Path $Repo "proof_out\stage8c_proof_$stamp"
$dirs = @(
    "00_status",
    "01_testing",
    "02_competition",
    "03_security",
    "04_scripts",
    "05_external_assets_record"
)

Write-Host "== Stage 8C proof pack builder =="
Write-Host "Output: $outRoot"
if ($DryRun) { Warn "DryRun: no files or directories will be created." }

if (-not $DryRun) {
    foreach ($dir in $dirs) {
        New-Item -ItemType Directory -Path (Join-Path $outRoot $dir) -Force | Out-Null
    }
}

$testingDest = Join-Path $outRoot "01_testing"
$competitionDest = Join-Path $outRoot "02_competition"
$scriptDest = Join-Path $outRoot "04_scripts"

foreach ($file in @(
    "docs\testing\stage8c_device_smoke.md",
    "docs\testing\stage8c_test_inputs.md",
    "docs\testing\stage8_ondevice_smoke_checklist.md",
    "docs\testing\stage8a2_real_sdk_smoke_plan.md",
    "docs\testing\stage8a2_ondevice_sdk_build_record.md"
)) {
    Copy-IfExists $file $testingDest
}

foreach ($file in @(
    "docs\competition\stage8c_proof_screenshot_list.md",
    "docs\competition\stage8_ondevice_talking_points.md",
    "docs\competition\stage7_submission_package_checklist.md"
)) {
    Copy-IfExists $file $competitionDest
}

foreach ($file in @(
    "scripts\qa\stage8c_device_helper.ps1",
    "scripts\qa\stage8a2_sdk_preflight.ps1",
    "scripts\proof\build_stage8c_proof_pack.ps1"
)) {
    Copy-IfExists $file $scriptDest
}

if (-not $DryRun) {
    $status = @()
    $status += "Stage 8C proof pack status"
    $status += "Generated: $(Get-Date)"
    $status += "Branch:"
    $status += (git branch --show-current)
    $status += ""
    $status += "Recent commits:"
    $status += (git log --oneline -10)
    $status += ""
    $status += "Git status:"
    $status += (git status --short)
    Set-Content -LiteralPath (Join-Path $outRoot "00_status\git_status.txt") -Value $status -Encoding UTF8

    $artifact = @()
    $artifact += "Local artifact summary"
    $apk = Join-Path $Repo "app\build\outputs\apk\debug\app-debug.apk"
    $aar = Join-Path $Repo "app\libs\llm-sdk-release.aar"
    if (Test-Path -LiteralPath $apk) {
        $i = Get-Item -LiteralPath $apk
        $artifact += "APK exists: yes"
        $artifact += "APK path: $($i.FullName)"
        $artifact += "APK size: $($i.Length)"
        $artifact += "APK LastWriteTime: $($i.LastWriteTime)"
    } else {
        $artifact += "APK exists: no"
    }
    if (Test-Path -LiteralPath $aar) {
        $i = Get-Item -LiteralPath $aar
        $artifact += "AAR exists: yes"
        $artifact += "AAR path recorded only, not copied: $($i.FullName)"
        $artifact += "AAR size: $($i.Length)"
        $artifact += "AAR LastWriteTime: $($i.LastWriteTime)"
    } else {
        $artifact += "AAR exists: no"
    }
    $artifact += "External proof assets path recorded only: $ExternalProofAssets"
    $artifact += "External proof assets exists: $(Test-Path -LiteralPath $ExternalProofAssets)"
    Set-Content -LiteralPath (Join-Path $outRoot "00_status\local_artifacts.txt") -Value $artifact -Encoding UTF8

    $security = @()
    $security += "Security summary"
    $security += "config.local.json exists: $(Test-Path -LiteralPath (Join-Path $Repo 'config.local.json'))"
    $security += "config.local.json content was not read."
    $security += "Forbidden tracked files:"
    $tracked = @(git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle 2>$null)
    if ($tracked.Count -eq 0) { $security += "none" } else { $security += $tracked }
    Set-Content -LiteralPath (Join-Path $outRoot "03_security\security_summary.txt") -Value $security -Encoding UTF8

    $external = @(
        "External proof assets record",
        "Path: $ExternalProofAssets",
        "Exists: $(Test-Path -LiteralPath $ExternalProofAssets)",
        "Screenshots are not copied by this script unless a future user explicitly requests it."
    )
    Set-Content -LiteralPath (Join-Path $outRoot "05_external_assets_record\external_assets.txt") -Value $external -Encoding UTF8

    $readme = @(
        "# Stage 8C Proof Pack",
        "",
        "This pack contains documentation, status summaries, and helper scripts for Stage 8C device testing.",
        "",
        "It does not copy APK files, AAR files, local config files, screenshots, or private proof assets.",
        "",
        "Do not screenshot or publish model keys, local private paths, full logs, or complete model interactions.",
        "",
        "Use docs/testing/stage8c_device_smoke.md and docs/competition/stage8c_proof_screenshot_list.md for device proof collection."
    )
    Set-Content -LiteralPath (Join-Path $outRoot "README.md") -Value $readme -Encoding UTF8
}

if ($Zip -and -not $DryRun) {
    $zipPath = "$outRoot.zip"
    Compress-Archive -Path $outRoot -DestinationPath $zipPath -Force
    Ok "Zip created: $zipPath"
}

if ($Open -and -not $DryRun) {
    Invoke-Item $outRoot
}

Write-Host "`n== Summary =="
if ($DryRun) {
    Ok "Dry-run complete. No proof pack created."
} else {
    Ok "Proof pack created: $outRoot"
}
Warn "APK and AAR are recorded only and not copied."
