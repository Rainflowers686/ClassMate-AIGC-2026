param(
    [switch]$SkipUnitTest,
    [switch]$SkipAssemble
)

$ErrorActionPreference = "Stop"
$repo = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repo

$script:Failures = New-Object System.Collections.Generic.List[string]

function Step($Name) {
    Write-Host ""
    Write-Host "== $Name ==" -ForegroundColor Cyan
}

function Ok($Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Fail($Message) {
    Write-Host "FAIL: $Message" -ForegroundColor Red
    $script:Failures.Add($Message) | Out-Null
}

function Run($Name, [scriptblock]$Block) {
    Step $Name
    try {
        & $Block
    } catch {
        Fail "$Name threw: $($_.Exception.Message)"
    }
}

function CheckExit($Message) {
    if ($LASTEXITCODE -eq 0) { Ok $Message } else { Fail "$Message exited $LASTEXITCODE" }
}

Write-Host "ClassMate cloud real-device precheck"
Write-Host "Repo: $repo"
Write-Host "This script does not read config.local.json content, does not run provider network smoke, and does not upload or install APKs."

Run "Repository status" {
    $branch = git branch --show-current
    Write-Host "Branch: $branch"
    CheckExit "git branch"

    $status = git status --short
    if ($status) { $status | ForEach-Object { Write-Host $_ } } else { Write-Host "Working tree clean." }
    CheckExit "git status"

    if (Test-Path -LiteralPath "config.local.json" -PathType Leaf) {
        Write-Host "config.local.json exists; content was not read." -ForegroundColor Yellow
    } else {
        Write-Host "config.local.json not present."
    }
}

Run "Whitespace diff check" {
    git diff --check
    CheckExit "git diff --check"
}

Run "Forbidden tracked files" {
    $patterns = @(
        "config.local.json",
        "local.properties",
        "secrets.properties",
        ".env",
        ".env.*",
        "*.jks",
        "*.keystore",
        "*.apk",
        "*.aab",
        "*.aar",
        "app/build",
        "core/build",
        "build",
        ".gradle",
        ".codex_work",
        ".codex_work/*"
    )
    $tracked = @(git ls-files -- $patterns 2>$null)
    if ($tracked.Count -eq 0) {
        Ok "No forbidden private/generated files tracked"
    } else {
        $tracked | ForEach-Object { Write-Host "TRACKED: $_" -ForegroundColor Red }
        Fail "Forbidden tracked files found"
    }
}

Run "Secrets scan" {
    $scan = "scripts\secrets_scan\secrets_scan.ps1"
    if (-not (Test-Path -LiteralPath $scan -PathType Leaf)) {
        Fail "secrets scan script missing"
        return
    }
    powershell -ExecutionPolicy Bypass -File $scan
    CheckExit "secrets scan"
}

if (-not $SkipUnitTest) {
    Run "App unit tests" {
        .\gradlew.bat :app:testDebugUnitTest --no-daemon
        CheckExit ":app:testDebugUnitTest"
    }
} else {
    Write-Host "SKIP: app unit tests" -ForegroundColor Yellow
}

if (-not $SkipAssemble) {
    Run "Debug assemble" {
        .\gradlew.bat :app:assembleDebug --no-daemon
        CheckExit ":app:assembleDebug"
    }
} else {
    Write-Host "SKIP: debug assemble" -ForegroundColor Yellow
}

Run "Debug APK path" {
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path -LiteralPath $apk -PathType Leaf) {
        $item = Get-Item -LiteralPath $apk
        Ok "Debug APK: $($item.FullName)"
        Write-Host "APK size: $($item.Length) bytes"
        Write-Host "APK modified: $($item.LastWriteTime)"
    } else {
        Fail "Debug APK missing: $apk"
    }
}

Write-Host ""
if ($script:Failures.Count -eq 0) {
    Write-Host "CLOUD DEVICE PRECHECK PASS" -ForegroundColor Green
    exit 0
}

Write-Host "CLOUD DEVICE PRECHECK FAIL" -ForegroundColor Red
$script:Failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
exit 1
