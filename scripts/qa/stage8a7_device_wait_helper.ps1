<#
.SYNOPSIS
  Stage 8A-7 Cloud Device Wait Helper — read-only / assistive QA script for
  cloud-device queue waiting and quick on-device checks.

.DESCRIPTION
  Does NOT run Gradle, does NOT read config.local.json content, does NOT copy
  APK/AAR into repo, does NOT delete device files, does NOT clear user data.
  All output files (logs, adb output, screenshots) are written under
  qa_out/stage8a7_logs/.

.PARAMETER Info
  Display project path, branch, git status, APK path/size/time, AAR path/size/time.

.PARAMETER Preflight
  Run lightweight static checks (no Gradle). See Preflight check list in the output.

.PARAMETER AdbDevices
  Run `adb devices` and report connected devices.

.PARAMETER Install
  Install current app-debug.apk to the connected device via `adb install`.

.PARAMETER Launch
  Launch ClassMate main Activity on the connected device.

.PARAMETER LogcatOnDevice
  Capture logcat lines matching ondevice / llm / vivo / classmate into
  qa_out/stage8a7_logs/.

.PARAMETER PullScreenshots
  Attempt to pull screenshots from /sdcard/Pictures/Screenshots/. Best-effort;
  failure is not fatal.

.PARAMETER AllLight
  Shorthand for -Info -Preflight -AdbDevices.

.PARAMETER Help
  Show this help message.

.EXAMPLE
  .\stage8a7_device_wait_helper.ps1 -Info
  .\stage8a7_device_wait_helper.ps1 -AllLight
  .\stage8a7_device_wait_helper.ps1 -Install -Launch -LogcatOnDevice
#>

param(
    [switch]$Info,
    [switch]$Preflight,
    [switch]$AdbDevices,
    [switch]$Install,
    [switch]$Launch,
    [switch]$LogcatOnDevice,
    [switch]$PullScreenshots,
    [switch]$AllLight,
    [switch]$Help
)

$ErrorActionPreference = "Continue"

# ---- Colour helpers ----
function Ok($Message)    { Write-Host "[OK] $Message"    -ForegroundColor Green }
function Warn($Message)  { Write-Host "[WARN] $Message"  -ForegroundColor Yellow }
function Fail($Message)  { Write-Host "[FAIL] $Message"  -ForegroundColor Red }
function Info($Message)  { Write-Host "[INFO] $Message"  -ForegroundColor Cyan }
function Header($Message){ Write-Host "`n-- $Message --" -ForegroundColor Magenta }

# ---- Help ----
function Show-Help {
    Write-Host @"
Stage 8A-7 Device Wait Helper
=============================
Read-only / assistive QA script for cloud-device queue waiting and on-device checks.

USAGE:
  .\stage8a7_device_wait_helper.ps1 [parameters]

PARAMETERS:
  -Info              Show project path, branch, git status, APK/AAR details.
  -Preflight         Run lightweight static checks (no Gradle).
  -AdbDevices        Run 'adb devices'.
  -Install           Install app-debug.apk to connected device.
  -Launch            Launch ClassMate main Activity.
  -LogcatOnDevice    Capture ondevice/llm/vivo/classmate logcat to qa_out/stage8a7_logs/.
  -PullScreenshots   Attempt to pull /sdcard/Pictures/Screenshots/ (best-effort).
  -AllLight          Shorthand for -Info -Preflight -AdbDevices.
  -Help              Show this help.

No parameters defaults to showing this help. No Gradle is ever run.
Does NOT read config.local.json content. Does NOT copy APK/AAR into repo.
Does NOT delete device files or clear user data.
"@
}

# ---- Ensure output directory ----
function Ensure-OutputDir {
    $script:outDir = Join-Path (Get-Location).Path "qa_out\stage8a7_logs"
    if (-not (Test-Path -LiteralPath $outDir)) {
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
        Info "Created output directory: $outDir"
    }
}

# ---- Validate paths that must exist ----
function Assert-Path($Path, $Label) {
    if (-not (Test-Path -LiteralPath $Path)) {
        Fail "$Label not found: $Path"
        return $false
    }
    return $true
}

# ---- Find adb ----
function Find-Adb {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) {
        Info "adb found: $($adb.Source)"
        return $adb.Source
    }
    # Try Android SDK default locations
    $candidates = @(
        [System.IO.Path]::Combine($env:LOCALAPPDATA, "Android\Sdk\platform-tools\adb.exe"),
        [System.IO.Path]::Combine($env:ANDROID_HOME, "platform-tools\adb.exe"),
        [System.IO.Path]::Combine($env:ANDROID_SDK_ROOT, "platform-tools\adb.exe"),
        "C:\Android\Sdk\platform-tools\adb.exe"
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path -LiteralPath $c)) {
            Info "adb found at: $c"
            return $c
        }
    }
    return $null
}

