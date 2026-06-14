param()

$ErrorActionPreference = "Continue"
$failures = New-Object System.Collections.Generic.List[string]

function Step($name) {
    Write-Host "`n== $name ==" -ForegroundColor Cyan
}

function Mark-Fail($message) {
    $script:failures.Add($message) | Out-Null
    Write-Host "FAIL: $message" -ForegroundColor Red
}

function Mark-Pass($message) {
    Write-Host "PASS: $message" -ForegroundColor Green
}

Step "Branch"
git branch --show-current

Step "Recent commits"
git log --oneline -10

Step "Working tree"
git status --short

Step "Local config presence only"
if (Test-Path "config.local.json") {
    Write-Host "config.local.json exists; content was not read."
} else {
    Write-Host "config.local.json not present."
}

Step "Diff whitespace check"
git diff --check
if ($LASTEXITCODE -eq 0) { Mark-Pass "git diff --check" } else { Mark-Fail "git diff --check failed" }

Step "Forbidden tracked files"
$tracked = git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
if ($tracked) {
    $tracked
    Mark-Fail "Forbidden generated or private files are tracked."
} else {
    Mark-Pass "No forbidden private/generated files tracked."
}

Step "Secrets scan"
$scan = "scripts\secrets_scan\secrets_scan.ps1"
if (Test-Path $scan) {
    & $scan
    if ($LASTEXITCODE -eq 0) { Mark-Pass "secrets scan" } else { Mark-Fail "secrets scan failed" }
} else {
    Mark-Fail "secrets scan script missing"
}

Step "qwen thinking guard"
$providerFiles = @(
    "core\src\main\kotlin\com\classmate\core\provider\VendorIo.kt",
    "core\src\main\kotlin\com\classmate\core\provider\BlueLMDiagnostic.kt"
)
$guardHits = Select-String -Path $providerFiles -Pattern "enable_thinking|qwen3.5-plus" -ErrorAction SilentlyContinue
$guardHits | ForEach-Object { $_.Line.Trim() }
if ($guardHits.Count -ge 3) { Mark-Pass "qwen guard present" } else { Mark-Fail "qwen guard missing or incomplete" }

Step "Android permissions"
$manifest = "app\src\main\AndroidManifest.xml"
if (Test-Path $manifest) {
    $danger = Select-String -Path $manifest -Pattern "RECORD_AUDIO|MANAGE_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE" -ErrorAction SilentlyContinue
    if ($danger) {
        $danger | ForEach-Object { $_.Line.Trim() }
        Mark-Fail "Dangerous permission found"
    } else {
        Mark-Pass "No RECORD_AUDIO / storage-management write permission"
    }
} else {
    Mark-Fail "AndroidManifest missing"
}

Step "Docs index"
if (Test-Path "docs\INDEX.md") { Mark-Pass "docs/INDEX.md exists" } else { Mark-Fail "docs/INDEX.md missing" }

Step "Summary"
if ($failures.Count -eq 0) {
    Write-Host "STAGE5 PREFLIGHT PASS" -ForegroundColor Green
    exit 0
}

Write-Host "STAGE5 PREFLIGHT FAIL" -ForegroundColor Red
$failures | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
exit 1

