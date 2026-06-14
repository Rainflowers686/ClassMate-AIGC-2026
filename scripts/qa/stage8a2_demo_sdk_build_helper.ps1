param(
    [switch]$Check,
    [switch]$WriteLocalProperties,
    [switch]$BuildRelease,
    [switch]$FindAar,
    [switch]$CopyToClassMate,
    [string]$SourcePath,
    [string]$EnglishPath = "D:\AIGC_SDK\ondevice_llm_demo_src",
    [string]$ClassMatePath = "D:\Edge Download\AIGC\ClassMate",
    [string]$SdkDir = "D:\AAAbiancheng\Android\SDK"
)

$ErrorActionPreference = "Continue"

function Ok($Message) { Write-Host "[OK] $Message" -ForegroundColor Green }
function Warn($Message) { Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Fail($Message) { Write-Host "[FAIL] $Message" -ForegroundColor Red }

function Default-SourcePath {
    $segment = "demo" + [char]0x548C + [char]0x7AEF + [char]0x4FA7 + "llm" + [char]0x6E90 + [char]0x7801
    return (Join-Path "D:\Edge Download\AIGC\official_sdk" $segment)
}

function Show-Help {
    Write-Host "Stage 8A-2 demo SDK build helper"
    Write-Host "Default is help-only. No build, copy, or file write happens without explicit switches."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\qa\stage8a2_demo_sdk_build_helper.ps1 -Check"
    Write-Host "  .\scripts\qa\stage8a2_demo_sdk_build_helper.ps1 -WriteLocalProperties"
    Write-Host "  .\scripts\qa\stage8a2_demo_sdk_build_helper.ps1 -BuildRelease"
    Write-Host "  .\scripts\qa\stage8a2_demo_sdk_build_helper.ps1 -FindAar"
    Write-Host "  .\scripts\qa\stage8a2_demo_sdk_build_helper.ps1 -CopyToClassMate"
    Write-Host ""
    Write-Host "This helper does not read config.local.json and does not commit/push/tag."
}

if (-not $SourcePath) { $SourcePath = Default-SourcePath }
$aarOut = Join-Path $EnglishPath "llm-sdk\build\outputs\aar\llm-sdk-release.aar"
$classMateAar = Join-Path $ClassMatePath "app\libs\llm-sdk-release.aar"

if (-not ($Check -or $WriteLocalProperties -or $BuildRelease -or $FindAar -or $CopyToClassMate)) {
    Show-Help
    exit 0
}

Write-Host "== Stage 8A-2 demo SDK build helper =="
Write-Host "Source path: $SourcePath"
Write-Host "English path: $EnglishPath"
Write-Host "ClassMate path: $ClassMatePath"
Write-Host "SDK dir: $SdkDir"

if (Test-Path -LiteralPath (Join-Path $ClassMatePath "config.local.json")) {
    Warn "ClassMate config.local.json exists. Content was not read."
}

if ($Check) {
    Write-Host "`n-- Check --"
    if (Test-Path -LiteralPath $SourcePath) { Ok "Source path exists." } else { Warn "Source path missing." }
    if (Test-Path -LiteralPath $EnglishPath) { Ok "English path exists." } else { Warn "English path missing." }
    if (Test-Path -LiteralPath $ClassMatePath) { Ok "ClassMate path exists." } else { Fail "ClassMate path missing." }
    if (Test-Path -LiteralPath $SdkDir) { Ok "Android SDK dir exists." } else { Warn "Android SDK dir missing." }
}

if ($WriteLocalProperties) {
    Write-Host "`n-- Write local.properties --"
    if (-not (Test-Path -LiteralPath $EnglishPath)) {
        Fail "English path missing. Cannot write local.properties."
    } else {
        $content = @(
            "android.overridePathCheck=true",
            ("sdk.dir=" + ($SdkDir -replace "\\", "/"))
        )
        $target = Join-Path $EnglishPath "local.properties"
        Set-Content -LiteralPath $target -Value $content -Encoding UTF8
        Ok "Wrote $target"
    }
}

if ($BuildRelease) {
    Write-Host "`n-- Build release AAR --"
    $gradlew = Join-Path $EnglishPath "gradlew.bat"
    if (-not (Test-Path -LiteralPath $gradlew)) {
        Fail "gradlew.bat missing: $gradlew"
    } else {
        Push-Location $EnglishPath
        try {
            & $gradlew ":llm-sdk:assembleRelease"
            if ($LASTEXITCODE -eq 0) { Ok "Build command completed." } else { Fail "Build command exit code: $LASTEXITCODE" }
        } finally {
            Pop-Location
        }
    }
}

if ($FindAar) {
    Write-Host "`n-- Find AAR --"
    if (Test-Path -LiteralPath $aarOut) {
        $item = Get-Item -LiteralPath $aarOut
        Ok "AAR: $($item.FullName)"
        Ok "Size: $($item.Length) bytes"
        Ok "LastWriteTime: $($item.LastWriteTime)"
    } else {
        Warn "AAR not found: $aarOut"
    }
}

if ($CopyToClassMate) {
    Write-Host "`n-- Copy to ClassMate --"
    if (-not (Test-Path -LiteralPath $aarOut)) {
        Fail "Source AAR missing: $aarOut"
    } elseif (-not (Test-Path -LiteralPath $ClassMatePath)) {
        Fail "ClassMate path missing: $ClassMatePath"
    } else {
        $libs = Join-Path $ClassMatePath "app\libs"
        New-Item -ItemType Directory -Path $libs -Force | Out-Null
        Copy-Item -LiteralPath $aarOut -Destination $classMateAar -Force
        Ok "Copied to $classMateAar"
        Push-Location $ClassMatePath
        try {
            $ignore = git check-ignore -v "app\libs\llm-sdk-release.aar" 2>$null
            if ($LASTEXITCODE -eq 0 -and $ignore) { Ok "AAR is ignored: $ignore" } else { Warn "AAR ignore check failed." }
        } finally {
            Pop-Location
        }
    }
}