# ---- Utility: format file size ----
function Format-Size($Bytes) {
    if ($Bytes -gt 1MB) { return "$([math]::Round($Bytes / 1MB, 2)) MB" }
    if ($Bytes -gt 1KB) { return "$([math]::Round($Bytes / 1KB, 2)) KB" }
    return "$Bytes bytes"
}

# ========================================================================
# INFO
# ========================================================================
function Do-Info {
    Header "Project Info"
    $repo = (Get-Location).Path
    Info "Project path: $repo"

    $branch = git branch --show-current 2>$null
    if ($branch) { Ok "Branch: $branch" } else { Warn "Could not determine git branch." }

    Info "git status --short:"
    $status = git status --short 2>$null
    if ($status) { Write-Host $status } else { Write-Host "  (clean)" }

    # APK
    Header "APK"
    $apk = Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path -LiteralPath $apk) {
        $apkItem = Get-Item -LiteralPath $apk
        Ok "APK path:  $apk"
        Ok "APK size:  $(Format-Size $apkItem.Length)"
        Ok "APK time:  $($apkItem.LastWriteTime)"
    } else {
        Warn "APK not found: $apk"
    }

    # AAR
    Header "AAR"
    $aar = Join-Path $repo "app\libs\llm-sdk-release.aar"
    if (Test-Path -LiteralPath $aar) {
        $aarItem = Get-Item -LiteralPath $aar
        Ok "AAR path:  $aar"
        Ok "AAR size:  $(Format-Size $aarItem.Length)"
        Ok "AAR time:  $($aarItem.LastWriteTime)"
    } else {
        Warn "AAR not found: $aar"
    }
}

