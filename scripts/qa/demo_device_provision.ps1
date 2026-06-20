param(
    [string]$PackageName = "com.classmate.app",
    [string]$ModelDir = "/sdcard/1225",
    [string]$ConfigPath = "config.local.json"
)

$ErrorActionPreference = "Stop"

function StatusLine {
    param(
        [string]$Name,
        [bool]$Ready,
        [string]$Message
    )
    $status = if ($Ready) { "READY" } else { "NOT_READY" }
    [pscustomobject]@{
        name = $Name
        ready = $Ready
        status = $status
        message = $Message
    }
}

function Invoke-AdbText {
    param([string[]]$Args)
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        return ""
    }
    try {
        $out = & adb @Args 2>$null
        return ($out -join "`n").Trim()
    } catch {
        return ""
    }
}

function Has-Device {
    $devices = Invoke-AdbText @("devices")
    return ($devices -split "`n" | Where-Object { $_ -match "\tdevice$" }).Count -gt 0
}

function App-Installed {
    param([string]$Package)
    $packages = Invoke-AdbText @("shell", "pm", "list", "packages", $Package)
    return $packages -match [regex]::Escape("package:$Package")
}

function Device-Path-Exists {
    param([string]$Path)
    $result = Invoke-AdbText @("shell", "if", "[", "-d", $Path, "];", "then", "echo", "YES;", "else", "echo", "NO;", "fi")
    return $result -match "YES"
}

function Permission-Granted {
    param(
        [string]$Package,
        [string]$Permission
    )
    $dump = Invoke-AdbText @("shell", "dumpsys", "package", $Package)
    return $dump -match ([regex]::Escape($Permission) + ".*granted=true")
}

function All-Files-Ready {
    param([string]$Package)
    $ops = Invoke-AdbText @("shell", "cmd", "appops", "get", $Package, "MANAGE_EXTERNAL_STORAGE")
    return $ops -match "allow"
}

function Network-Ready {
    $state = Invoke-AdbText @("shell", "cmd", "connectivity", "diagnostics", "internet")
    if ($state) { return $true }
    $wifi = Invoke-AdbText @("shell", "dumpsys", "connectivity")
    return $wifi -match "CONNECTED"
}

$adbReady = Has-Device
$configPresent = Test-Path -LiteralPath $ConfigPath -PathType Leaf
$appInstalled = if ($adbReady) { App-Installed $PackageName } else { $false }
$modelPresent = if ($adbReady) { Device-Path-Exists $ModelDir } else { $false }
$storageReady = if ($adbReady -and $appInstalled) { All-Files-Ready $PackageName } else { $false }
$recordAudioReady = if ($adbReady -and $appInstalled) { Permission-Granted $PackageName "android.permission.RECORD_AUDIO" } else { $false }
$cameraReady = if ($adbReady -and $appInstalled) { Permission-Granted $PackageName "android.permission.CAMERA" } else { $false }
$networkReady = if ($adbReady) { Network-Ready } else { $false }
$demoDataReady = (Test-Path -LiteralPath "docs/current/demo_script_l3_pipeline.md" -PathType Leaf)

$items = @(
    (StatusLine "ADB_DEVICE_CONNECTED" $adbReady "adb reports at least one connected device"),
    (StatusLine "APP_INSTALLED" $appInstalled "package presence only: $PackageName"),
    (StatusLine "CLOUD_CONFIG_PRESENT" $configPresent "config.local.json presence only; content is never read"),
    (StatusLine "ON_DEVICE_MODEL_PRESENT" $modelPresent "model directory presence only: $ModelDir"),
    (StatusLine "STORAGE_PERMISSION_READY" $storageReady "all-files access/app-op status for model directory and user-chosen materials"),
    (StatusLine "RECORD_AUDIO_READY" $recordAudioReady "runtime RECORD_AUDIO grant status"),
    (StatusLine "CAMERA_READY" $cameraReady "runtime CAMERA grant status"),
    (StatusLine "NETWORK_READY" $networkReady "generic device connectivity status; no endpoint is contacted"),
    (StatusLine "L3_DEMO_DATA_READY" $demoDataReady "local demo script exists")
)

$go = ($items | Where-Object { -not $_.ready }).Count -eq 0

Write-Host "ClassMate demo device provisioning readiness"
Write-Host "Package: $PackageName"
Write-Host "Model directory: $ModelDir"
Write-Host "No keys, auth headers, config contents, or endpoints are printed."
Write-Host ""
$items | ForEach-Object {
    $mark = if ($_.ready) { "GO" } else { "NO-GO" }
    Write-Host ("{0}: {1} - {2}" -f $_.name, $mark, $_.message)
}
Write-Host ""
if ($go) {
    Write-Host "FINAL: GO"
    exit 0
}

Write-Host "FINAL: NO-GO"
exit 1
