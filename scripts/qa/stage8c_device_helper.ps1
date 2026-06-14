param(
    [switch]$Info,
    [switch]$Preflight,
    [switch]$AdbDevices,
    [switch]$Install,
    [switch]$Launch,
    [switch]$LogcatStage8C,
    [switch]$AllLight
)

$ErrorActionPreference = "Continue"
$ExpectedRoot = "D:\Edge Download\AIGC\ClassMate"
$PackageName = "com.classmate.app"
$ApkPath = Join-Path $ExpectedRoot "app\build\outputs\apk\debug\app-debug.apk"
$AarPath = Join-Path $ExpectedRoot "app\libs\llm-sdk-release.aar"
$ProofAssets = "D:\Edge Download\AIGC\ClassMate_proof_assets\stage8a2_ondevice_20260606"

function Ok($Message) { Write-Host "[OK] $Message" -ForegroundColor Green }
function Warn($Message) { Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Fail($Message) { Write-Host "[FAIL] $Message" -ForegroundColor Red }

function Show-Help {
    Write-Host "Stage 8C device helper"
    Write-Host "Default is help-only. No Gradle. No device deletion. No app data clearing."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -Info"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -Preflight"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -AdbDevices"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -Install"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -Launch"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -LogcatStage8C"
    Write-Host "  .\scripts\qa\stage8c_device_helper.ps1 -AllLight"
}

function Assert-Root {
    $current = (Get-Location).Path
    if ($current -ne $ExpectedRoot) {
        Fail "Wrong working directory: $current"
        Fail "Expected: $ExpectedRoot"
        exit 2
    }
}

function Find-Adb {
    $cmd = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
        "D:\AAAbiancheng\Android\SDK\platform-tools\adb.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return $null
}

function Get-Files($Roots) {
    $files = New-Object System.Collections.Generic.List[string]
    foreach ($root in $Roots) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue |
                ForEach-Object { [void]$files.Add($_.FullName) }
        }
    }
    return $files
}

function Search-Simple($Title, $Terms, $Roots) {
    Write-Host "`n-- $Title --"
    $files = Get-Files $Roots
    $hits = 0
    foreach ($term in $Terms) {
        foreach ($file in $files) {
            $matches = Select-String -LiteralPath $file -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
            foreach ($m in $matches) {
                $hits++
                if ($hits -le 40) {
                    $line = $m.Line.Trim()
                    if ($line.Length -gt 160) { $line = $line.Substring(0, 160) + "..." }
                    Warn "$($m.Path):$($m.LineNumber): $line"
                }
            }
        }
    }
    if ($hits -eq 0) { Ok "No hits." } else { Warn "$hits hit(s), review manually." }
}

function Invoke-Info {
    Write-Host "== Stage 8C Info =="
    Write-Host "Project: $ExpectedRoot"
    Write-Host "PWD: $((Get-Location).Path)"
    Write-Host "Branch:"
    git branch --show-current
    Write-Host "Git status:"
    git status --short
    if (Test-Path -LiteralPath $ApkPath) {
        $apk = Get-Item -LiteralPath $ApkPath
        Ok "APK: $($apk.FullName)"
        Ok "APK size: $($apk.Length) bytes"
        Ok "APK LastWriteTime: $($apk.LastWriteTime)"
    } else {
        Warn "APK missing: $ApkPath"
    }
    if (Test-Path -LiteralPath $AarPath) {
        $aar = Get-Item -LiteralPath $AarPath
        Ok "AAR: $($aar.FullName)"
        Ok "AAR size: $($aar.Length) bytes"
        Ok "AAR LastWriteTime: $($aar.LastWriteTime)"
    } else {
        Warn "AAR missing: $AarPath"
    }
    if (Test-Path -LiteralPath $ProofAssets) { Ok "External proof assets path exists: $ProofAssets" } else { Warn "External proof assets path missing: $ProofAssets" }
}