# ========================================================================
# PREFLIGHT
# ========================================================================
function Do-Preflight {
    $warns = 0
    $fails = 0

    Header "Preflight — Lightweight Static Checks"

    $repo = (Get-Location).Path

    # 1. git status --short
    Info "1. git status --short"
    $status = git status --short 2>$null
    if ($status) {
        Warn "Working tree not clean:"
        Write-Host $status
        $warns++
    } else {
        Ok "Working tree clean."
    }

    # 2. AAR exists
    Info "2. AAR presence"
    $aar = Join-Path $repo "app\libs\llm-sdk-release.aar"
    if (Test-Path -LiteralPath $aar) {
        $aarItem = Get-Item -LiteralPath $aar
        Ok "AAR exists: $($aarItem.FullName)"
        Ok "  Size: $(Format-Size $aarItem.Length), Time: $($aarItem.LastWriteTime)"
    } else {
        Fail "AAR missing at: $aar"
        $fails++
    }

    # 3. AAR gitignored
    Info "3. AAR gitignored"
    $ignoreCheck = git check-ignore -q "app\libs\llm-sdk-release.aar" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Ok "AAR is git-ignored (safe)."
    } else {
        Warn "AAR is NOT git-ignored or check-ignore failed."
        $warns++
    }

    # 4. APK exists
    Info "4. APK presence"
    $apk = Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path -LiteralPath $apk) {
        $apkItem = Get-Item -LiteralPath $apk
        Ok "APK exists: $($apkItem.FullName)"
        Ok "  Size: $(Format-Size $apkItem.Length), Time: $($apkItem.LastWriteTime)"
    } else {
        Warn "APK not found at: $apk"
        $warns++
    }

    # 5. Forbidden tracked files
    Info "5. Forbidden tracked files"
    $forbidden = @(
        "config.local.json", "local.properties", "secrets.properties",
        ".env", ".env.*", "*.jks", "*.keystore", "*.apk", "*.aab",
        "app/build", "core/build", "build", ".gradle"
    )
    $tracked = @(git ls-files $forbidden 2>$null)
    if ($tracked.Count -eq 0) {
        Ok "No forbidden tracked files."
    } else {
        foreach ($p in $tracked) { Fail "Tracked forbidden path: $p"; $fails++ }
    }

    # 6. qwen enable_thinking guard
    Info "6. qwen enable_thinking guard"
    $srcRoots = @("app/src", "core/src")
    $qwenHits = @()
    foreach ($root in $srcRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $qwenHits += Select-String -LiteralPath $file.FullName -Pattern "qwen3.5-plus" -SimpleMatch -ErrorAction SilentlyContinue
            $qwenHits += Select-String -LiteralPath $file.FullName -Pattern "enable_thinking" -SimpleMatch -ErrorAction SilentlyContinue
        }
    }
    if ($qwenHits.Count -gt 0) {
        Ok "qwen3.5-plus / enable_thinking references: $($qwenHits.Count)"
        foreach ($h in $qwenHits[0..[Math]::Min(9, $qwenHits.Count-1)]) {
            $line = $h.Line.Trim(); if ($line.Length -gt 150) { $line = $line.Substring(0, 150) + "..." }
            Write-Host "  $($h.Path):$($h.LineNumber): $line" -ForegroundColor Gray
        }
        if ($qwenHits.Count -gt 10) { Warn "... and $($qwenHits.Count - 10) more hits." }
    } else {
        Warn "No qwen3.5-plus / enable_thinking references found."
        $warns++
    }

    # 7. direct import com.vivo.llmsdk
    Info "7. Direct import com.vivo.llmsdk"
    $directHitsProd = @()
    $directHitsTest = @()
    foreach ($root in $srcRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $hits = Select-String -LiteralPath $file.FullName -Pattern "import com.vivo.llmsdk" -SimpleMatch -ErrorAction SilentlyContinue
            if ($file.FullName -match "[\\/]test[\\/]") {
                $directHitsTest += $hits
            } else {
                $directHitsProd += $hits
            }
        }
    }
    if ($directHitsProd.Count -eq 0) {
        Ok "No direct import com.vivo.llmsdk in production code (reflection bridge is correct)."
    } else {
        foreach ($h in $directHitsProd) { Fail "$($h.Path):$($h.LineNumber): $($h.Line.Trim())"; $fails++ }
    }
    if ($directHitsTest.Count -gt 0) {
        Warn "$($directHitsTest.Count) direct import(s) in test code (may be fake SDK / educational):"
        foreach ($h in $directHitsTest) { Write-Host "  [WARN] $($h.Path):$($h.LineNumber): $($h.Line.Trim())" -ForegroundColor Yellow }
    }

    # 8. onComplete(LlmStats) old signature
    Info "8. onComplete(LlmStats) old signature"
    $oldSigHitsProd = @()
    $oldSigHitsTest = @()
    foreach ($root in $srcRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $hits = Select-String -LiteralPath $file.FullName -Pattern "onComplete\s*\(\s*LlmStats" -ErrorAction SilentlyContinue
            if ($file.FullName -match "[\\/]test[\\/]") {
                $oldSigHitsTest += $hits
            } else {
                $oldSigHitsProd += $hits
            }
        }
    }
    if ($oldSigHitsProd.Count -eq 0) {
        Ok "No onComplete(LlmStats) old signature in production code."
    } else {
        foreach ($h in $oldSigHitsProd) { Fail "$($h.Path):$($h.LineNumber): $($h.Line.Trim())"; $fails++ }
    }
    if ($oldSigHitsTest.Count -gt 0) {
        Warn "$($oldSigHitsTest.Count) onComplete(LlmStats) reference(s) in test code (likely educational / counter-example / fake SDK comment — not a blocker):"
        foreach ($h in $oldSigHitsTest) { Write-Host "  [WARN] $($h.Path):$($h.LineNumber): $($h.Line.Trim().Substring(0, [Math]::Min(150, $h.Line.Trim().Length)))" -ForegroundColor Yellow }
    }

    # 9. void callVit wrong signature
    Info "9. void callVit wrong signature"
    $wrongVitHitsProd = @()
    $wrongVitHitsTest = @()
    foreach ($root in $srcRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $hits = Select-String -LiteralPath $file.FullName -Pattern "void\s+callVit" -ErrorAction SilentlyContinue
            if ($file.FullName -match "[\\/]test[\\/]") {
                $wrongVitHitsTest += $hits
            } else {
                $wrongVitHitsProd += $hits
            }
        }
    }
    if ($wrongVitHitsProd.Count -eq 0) {
        Ok "No 'void callVit' wrong signature in production code."
    } else {
        foreach ($h in $wrongVitHitsProd) { Fail "$($h.Path):$($h.LineNumber): $($h.Line.Trim())"; $fails++ }
    }
    if ($wrongVitHitsTest.Count -gt 0) {
        Warn "$($wrongVitHitsTest.Count) 'void callVit' reference(s) in test code (likely educational / counter-example — not a blocker):"
        foreach ($h in $wrongVitHitsTest) { Write-Host "  [WARN] $($h.Path):$($h.LineNumber): $($h.Line.Trim().Substring(0, [Math]::Min(150, $h.Line.Trim().Length)))" -ForegroundColor Yellow }
    }

    # 10. Manifest dangerous storage permission
    Info "10. Manifest dangerous storage permissions"
    $manifest = Join-Path $repo "app\src\main\AndroidManifest.xml"
    if (Test-Path -LiteralPath $manifest) {
        $dangerPerms = @("MANAGE_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE")
        $foundDanger = $false
        foreach ($perm in $dangerPerms) {
            $hit = Select-String -LiteralPath $manifest -Pattern $perm -SimpleMatch -ErrorAction SilentlyContinue
            if ($hit) {
                Warn "Manifest contains: $perm at $($hit.Path):$($hit.LineNumber)"
                $foundDanger = $true
                $warns++
            }
        }
        if (-not $foundDanger) {
            Ok "Manifest does NOT contain dangerous storage permissions."
        }
    } else {
        Warn "AndroidManifest.xml not found at $manifest"
        $warns++
    }

    # 11. config.local.json test-path only
    Info "11. config.local.json"
    if (Test-Path -LiteralPath "config.local.json") {
        Warn "config.local.json exists locally. Content was NOT read (Test-Path only)."
    } else {
        Ok "config.local.json not present."
    }

    # 12. Wrong package name com.blue.lm.sdk in source
    Info "12. Wrong package name com.blue.lm.sdk"
    $wrongPkgHitsProd = @()
    $wrongPkgHitsTest = @()
    foreach ($root in $srcRoots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $hits = Select-String -LiteralPath $file.FullName -Pattern "com\.blue\.lm\.sdk" -ErrorAction SilentlyContinue
            if ($file.FullName -match "[\\/]test[\\/]") {
                $wrongPkgHitsTest += $hits
            } else {
                $wrongPkgHitsProd += $hits
            }
        }
    }
    if ($wrongPkgHitsProd.Count -eq 0) {
        Ok "No wrong package name 'com.blue.lm.sdk' in production code."
    } else {
        foreach ($h in $wrongPkgHitsProd) { Fail "$($h.Path):$($h.LineNumber): $($h.Line.Trim())"; $fails++ }
    }
    if ($wrongPkgHitsTest.Count -gt 0) {
        Warn "$($wrongPkgHitsTest.Count) 'com.blue.lm.sdk' reference(s) in test code (likely educational / counter-example — not a blocker):"
        foreach ($h in $wrongPkgHitsTest) { Write-Host "  [WARN] $($h.Path):$($h.LineNumber): $($h.Line.Trim().Substring(0, [Math]::Min(150, $h.Line.Trim().Length)))" -ForegroundColor Yellow }
    }

    # Summary
    Header "Preflight Summary"
    Write-Host "  WARNs : $warns"
    Write-Host "  FAILs : $fails"
    if ($fails -gt 0) {
        Fail "Preflight has FAIL items. Review before cloud-device smoke."
    } elseif ($warns -gt 0) {
        Warn "Preflight has WARN items. Review manually."
    } else {
        Ok "Preflight all clear."
    }
}

