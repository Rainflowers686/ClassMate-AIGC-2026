# Runs the same checks as CI, locally (Windows).
$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root

Write-Output "== core tests =="
& "$root\gradlew.bat" :core:test --console=plain
Write-Output "== app unit tests =="
& "$root\gradlew.bat" :app:testDebugUnitTest --console=plain
Write-Output "== assemble debug =="
& "$root\gradlew.bat" :app:assembleDebug --console=plain
Write-Output "== secrets scan =="
& powershell -ExecutionPolicy Bypass -File "$root\scripts\secrets_scan\secrets_scan.ps1"
Write-Output "All local checks passed."
