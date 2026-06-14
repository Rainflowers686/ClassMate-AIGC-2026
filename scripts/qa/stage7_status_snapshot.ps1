param()

$ErrorActionPreference = "Continue"
$warnings = New-Object System.Collections.Generic.List[string]
$failures = New-Object System.Collections.Generic.List[string]

function Section($name) { Write-Host "`n== $name ==" }
function Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $warnings.Add($msg) | Out-Null }
function Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; $failures.Add($msg) | Out-Null }
function Exists($path) { return Test-Path -LiteralPath $path }

Section "Stage 7 Status Snapshot"
Write-Host "Read-only. Does not run Gradle. Does not read config.local.json."

Section "Git"
$branch = (& git branch --show-current 2>$null)
Write-Host "Branch: $branch"
Write-Host "Recent commits:"
& git log --oneline -15
Write-Host "`nWorking tree:"
& git status --short

Section "APK"
$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (Exists $apk) {
    Get-Item $apk | Select-Object FullName,Length,LastWriteTime | Format-Table -AutoSize
    Pass "Debug APK exists."
} else {
    Warn "Debug APK not found."
}

Section "Directories"
foreach ($dir in @("docs\competition", "docs\testing", "scripts\qa")) {
    if (Exists $dir) { Pass "$dir exists." } else { Fail "$dir missing." }
}

Section "Local config presence"
if (Exists "config.local.json") {
    Warn "config.local.json exists; content was not read."
} else {
    Pass "config.local.json not present."
}

Section "Forbidden tracked files"
$forbidden = & git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
if ($forbidden) {
    Fail "Forbidden tracked files found:"
    $forbidden | ForEach-Object { Write-Host "  $_" }
} else {
    Pass "No forbidden tracked files."
}

Section "qwen guard"
$providerFiles = @(
    "core\src\main\kotlin\com\classmate\core\provider\VendorIo.kt",
    "core\src\main\kotlin\com\classmate\core\provider\BlueLMDiagnostic.kt"
) | Where-Object { Exists $_ }
$guardThinking = $providerFiles | ForEach-Object { Select-String -Path $_ -Pattern "enable_thinking" -SimpleMatch -ErrorAction SilentlyContinue }
$guardModel = $providerFiles | ForEach-Object { Select-String -Path $_ -Pattern "qwen3.5-plus" -SimpleMatch -ErrorAction SilentlyContinue }
if ($guardThinking -and $guardModel) {
    Pass "qwen3.5-plus enable_thinking guard found."
} else {
    Fail "qwen3.5-plus enable_thinking guard missing or incomplete."
}

Section "Manifest permissions"
$manifest = "app\src\main\AndroidManifest.xml"
if (Exists $manifest) {
    $manifestText = Get-Content -LiteralPath $manifest -Raw
    foreach ($perm in @("INTERNET", "RECORD_AUDIO", "WRITE_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE")) {
        if ($manifestText.Contains($perm)) {
            Write-Host "$perm: present"
        } else {
            Write-Host "$perm: absent"
        }
    }
} else {
    Fail "AndroidManifest.xml missing."
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
Write-Host "PASS: no failures or warnings." -ForegroundColor Green