function Invoke-Preflight {
    Write-Host "== Stage 8C Preflight =="
    Write-Host "config.local.json presence only:"
    if (Test-Path -LiteralPath "config.local.json") { Warn "config.local.json exists. Content was not read." } else { Ok "config.local.json not present." }

    Write-Host "`n-- git status --"
    git status --short

    Write-Host "`n-- AAR --"
    if (Test-Path -LiteralPath $AarPath) { Ok "AAR exists." } else { Warn "AAR missing." }
    $ignore = git check-ignore -v "app\libs\llm-sdk-release.aar" 2>$null
    if ($LASTEXITCODE -eq 0 -and $ignore) { Ok "AAR is gitignored: $ignore" } else { Warn "AAR gitignore check failed." }

    Write-Host "`n-- APK --"
    if (Test-Path -LiteralPath $ApkPath) { Ok "APK exists." } else { Warn "APK missing." }

    Write-Host "`n-- forbidden tracked files --"
    $forbidden = @("config.local.json","local.properties","secrets.properties",".env",".env.*","*.jks","*.keystore","*.apk","*.aab","app/build","core/build","build",".gradle")
    $tracked = @(git ls-files $forbidden 2>$null)
    if ($tracked.Count -eq 0) { Ok "No forbidden tracked files." } else { foreach ($t in $tracked) { Warn "Tracked forbidden path: $t" } }

    Search-Simple "direct SDK import" @("import com.vivo.llmsdk") @("app\src", "core\src")

    $localRuleTerms = @(
        "LocalRule available",
        "LocalRule success",
        "local rule analysis",
        "rule intelligence",
        "本地规则兜底",
        "规则智能",
        "本地规则分析"
    )
    Search-Simple "LocalRule disabled wording" $localRuleTerms @("app\src", "core\src", "docs")

    Search-Simple "qwen guard" @("qwen3.5-plus", "enable_thinking") @("app\src", "core\src")

    Write-Host "`n-- Manifest permissions --"
    $manifest = "app\src\main\AndroidManifest.xml"
    if (Test-Path -LiteralPath $manifest) {
        foreach ($perm in @(
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.BLUETOOTH",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )) {
            $hit = Select-String -LiteralPath $manifest -Pattern $perm -SimpleMatch -ErrorAction SilentlyContinue
            if ($hit) { Warn "Manifest contains $perm" } else { Ok "Manifest does not contain $perm" }
        }
        foreach ($blocked in @("CONTACTS", "LOCATION", "SMS", "PHONE", "CALL_LOG", "BODY_SENSORS")) {
            $hit = Select-String -LiteralPath $manifest -Pattern $blocked -SimpleMatch -ErrorAction SilentlyContinue
            if ($hit) { Warn "Manifest contains sensitive permission family: $blocked" } else { Ok "No $blocked permission family." }
        }
    } else {
        Warn "Manifest not found."
    }
}

function Invoke-AdbDevices {
    $adb = Find-Adb
    if (-not $adb) { Warn "adb.exe not found. Install Android SDK platform-tools or add adb to PATH."; return }
    & $adb devices
}

function Invoke-Install {
    $adb = Find-Adb
    if (-not $adb) { Warn "adb.exe not found."; return }
    if (-not (Test-Path -LiteralPath $ApkPath)) { Warn "APK missing: $ApkPath"; return }
    & $adb install -r $ApkPath
}

function Invoke-Launch {
    $adb = Find-Adb
    if (-not $adb) { Warn "adb.exe not found."; return }
    & $adb shell monkey -p $PackageName 1
}

function Invoke-LogcatStage8C {
    $adb = Find-Adb
    if (-not $adb) { Warn "adb.exe not found."; return }
    $outDir = Join-Path $ExpectedRoot "qa_out\stage8c_logs"
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $outFile = Join-Path $outDir "stage8c_logcat_$stamp.txt"
    $patterns = "classmate|ondevice|image|camera|course analysis|courseanalysis|validator|llm|callvit"
    Warn "Capturing filtered logcat snapshot. Review before sharing; do not publish sensitive logs."
    & $adb logcat -d |
        Select-String -Pattern $patterns -CaseSensitive:$false |
        ForEach-Object { $_.Line } |
        Set-Content -LiteralPath $outFile -Encoding UTF8
    Ok "Logcat snapshot written: $outFile"
}

Assert-Root

if (-not ($Info -or $Preflight -or $AdbDevices -or $Install -or $Launch -or $LogcatStage8C -or $AllLight)) {
    Show-Help
    exit 0
}

if ($AllLight) {
    Invoke-Info
    Invoke-Preflight
    Invoke-AdbDevices
    exit 0
}

if ($Info) { Invoke-Info }
if ($Preflight) { Invoke-Preflight }
if ($AdbDevices) { Invoke-AdbDevices }
if ($Install) { Invoke-Install }
if ($Launch) { Invoke-Launch }
if ($LogcatStage8C) { Invoke-LogcatStage8C }