# ========================================================================
# ADB DEVICES
# ========================================================================
function Do-AdbDevices {
    Header "ADB Devices"
    $adb = Find-Adb
    if (-not $adb) {
        Warn "adb not found on PATH or in SDK default locations."
        Warn "Cloud-device manual alternative:"
        Warn "  1. Connect cloud device via vendor remote-control client."
        Warn "  2. Use vendor-provided install/logcat UI."
        Warn "  3. Record results manually using the runbook in docs/testing/"
        return
    }
    Info "Running: & '$adb' devices"
    & $adb devices 2>&1
}

# ========================================================================
# INSTALL
# ========================================================================
function Do-Install {
    Header "Install APK"
    $adb = Find-Adb
    if (-not $adb) {
        Fail "adb not found. Cannot install APK."
        return
    }
    $apk = Join-Path (Get-Location).Path "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path -LiteralPath $apk)) {
        Fail "APK not found: $apk"
        return
    }
    Info "Installing: $apk"
    & $adb install -r $apk 2>&1
    if ($LASTEXITCODE -eq 0) {
        Ok "APK installed successfully."
    } else {
        Fail "adb install failed (exit code: $LASTEXITCODE)."
    }
}

# ========================================================================
# LAUNCH
# ========================================================================
function Do-Launch {
    Header "Launch ClassMate"
    $adb = Find-Adb
    if (-not $adb) {
        Fail "adb not found. Cannot launch app."
        return
    }
    # Common package/activity — adjust if different
    $pkg = "com.classmate.app"
    $act = "com.classmate.app.MainActivity"
    $launch = "$pkg/$act"
    Info "Attempting: am start -n $launch"
    $result = & $adb shell am start -n $launch 2>&1
    Write-Host $result
    if ($LASTEXITCODE -eq 0) {
        Ok "Launch command sent."
    } else {
        Warn "Launch may have failed."
        Warn "Try manually: adb shell am start -n $launch"
    }
}

