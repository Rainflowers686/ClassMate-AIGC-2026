param(
    [switch]$Zip,
    [switch]$Open,
    [switch]$DryRun,
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
$packDir = Join-Path $OutputRoot "stage7_proof_$stamp"
$dirs = @(
    "00_status",
    "01_demo",
    "02_testing",
    "03_security",
    "04_competition",
    "05_scripts"
)

$copyPlan = @(
    @{ From = "docs\competition\stage7_final_demo_script.md"; To = "01_demo" },
    @{ From = "docs\competition\stage7_judge_qna_50.md"; To = "04_competition" },
    @{ From = "docs\competition\stage7_competitor_positioning.md"; To = "04_competition" },
    @{ From = "docs\competition\stage7_proof_screenshot_list.md"; To = "04_competition" },
    @{ From = "docs\competition\stage7_feature_matrix_for_reviewers.md"; To = "04_competition" },
    @{ From = "docs\testing\stage7_full_regression_plan.md"; To = "02_testing" },
    @{ From = "docs\testing\stage7_scripts_usage.md"; To = "02_testing" },
    @{ From = "docs\testing\stage7_asr_smoke.md"; To = "02_testing" },
    @{ From = "docs\testing\stage6_report_acceptance.md"; To = "02_testing" },
    @{ From = "docs\testing\stage6_markdown_import_samples.md"; To = "02_testing" },
    @{ From = "docs\product\practice_search_keyword_bank.md"; To = "02_testing" },
    @{ From = "docs\product\course_library_ia_notes.md"; To = "02_testing" }
)

Section "Stage 7 Proof Pack"
Write-Host "Output: $packDir"
Write-Host "DryRun: $DryRun Zip: $Zip Open: $Open"
Write-Host "This script does not run Gradle and does not read config.local.json."

if ($DryRun) {
    Section "Dry run copy plan"
    $copyPlan | ForEach-Object { Write-Host "$($_.From) -> $($_.To)" }
    if (Exists "scripts\qa") {
        Get-ChildItem "scripts\qa" -Filter "*.ps1" | ForEach-Object { Write-Host "$($_.FullName) -> 05_scripts" }
    }
    Pass "Dry run complete. No files created."
    exit 0
}

Ensure-Dir $packDir
$dirs | ForEach-Object { Ensure-Dir (Join-Path $packDir $_) }

Section "Copy docs"
foreach ($item in $copyPlan) {
    $from = $item.From
    if (Exists $from) {
        $dest = Join-Path (Join-Path $packDir $item.To) (Split-Path $from -Leaf)
        Copy-Item -LiteralPath $from -Destination $dest -Force
        Pass "Copied $from"
    } else {
        Warn "Missing optional file: $from"
    }
}

Section "Copy QA scripts"
if (Exists "scripts\qa") {
    Get-ChildItem "scripts\qa" -Filter "*.ps1" | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $packDir "05_scripts") -Force
        Pass "Copied $($_.Name)"
    }
} else {
    Warn "scripts\qa missing."
}

Section "Write status files"
$statusDir = Join-Path $packDir "00_status"
$securityDir = Join-Path $packDir "03_security"

$gitStatusPath = Join-Path $statusDir "git_status.txt"
@(
    "ClassMate Stage 7 Git Status",
    "GeneratedAt: $(Get-Date -Format o)",
    "",
    "Branch:",
    (& git branch --show-current 2>$null),
    "",
    "Recent commits:",
    (& git log --oneline -20 2>$null),
    "",
    "Working tree:",
    (& git status --short 2>$null)
) | Set-Content -Encoding UTF8 -LiteralPath $gitStatusPath
Pass "Wrote 00_status/git_status.txt"

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
$artifactLines = New-Object System.Collections.Generic.List[string]
$artifactLines.Add("ClassMate Stage 7 Local Artifacts")
$artifactLines.Add("GeneratedAt: $(Get-Date -Format o)")
$artifactLines.Add("")
if (Exists $apkPath) {
    $apk = Get-Item $apkPath
    $artifactLines.Add("APK exists: yes")
    $artifactLines.Add("APK path: $($apk.FullName)")
    $artifactLines.Add("APK size: $($apk.Length)")
    $artifactLines.Add("APK LastWriteTime: $($apk.LastWriteTime)")
} else {
    $artifactLines.Add("APK exists: no")
}
foreach ($dir in @("docs\competition", "docs\testing", "scripts\qa")) {
    $artifactLines.Add("$dir exists: $(Exists $dir)")
}
$artifactLines | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $statusDir "local_artifacts.txt")
Pass "Wrote 00_status/local_artifacts.txt"

