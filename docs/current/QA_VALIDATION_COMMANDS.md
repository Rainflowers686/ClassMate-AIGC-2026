# QA Validation Commands

## 基础验证

```powershell
git diff --check
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

## 项目预检

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
```

## 禁区检查

```powershell
git status --short
git diff --name-only | Select-String "config\.local\.json|\.aar$|\.apk$|\.aab$|\.ttf$|\.otf$|OfficialDemos|Authorization|SECRET|secret|token"
git diff | Select-String "AppKey|Authorization|SECRET|secret|config\.local\.json|LOCAL_FALLBACK|provider trace|raw id|assetId|kp_|q_|ev_|token"
```

说明：字段名和“禁止泄露项说明”可以出现；真实密钥值、用户演示正文中的 raw id 或 provider trace 不允许出现。

## 真机验证

按 [REAL_DEVICE_TEST_MANUAL_1_14_2.md](REAL_DEVICE_TEST_MANUAL_1_14_2.md) 执行，并保存失败截图。