# ========================================================================
# LOGCAT ON DEVICE
# ========================================================================
function Do-LogcatOnDevice {
    Header "Logcat — OnDevice / LLM / Vivo / ClassMate"
    Ensure-OutputDir

    $adb = Find-Adb
    if (-not $adb) {
        Fail "adb not found. Cannot capture logcat."
        return
    }

    # Clear logcat buffer first for clean capture
    & $adb logcat -c 2>$null
    Info "Logcat buffer cleared. Capturing for 5 seconds..."

    $logFile = Join-Path $outDir "logcat_ondevice_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
    $filters = "ondevice|llm|vivo|classmate|bluelm|llmsdk|classmate"
    $result = & $adb logcat -d -e $filters 2>&1
    if ($result) {
        $result | Out-File -LiteralPath $logFile -Encoding utf8
        Ok "Logcat saved to: $logFile"
        $lines = ($result | Measure-Object -Line).Lines
        Info "Captured $lines matching lines."
    } else {
        Info "No matching logcat lines captured."
        "No matching logcat lines at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')." | Out-File -LiteralPath $logFile -Encoding utf8
    }
}

# ========================================================================
# PULL SCREENSHOTS
# ========================================================================
function Do-PullScreenshots {
    Header "Pull Screenshots (best-effort)"
    Ensure-OutputDir

    $adb = Find-Adb
    if (-not $adb) {
        Fail "adb not found. Cannot pull screenshots."
        return
    }

    $remotePaths = @(
        "/sdcard/Pictures/Screenshots/",
        "/sdcard/DCIM/Screenshots/",
        "/sdcard/Screenshots/"
    )

    $found = $false
    foreach ($rp in $remotePaths) {
        $check = & $adb shell ls $rp 2>&1
        if ($LASTEXITCODE -eq 0 -and $check -notmatch "No such file") {
            Info "Found screenshots at: $rp"
            $ts = Get-Date -Format 'yyyyMMdd_HHmmss'
            $localDir = Join-Path $outDir "screenshots_$ts"
            New-Item -ItemType Directory -Path $localDir -Force | Out-Null
            & $adb pull $rp $localDir 2>&1
            if ($LASTEXITCODE -eq 0) {
                Ok "Screenshots pulled to: $localDir"
                $found = $true
            } else {
                Warn "adb pull from $rp failed."
            }
            break
        }
    }

    if (-not $found) {
        Warn "No screenshot directories found on device."
        Warn "Take screenshots manually and place in: $outDir"
    }
}

# ========================================================================
# ALL LIGHT
# ========================================================================
function Do-AllLight {
    Do-Info
    Do-Preflight
    Do-AdbDevices
}

# ========================================================================
# DISPATCH
# ========================================================================

# If no parameter at all, show help
if (-not ($Info -or $Preflight -or $AdbDevices -or $Install -or $Launch -or $LogcatOnDevice -or $PullScreenshots -or $AllLight -or $Help)) {
    Show-Help
    exit 0
}

if ($Help) { Show-Help; exit 0 }

Write-Host "== Stage 8A-7 Device Wait Helper ==" -ForegroundColor Magenta
Write-Host "Read-only WARN-only. No Gradle. No config.local.json read. No file copy into repo."
Write-Host "Output dir: qa_out/stage8a7_logs/"
Write-Host ""

if ($AllLight) {
    Do-AllLight
} else {
    if ($Info)           { Do-Info }
    if ($Preflight)      { Do-Preflight }
    if ($AdbDevices)     { Do-AdbDevices }
    if ($Install)        { Do-Install }
    if ($Launch)         { Do-Launch }
    if ($LogcatOnDevice) { Do-LogcatOnDevice }
    if ($PullScreenshots){ Do-PullScreenshots }
}

Write-Host "`n== Stage 8A-7 Helper finished ==" -ForegroundColor Magenta
