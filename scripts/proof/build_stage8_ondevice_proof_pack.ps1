param(
    [switch]$DryRun,
    [switch]$Build,
    [switch]$Zip,
    [switch]$Open,
    [string]$OutputRoot = "proof_out"
)

$ErrorActionPreference = "Continue"
$warnings = New-Object System.Collections.Generic.List[string]
$failures = New-Object System.Collections.Generic.List[string]

function Section($name) { Write-Host "`n== $name ==" }
function Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $warnings.Add($msg) | Out-Null }
function Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; $failures.Add($msg) | Out-Null }
function Exists($path) { return Test-Path -LiteralPath $path }
function Ensure-Dir($path) { if (!$DryRun -and !(Exists $path)) { New-Item -ItemType Directory -Force -Path $path | Out-Null } }

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$packDir = Join-Path $OutputRoot "stage8a3_ondevice_proof_$stamp"
$dirs = @(
    "00_status",
    "01_screenshots",
    "02_docs",
    "03_security",
    "04_scripts"
)

$copyPlan = @(
    # docs/testing stage8a*
    @{ From = "docs\testing\stage8a2_manual_command_cheatsheet.md"; To = "02_docs" },
    @{ From = "docs\testing\stage8a3_real_device_test_sheet.md"; To = "02_docs" },
    @{ From = "docs\testing\stage8a3_tomorrow_work_order.md"; To = "02_docs" },
    # docs/competition stage8a*
    @{ From = "docs\competition\stage8a2_judge_qna_ondevice.md"; To = "02_docs" },
    @{ From = "docs\competition\stage8a3_ondevice_proof_pack_checklist.md"; To = "02_docs" },
    @{ From = "docs\competition\stage8a3_ondevice_demo_script.md"; To = "02_docs" },
    # docs/product stage8a*
    @{ From = "docs\product\stage8a2_ondevice_user_copy.md"; To = "02_docs" },
    @{ From = "docs\product\stage8a3_ondevice_error_copy.md"; To = "02_docs" },
    # docs/architecture stage8a*
    @{ From = "docs\architecture\stage8a2_multimodal_bridge_design.md"; To = "02_docs" },
    # docs/issues stage8a*
    @{ From = "docs\issues\stage8a3_after_bridge_followups.md"; To = "02_docs" }
)

# ---- Help ----
if (-not ($DryRun -or $Build -or $Zip -or $Open)) {
    Write-Host @"

Stage 8A-3 OnDevice Proof Pack Builder
======================================
Generates proof pack for ondevice BlueLM 3B competition materials.

Usage:
  .\build_stage8_ondevice_proof_pack.ps1 -DryRun
  .\build_stage8_ondevice_proof_pack.ps1 -Build
  .\build_stage8_ondevice_proof_pack.ps1 -Build -Zip
  .\build_stage8_ondevice_proof_pack.ps1 -Build -Zip -Open

Parameters:
  -DryRun        List files that would be copied; create nothing.
  -Build         Create the proof pack directory and copy files.
  -Zip           Compress the pack into a .zip file.
  -Open          Open the pack directory in Explorer.
  -OutputRoot    Root output directory (default: proof_out).

Safety:
  * Does NOT read config.local.json content (only Test-Path).
  * Does NOT copy local.properties, .env, secrets.properties.
  * Does NOT copy APK/AAR files (records path/size/time only).
  * Does NOT run Gradle.
  * Does NOT commit, push, or tag.
  * WARN-only: failures are non-blocking.

"@
    exit 0
}

Section "Stage 8A-3 OnDevice Proof Pack"
Write-Host "Output: $packDir"
Write-Host "DryRun: $DryRun  Build: $Build  Zip: $Zip  Open: $Open"
Write-Host "This script does not run Gradle and does not read config.local.json."

