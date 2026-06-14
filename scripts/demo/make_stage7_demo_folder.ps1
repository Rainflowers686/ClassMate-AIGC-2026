param(
    [switch]$Open,
    [switch]$DryRun,
    [string]$OutputRoot = "demo_out"
)

$ErrorActionPreference = "Continue"
$warnings = New-Object System.Collections.Generic.List[string]

function Section($name) { Write-Host "`n== $name ==" }
function Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $warnings.Add($msg) | Out-Null }
function Exists($path) { return Test-Path -LiteralPath $path }
function Ensure-Dir($path) { if (!$DryRun -and !(Exists $path)) { New-Item -ItemType Directory -Force -Path $path | Out-Null } }

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$demoDir = Join-Path $OutputRoot "stage7_demo_$stamp"
$dirs = @("01_scripts", "02_slide_outline", "03_test_inputs", "04_screenshot_checklist", "05_do_not_show")

$plan = @(
    @{ From = "docs\demo_assets\stage7_demo_narration_short.md"; To = "01_scripts" },
    @{ From = "docs\demo_assets\stage7_demo_narration_long.md"; To = "01_scripts" },
    @{ From = "docs\demo_assets\stage7_video_recording_script.md"; To = "01_scripts" },
    @{ From = "docs\demo_assets\stage7_slide_outline.md"; To = "02_slide_outline" },
    @{ From = "docs\demo_assets\stage7_slide_image_prompt_pack.md"; To = "02_slide_outline" },
    @{ From = "docs\demo_assets\stage7_test_questions_for_live_demo.md"; To = "03_test_inputs" },
    @{ From = "docs\competition\stage7_proof_screenshot_list.md"; To = "04_screenshot_checklist" }
)

Section "Stage 7 Demo Folder Maker"
Write-Host "Output: $demoDir"
Write-Host "DryRun: $DryRun Open: $Open"
Write-Host "This script does not run Gradle, does not copy APK, and does not read config.local.json."

if ($DryRun) {
    Section "Dry run plan"
    $plan | ForEach-Object { Write-Host "$($_.From) -> $($_.To)" }
    Write-Host "Generate 05_do_not_show\DO_NOT_SHOW.md"
    Pass "Dry run complete. No files created."
    exit 0
}

Ensure-Dir $demoDir
$dirs | ForEach-Object { Ensure-Dir (Join-Path $demoDir $_) }

Section "Copy demo assets"
foreach ($item in $plan) {
    if (Exists $item.From) {
        Copy-Item -LiteralPath $item.From -Destination (Join-Path (Join-Path $demoDir $item.To) (Split-Path $item.From -Leaf)) -Force
        Pass "Copied $($item.From)"
    } else {
        Warn "Missing optional file: $($item.From)"
    }
}

$doNotShow = @"
# Do Not Show During Demo

- Do not record full local application keys.
- Do not record `config.local.json`.
- Do not record complete debug logs.
- Do not record internal model input, message payload, or internal reasoning fields.
- Do not record credential import fields.
- Do not record private file paths, private accounts, or real student data.
- Do not claim real vivo ASR/OCR providers are already integrated.
- Do not claim voiceprint identity recognition, automatic speaker diarization, or self-built denoise is complete.
- Do not show any third-party platform crawling workflow.
"@
$doNotShow | Set-Content -Encoding UTF8 -LiteralPath (Join-Path (Join-Path $demoDir "05_do_not_show") "DO_NOT_SHOW.md")
Pass "Wrote DO_NOT_SHOW.md"

if ($Open) { Invoke-Item $demoDir }

Section "Summary"
if ($warnings.Count -gt 0) {
    Write-Host "WARN: demo folder created with $($warnings.Count) warning(s): $demoDir" -ForegroundColor Yellow
} else {
    Write-Host "PASS: demo folder created at $demoDir" -ForegroundColor Green
}