$securityLines = New-Object System.Collections.Generic.List[string]
$securityLines.Add("ClassMate Stage 7 Security Summary")
$securityLines.Add("GeneratedAt: $(Get-Date -Format o)")
$securityLines.Add("")
$securityLines.Add("config.local.json exists: $(Exists 'config.local.json') (content not read)")
$securityLines.Add("")
$securityLines.Add("Forbidden tracked files:")
$forbidden = & git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle 2>$null
if ($forbidden) { $forbidden | ForEach-Object { $securityLines.Add(" - $_") } } else { $securityLines.Add(" none") }
$securityLines.Add("")
$securityLines.Add("qwen guard:")
$providerFiles = @(
    "core\src\main\kotlin\com\classmate\core\provider\VendorIo.kt",
    "core\src\main\kotlin\com\classmate\core\provider\BlueLMDiagnostic.kt"
) | Where-Object { Exists $_ }
$guardHits = foreach ($file in $providerFiles) {
    Select-String -Path $file -Pattern "enable_thinking|qwen3.5-plus" -ErrorAction SilentlyContinue |
        ForEach-Object { "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
}
if ($guardHits) { $guardHits | ForEach-Object { $securityLines.Add(" - $_") } } else { $securityLines.Add(" none") }
$securityLines.Add("")
$securityLines.Add("Manifest permission summary:")
$manifest = "app\src\main\AndroidManifest.xml"
if (Exists $manifest) {
    $manifestText = Get-Content -LiteralPath $manifest -Raw
    foreach ($perm in @("INTERNET", "RECORD_AUDIO", "WRITE_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE")) {
        $securityLines.Add(" - ${perm}: $($manifestText.Contains($perm))")
    }
} else {
    $securityLines.Add(" manifest missing")
}
$securityLines | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $securityDir "security_summary.txt")
Pass "Wrote 03_security/security_summary.txt"

$readme = @"
# ClassMate Stage 7 Proof Pack

This folder collects competition proof materials for review, rehearsal, and final packaging.

## How to use

- Read `01_demo` for narration and demo flow.
- Read `02_testing` for device regression and smoke plans.
- Read `03_security/security_summary.txt` before screenshot capture.
- Read `04_competition` for judge Q&A, feature matrix, and competitor positioning.
- Use `05_scripts` for lightweight QA helpers.

## Safety boundaries

- This pack does not copy local credential files.
- This pack does not copy APK files; it only records APK path, size, and timestamp.
- This pack does not read `config.local.json`; it only records whether it exists.
- Do not screenshot credential input, local config files, full auth details, internal model input/output, private account pages, or private student material.

## Not a replacement

This pack is useful for semifinal proof archiving. It does not replace GitHub Actions, local Gradle validation, real-device smoke testing, or manual review of exported reports.
"@
$readme | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $packDir "README.md")
Pass "Wrote README.md"

if ($Zip) {
    $zipPath = "$packDir.zip"
    if (Exists $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
    Compress-Archive -Path (Join-Path $packDir "*") -DestinationPath $zipPath -Force
    Pass "Created zip: $zipPath"
}

if ($Open) {
    Invoke-Item $packDir
}

Section "Summary"
if ($failures.Count -gt 0) {
    Write-Host "FAIL: $($failures.Count) failure(s), $($warnings.Count) warning(s)." -ForegroundColor Red
    exit 1
}
if ($warnings.Count -gt 0) {
    Write-Host "WARN: 0 failure(s), $($warnings.Count) warning(s)." -ForegroundColor Yellow
    exit 0
}
Write-Host "PASS: proof pack created at $packDir" -ForegroundColor Green