if ($DryRun) {
    Section "Dry run copy plan"
    $copyPlan | ForEach-Object {
        $status = if (Exists $_.From) { "+ " } else { "? " }
        Write-Host "$status$($_.From) -> $($_.To)"
    }
    Write-Host ""
    # List scripts
    if (Exists "scripts\qa") {
        Get-ChildItem "scripts\qa" -Filter "stage8a*.ps1" -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "+ $($_.FullName) -> 04_scripts"
        }
    }
    Write-Host "+ scripts\proof\build_stage8_ondevice_proof_pack.ps1 -> 04_scripts (self-copy)"
    Write-Host ""
    # Preview status files
    Write-Host "Would create: 00_status/git_status.txt"
    Write-Host "Would create: 00_status/local_artifacts.txt"
    Write-Host "Would create: 03_security/security_summary.txt"
    Write-Host "Would create: MANIFEST.txt"
    Write-Host "Would create: README.md"
    Pass "Dry run complete. No files created."
    exit 0
}

# ---- Build ----
Ensure-Dir $packDir
$dirs | ForEach-Object { Ensure-Dir (Join-Path $packDir $_) }

Section "Copy docs"
$copiedCount = 0
$missingCount = 0
foreach ($item in $copyPlan) {
    $from = $item.From
    if (Exists $from) {
        $dest = Join-Path (Join-Path $packDir $item.To) (Split-Path $from -Leaf)
        Copy-Item -LiteralPath $from -Destination $dest -Force
        $copiedCount++
    } else {
        Warn "Missing optional file: $from"
        $missingCount++
    }
}
Pass "Copied $copiedCount doc(s), $missingCount missing (optional)."

Section "Copy QA scripts"
$scriptDir = Join-Path $packDir "04_scripts"
if (Exists "scripts\qa") {
    Get-ChildItem "scripts\qa" -Filter "stage8a*.ps1" -ErrorAction SilentlyContinue | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $scriptDir -Force
        Pass "Copied $($_.Name)"
    }
} else {
    Warn "scripts\qa missing."
}

# Self-copy
$selfPath = $MyInvocation.MyCommand.Path
if ($selfPath) {
    Copy-Item -LiteralPath $selfPath -Destination $scriptDir -Force
    Pass "Copied build_stage8_ondevice_proof_pack.ps1 (self-copy)"
}

# ---- MANIFEST ----
Section "Write MANIFEST"
$manifestLines = New-Object System.Collections.Generic.List[string]
$manifestLines.Add("ClassMate Stage 8A-3 OnDevice Proof Pack Manifest")
$manifestLines.Add("GeneratedAt: $(Get-Date -Format o)")
$manifestLines.Add("GeneratedBy: build_stage8_ondevice_proof_pack.ps1")
$manifestLines.Add("")
$manifestLines.Add("=== Branch ===")
$branchName = try { & git branch --show-current 2>$null } catch { "unknown" }
$manifestLines.Add($branchName)
$manifestLines.Add("")
$manifestLines.Add("=== Recent commits ===")
$recentLog = try { (& git log --oneline -5 2>$null) -join "`n" } catch { "unavailable" }
$manifestLines.Add($recentLog)
$manifestLines.Add("")
$manifestLines.Add("=== Copied files ===")
foreach ($item in $copyPlan) {
    $status = if (Exists $item.From) { "OK" } else { "MISSING" }
    $manifestLines.Add("$status  $($item.From)")
}
$manifestLines.Add("")
$manifestLines.Add("=== AAR record (not copied) ===")
$aarPath = "app\libs\llm-sdk-release.aar"
if (Exists $aarPath) {
    $aar = Get-Item $aarPath
    $manifestLines.Add("AAR path: $($aar.FullName)")
    $manifestLines.Add("AAR size: $($aar.Length) bytes")
    $manifestLines.Add("AAR modified: $($aar.LastWriteTime)")
    $aarIgnored = & git check-ignore -q $aarPath 2>$null
    $manifestLines.Add("AAR git-ignored: $(if ($LASTEXITCODE -eq 0) { 'yes' } else { 'NO — CHECK .gitignore' })")
} else {
    $manifestLines.Add("AAR: NOT FOUND at $aarPath")
}
$manifestLines.Add("")
$manifestLines.Add("=== APK record (not copied) ===")
$apkDebug = "app\build\outputs\apk\debug\app-debug.apk"
if (Exists $apkDebug) {
    $apk = Get-Item $apkDebug
    $manifestLines.Add("APK path: $($apk.FullName)")
    $manifestLines.Add("APK size: $($apk.Length) bytes")
    $manifestLines.Add("APK modified: $($apk.LastWriteTime)")
} else {
    $manifestLines.Add("APK: NOT FOUND at $apkDebug")
}
$manifestLines.Add("")
$manifestLines.Add("=== config.local.json (not read, not copied) ===")
$manifestLines.Add("Exists: $(Exists 'config.local.json')")
$manifestLines.Add("Content was NOT read. This script only uses Test-Path.")
$manifestLines | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $packDir "MANIFEST.txt")
Pass "Wrote MANIFEST.txt"

