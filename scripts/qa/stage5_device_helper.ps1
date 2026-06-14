param(
    [switch]$Info,
    [switch]$FindAdb,
    [switch]$Devices,
    [switch]$Install,
    [switch]$Launch,
    [switch]$Logcat,
    [switch]$ListExports,
    [switch]$PullExports,
    [switch]$CleanQaExports,
    [switch]$AllLight
)

$ErrorActionPreference = "Continue"
$PackageName = "com.classmate.app"
$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
$QaExportDir = "qa_exports"
$script:AdbPath = $null
$script:Failures = New-Object System.Collections.Generic.List[string]

function Show-Help {
    Write-Host "Stage 5 device helper"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -Info"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -FindAdb"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -Devices"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -Install"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -Launch"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -Logcat"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -ListExports"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -PullExports"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -CleanQaExports"
    Write-Host "  .\scripts\qa\stage5_device_helper.ps1 -AllLight"
    Write-Host ""
    Write-Host "No parameters: help only. No install, launch, pull, or cleanup is performed."
}

function Step($Name) {
    Write-Host "`n== $Name ==" -ForegroundColor Cyan
}

function Pass($Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Fail($Message) {
    $script:Failures.Add($Message) | Out-Null
    Write-Host "FAIL: $Message" -ForegroundColor Red
}

function Resolve-Adb {
    if ($script:AdbPath -and (Test-Path $script:AdbPath)) {
        return $script:AdbPath
    }

    $pathAdb = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($pathAdb) {
        $script:AdbPath = $pathAdb.Source
        return $script:AdbPath
    }

    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
        "D:\AAAbiancheng\Android\SDK\platform-tools\adb.exe"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $script:AdbPath = $candidate
            return $script:AdbPath
        }
    }

    return $null
}

function Run-Info {
    Step "Info"
    Write-Host "Branch:"
    git branch --show-current
    Write-Host "`nRecent commit:"
    git log --oneline -1
    Write-Host "`nAPK:"
    if (Test-Path $ApkPath) {
        Get-Item $ApkPath | Select-Object FullName, Length, LastWriteTime | Format-List
        Pass "APK found"
    } else {
        Fail "APK not found at $ApkPath"
    }
}

function Run-FindAdb {
    Step "Find adb"
    $adb = Resolve-Adb
    if ($adb) {
        Write-Host $adb
        Pass "adb.exe found"
    } else {
        Fail "adb.exe not found"
        Write-Host "Common locations:"
        Write-Host "  PATH"
        Write-Host "  $env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        Write-Host "  D:\AAAbiancheng\Android\SDK\platform-tools\adb.exe"
    }
}

function Run-Devices {
    Step "Devices"
    $adb = Resolve-Adb
    if (!$adb) {
        Fail "adb.exe not found"
        return
    }
    & $adb devices
    if ($LASTEXITCODE -eq 0) { Pass "adb devices completed" } else { Fail "adb devices failed" }
}

function Run-Install {
    Step "Install APK"
    $adb = Resolve-Adb
    if (!$adb) {
        Fail "adb.exe not found"
        return
    }
    if (!(Test-Path $ApkPath)) {
        Fail "APK not found at $ApkPath"
        return
    }
    & $adb install -r $ApkPath
    if ($LASTEXITCODE -eq 0) { Pass "APK installed" } else { Fail "APK install failed" }
}

function Run-Launch {
    Step "Launch app"
    $adb = Resolve-Adb
    if (!$adb) {
        Fail "adb.exe not found"
        return
    }
    & $adb shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1
    if ($LASTEXITCODE -eq 0) { Pass "Launch command sent" } else { Fail "Launch failed" }
}

function Run-LogcatHint {
    Step "Logcat hint"
    $adb = Resolve-Adb
    if (!$adb) {
        Fail "adb.exe not found"
        return
    }
    Write-Host "Use this only for live observation. Do not save device logs into the repository."
    Write-Host "`"$adb`" logcat | Select-String -Pattern `"ClassMate|$PackageName`""
    Pass "Logcat hint printed"
}

function Run-ListExports {
    Step "List app-private exports"
    $adb = Resolve-Adb
    if (!$adb) {
        Fail "adb.exe not found"
        return
    }
    Write-Host "Trying run-as. Some cloud devices disable this for debug builds."
    & $adb shell run-as $PackageName sh -c "ls -la files/exports 2>/dev/null; ls -la cache/export_share 2>/dev/null"
    if ($LASTEXITCODE -eq 0) {
        Pass "Listed app-private export folders"
    } else {
        Fail "run-as listing failed"
        Write-Host "Use the App Export Center: Save to file or system share."
    }
}

function Run-PullExports {
    Step "Pull app-private exports"
    $adb = Resolve-Adb
    if (!$adb) {
        Fail "adb.exe not found"
        return
    }
    if (!(Test-Path $QaExportDir)) {
        New-Item -ItemType Directory -Path $QaExportDir | Out-Null
    }
    $archive = Join-Path $QaExportDir "classmate_app_private_exports.tar"
    Write-Host "Creating local archive: $archive"
    cmd /c "`"$adb`" exec-out run-as $PackageName sh -c `"cd files/exports 2>/dev/null && tar -cf - .`" > `"$archive`""
    if ($LASTEXITCODE -ne 0 -or !(Test-Path $archive) -or (Get-Item $archive).Length -eq 0) {
        Fail "Pull failed or no app-private exports found"
        if (Test-Path $archive) { Remove-Item -LiteralPath $archive -Force }
        Write-Host "Use Export Center to save to a visible folder or share through the system panel."
        return
    }
    Pass "Pulled app-private exports archive"
    Write-Host "Archive: $archive"
}

function Run-CleanQaExports {
    Step "Clean local qa_exports"
    if (Test-Path $QaExportDir) {
        Remove-Item -LiteralPath $QaExportDir -Recurse -Force
        Pass "Removed $QaExportDir"
    } else {
        Pass "$QaExportDir does not exist"
    }
}

$hasAny =
    $Info -or $FindAdb -or $Devices -or $Install -or $Launch -or $Logcat -or
    $ListExports -or $PullExports -or $CleanQaExports -or $AllLight

if (!$hasAny) {
    Show-Help
    exit 0
}

if ($AllLight) {
    Run-Info
    Run-FindAdb
    Run-Devices
} else {
    if ($Info) { Run-Info }
    if ($FindAdb) { Run-FindAdb }
    if ($Devices) { Run-Devices }
    if ($Install) { Run-Install }
    if ($Launch) { Run-Launch }
    if ($Logcat) { Run-LogcatHint }
    if ($ListExports) { Run-ListExports }
    if ($PullExports) { Run-PullExports }
    if ($CleanQaExports) { Run-CleanQaExports }
}

Step "Summary"
if ($script:Failures.Count -eq 0) {
    Write-Host "DEVICE HELPER PASS" -ForegroundColor Green
    exit 0
}

Write-Host "DEVICE HELPER FINISHED WITH FAILURES" -ForegroundColor Red
$script:Failures | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
exit 1

