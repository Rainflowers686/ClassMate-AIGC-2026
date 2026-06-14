# Stage 5 Preflight 使用说明

运行命令：

```powershell
.\scripts\qa\stage5_preflight.ps1
```

这个脚本只做轻量预检查，不运行 Gradle，也不会读取 `config.local.json` 内容。它会检查：

- 当前分支和最近 commit。
- 工作区状态。
- `git diff --check`。
- 是否有本地配置、密钥文件、签名文件、APK/AAB、build 输出被 Git 追踪。
- PowerShell secrets scan。
- qwen3.5-plus 的 `enable_thinking=false` guard 是否仍在。
- Manifest 是否新增录音或外部存储危险权限。
- `docs/INDEX.md` 是否存在。

它不替代完整构建验证。正式提交或真机测试前仍需要运行：

```powershell
.\gradlew.bat :core
.\gradlew.bat :app
.\gradlew.bat :app
```

如果当前项目中这些缩写任务不可用，请改跑实际模块任务，例如：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

失败处理：

- 如果 forbidden tracked files 有输出，先停止提交，确认是否误追踪本地配置、构建产物或签名文件。
- 如果 secrets scan 失败，先不要截图、不要导出 proof，定位命中项并移除敏感内容。
- 如果 qwen guard 缺失，停止真机 BlueLM 长文本测试，先恢复关闭 thinking 的请求字段。
- 如果危险权限出现，确认是否误加入录音或外部存储权限；当前复赛测试不需要这些权限。

