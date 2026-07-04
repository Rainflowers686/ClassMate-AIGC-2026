# Build and Release Guide - ClassMate 1.14.18 / 131

## Build Debug APK

```powershell
cd "D:\Edge Download\AIGC\ClassMate"
.\gradlew.bat clean :app:assembleDebug --no-daemon
```

Output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Suggested final filename outside git:

```text
ClassMate-debug-v1.14.18-<commit>.apk
```

## Install

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Confirm Version

Open Settings -> Developer settings -> Diagnostics and logs. Confirm version, versionCode, commit, build time, and build variant.

## Do Not Commit

- `config.local.json`
- APK/AAB/AAR
- OfficialDemos
- fonts
- real AppKey, Authorization, Bearer, token, or screenshots containing credentials

## Final Preflight

Run:

```powershell
git diff --check
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\provider_live_smoke.ps1
```