# ---- Status files ----
Section "Write status files"
$statusDir = Join-Path $packDir "00_status"

$gitStatusPath = Join-Path $statusDir "git_status.txt"
@(
    "ClassMate Stage 8A-3 Git Status",
    "GeneratedAt: $(Get-Date -Format o)",
    "",
    "Branch: $(& git branch --show-current 2>$null)",
    "",
    "Recent commits (10):",
    (& git log --oneline -10 2>$null),
    "",
    "Working tree:",
    (& git status --short 2>$null)
) | Set-Content -Encoding UTF8 -LiteralPath $gitStatusPath
Pass "Wrote 00_status/git_status.txt"

$artifactsPath = Join-Path $statusDir "local_artifacts.txt"
$artifactLines = New-Object System.Collections.Generic.List[string]
$artifactLines.Add("ClassMate Stage 8A-3 Local Artifacts")
$artifactLines.Add("GeneratedAt: $(Get-Date -Format o)")
$artifactLines.Add("")
$artifactLines.Add("APK exists: $(Exists $apkDebug)")
if (Exists $apkDebug) {
    $apk = Get-Item $apkDebug
    $artifactLines.Add("APK path: $($apk.FullName)")
    $artifactLines.Add("APK size: $($apk.Length)")
    $artifactLines.Add("APK LastWriteTime: $($apk.LastWriteTime)")
}
$artifactLines.Add("")
$artifactLines.Add("AAR exists: $(Exists $aarPath)")
if (Exists $aarPath) {
    $aar = Get-Item $aarPath
    $artifactLines.Add("AAR path: $($aar.FullName)")
    $artifactLines.Add("AAR size: $($aar.Length)")
    $artifactLines.Add("AAR LastWriteTime: $($aar.LastWriteTime)")
    $aarIgnored = & git check-ignore -q $aarPath 2>$null
    $artifactLines.Add("AAR git-ignored: $(if ($LASTEXITCODE -eq 0) { 'yes' } else { 'NO' })")
}
$artifactLines.Add("")
foreach ($dir in @("docs\competition", "docs\testing", "docs\product", "docs\architecture", "docs\issues", "scripts\qa")) {
    $artifactLines.Add("$dir exists: $(Exists $dir)")
}
$artifactLines | Set-Content -Encoding UTF8 -LiteralPath $artifactsPath
Pass "Wrote 00_status/local_artifacts.txt"

