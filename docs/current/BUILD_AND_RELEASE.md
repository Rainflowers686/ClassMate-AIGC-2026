# Build and Release

## 验证命令

```powershell
cd "D:\Edge Download\AIGC\ClassMate"
git diff --check
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
```

## 打包命令

```powershell
cd "D:\Edge Download\AIGC\ClassMate"
git pull
.\gradlew.bat clean :app:assembleDebug --no-daemon
$commit = git rev-parse --short HEAD
Copy-Item "app\build\outputs\apk\debug\app-debug.apk" "ClassMate-debug-v1.14.2-$commit.apk"
explorer .
```

## 代理 push 命令

如需代理环境，先在本机配置代理，再执行：

```powershell
git status --short
git push origin feature/audio-official-loop-hardening-v1
```

不要 push 未通过验证的提交。

## 禁止提交

- `config.local.json`
- AAR/APK/AAB
- 字体文件
- keystore / key
- OfficialDemos
- 真实 AppKey / Authorization / Bearer 值
