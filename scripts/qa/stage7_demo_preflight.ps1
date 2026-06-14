param(
    [string]$Branch = "feature/product-review-compatible"
)

$ErrorActionPreference = "Continue"
$warnings = New-Object System.Collections.Generic.List[string]
$failures = New-Object System.Collections.Generic.List[string]

function Section($name) { Write-Host "`n== $name ==" }
function Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $warnings.Add($msg) | Out-Null }
function Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; $failures.Add($msg) | Out-Null }
function Exists($path) { return Test-Path -LiteralPath $path }

Section "Stage 7 Demo Preflight"
Write-Host "Read-only. Does not run Gradle. Does not read config.local.json."

Section "Working tree"
$status = & git status --short
if ($status) {
    Warn "Working tree is not clean. Review changes before demo."
    $status | ForEach-Object { Write-Host $_ }
} else {
    Pass "Working tree clean."
}

Section "GitHub Actions"
$gh = Get-Command gh -ErrorAction SilentlyContinue
if ($gh) {
    & gh run list --branch $Branch --limit 8
} else {
    Warn "gh CLI not found. Check GitHub Actions manually."
}

Section "APK"
$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (Exists $apk) {
    $apkInfo = Get-Item $apk
    $apkInfo | Select-Object FullName,Length,LastWriteTime | Format-Table -AutoSize
    $sourceDirs = @("app\src", "core\src") | Where-Object { Exists $_ }
    $latestSource = $null
    if ($sourceDirs.Count -gt 0) {
        $latestSource = Get-ChildItem $sourceDirs -Recurse -File -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    }
    if ($latestSource -and $apkInfo.LastWriteTime -lt $latestSource.LastWriteTime) {
        Warn "APK is older than latest source file. Build may be stale."
        Write-Host "Latest source: $($latestSource.FullName) $($latestSource.LastWriteTime)"
    } else {
        Pass "APK exists and does not look older than latest source metadata."
    }
} else {
    Fail "Debug APK not found."
}

Section "Secrets scan"
if (Exists "scripts\secrets_scan\secrets_scan.ps1") {
    & scripts\secrets_scan\secrets_scan.ps1
    if ($LASTEXITCODE -eq 0) { Pass "secrets scan passed." } else { Fail "secrets scan failed." }
} else {
    Fail "secrets scan script missing."
}

Section "Required demo docs"
foreach ($doc in @(
    "docs\testing\stage7_asr_smoke.md",
    "docs\competition\stage7_final_demo_script.md",
    "docs\competition\stage7_proof_screenshot_list.md"
)) {
    if (Exists $doc) { Pass "$doc exists." } else { Fail "$doc missing." }
}

Section "Recommendation"
if ($failures.Count -gt 0) {
    Write-Host "Not recommended for demo: $($failures.Count) failure(s), $($warnings.Count) warning(s)." -ForegroundColor Red
    exit 1
}
if ($warnings.Count -gt 0) {
    Write-Host "Demo possible with caution: $($warnings.Count) warning(s)." -ForegroundColor Yellow
    exit 0
}
Write-Host "Ready for demo preflight. Still run Gradle/CI separately before final submission." -ForegroundColor Green