# ---- Security summary ----
Section "Write security summary"
$securityDir = Join-Path $packDir "03_security"
$securityLines = New-Object System.Collections.Generic.List[string]
$securityLines.Add("ClassMate Stage 8A-3 Security Summary")
$securityLines.Add("GeneratedAt: $(Get-Date -Format o)")
$securityLines.Add("")
$securityLines.Add("=== Sensitive files ===")
$securityLines.Add("config.local.json exists: $(Exists 'config.local.json') (content not read)")
$securityLines.Add("local.properties exists: $(Exists 'local.properties')")
$securityLines.Add("secrets.properties exists: $(Exists 'secrets.properties')")
$securityLines.Add(".env exists: $(Exists '.env')")
$securityLines.Add("")
$securityLines.Add("=== Forbidden tracked files (git ls-files) ===")
$forbidden = & git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle 2>$null
if ($forbidden) {
    $forbidden | ForEach-Object { $securityLines.Add(" - $_") }
} else {
    $securityLines.Add(" none")
}
$securityLines.Add("")
$securityLines.Add("=== Secrets scan ===")
if (Exists "scripts\secrets_scan\secrets_scan.ps1") {
    $scanResult = & powershell -ExecutionPolicy Bypass -File "scripts\secrets_scan\secrets_scan.ps1" 2>&1
    $securityLines.Add(($scanResult -join "`n"))
} else {
    $securityLines.Add(" secrets_scan.ps1 not found — cannot run.")
}
$securityLines.Add("")
$securityLines.Add("=== AAR gitignore verification ===")
if (Exists $aarPath) {
    $aarIgnored = & git check-ignore -v $aarPath 2>$null
    $securityLines.Add("AAR check-ignore: $aarIgnored")
} else {
    $securityLines.Add("AAR check-ignore: AAR not found, skipped.")
}
$securityLines | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $securityDir "security_summary.txt")
Pass "Wrote 03_security/security_summary.txt"

# ---- README ----
Section "Write README"
$readme = @"
# ClassMate Stage 8A-3 OnDevice Proof Pack

This folder collects competition proof materials for the ondevice BlueLM 3B integration.

## How to use

- Read `02_docs` for testing sheets, demo scripts, error copy, and judge Q&A.
- Read `03_security/security_summary.txt` for security audit results.
- Use `04_scripts` for QA helpers and static audit.
- `MANIFEST.txt` records what was copied and artifact metadata.
- `01_screenshots` is empty — copy your screenshots here before submission.

## Safety boundaries

- This pack does NOT copy local credential files (config.local.json, local.properties, .env).
- This pack does NOT copy APK or AAR files; it only records path, size, and timestamp.
- This pack does NOT read config.local.json content; it only records whether it exists.
- Do NOT include screenshots showing real credentials, full auth details, or private student material.

## Quick check before submission

1. Open `03_security/security_summary.txt` — confirm no forbidden tracked files.
2. Confirm `01_screenshots` contains all 17 proof screenshots (see proof pack checklist).
3. Confirm no real API keys appear in any screenshot.
4. Run `scripts\secrets_scan\secrets_scan.ps1` one final time.

## Not a replacement

This pack is for semifinal proof archiving. It does not replace GitHub Actions, local Gradle validation, real-device smoke testing, or manual review of exported reports.
"@
$readme | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $packDir "README.md")
Pass "Wrote README.md"

# ---- Zip ----
if ($Zip) {
    Section "Create zip"
    $zipPath = "$packDir.zip"
    if (Exists $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
    Compress-Archive -Path (Join-Path $packDir "*") -DestinationPath $zipPath -Force
    Pass "Created zip: $zipPath"
}

# ---- Open ----
if ($Open) {
    Invoke-Item $packDir
}

# ---- Summary ----
Section "Summary"
Write-Host "Pack dir: $packDir"
Write-Host "Failures: $($failures.Count), Warnings: $($warnings.Count)"
if ($failures.Count -gt 0) {
    Write-Host "FAIL: $($failures.Count) failure(s). Review output above." -ForegroundColor Red
    exit 1
}
if ($warnings.Count -gt 0) {
    Write-Host "WARN: $($warnings.Count) warning(s). Missing optional files are non-blocking." -ForegroundColor Yellow
    exit 0
}
Write-Host "PASS: proof pack created successfully." -ForegroundColor Green
