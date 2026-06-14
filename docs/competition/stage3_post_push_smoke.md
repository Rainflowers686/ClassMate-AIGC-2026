# Stage 3 Post-Push Smoke Guide

生成时间：post-push QA 后，用于真机 smoke、Actions 复核和 proof 截图。

## 1. 当前 commit 列表

最近 8 个 commit：

```text
de44645 feat(ui): productize Live Companion with Flow visuals and design tokens
8efda8d docs(proof): add stage 3 competition materials
19db3e2 docs(design): add ClassMate theme handoff references
bd502c3 feat(product): add multimodal-ready learning shell
f08a492 feat(export): add report export mind map and video recommendations
f1ff6e2 feat(review): persist adaptive learning tasks
c732868 chore(gitignore): ignore generated bin output
d0f3c7e feat(app): add product navigation history and provider profiles
```

当前分支：

```text
feature/product-review-compatible
```

## 2. GitHub Actions 检查步骤

本机 `gh` 可用时：

```powershell
gh run list --limit 5
```

本次检查观察到：

- `Secrets Scan` for `feat(ui): productize Live Companion with Flow visuals and design tokens`：success。
- `Android CI` for `feat(ui): productize Live Companion with Flow visuals and design tokens`：in_progress。
- `Android CI` for `docs(proof): add stage 3 competition materials`：cancelled，原因是后续 push 触发了新 run。
- `Secrets Scan` for docs commit：success。

如果有 failed run：

```powershell
gh run view <run-id> --log-failed
```

手动检查 GitHub Actions 页面时看：

1. 最新 workflow 是否由最新 commit 触发。
2. `Android CI` 是否成功。
3. `Secrets Scan` 是否成功。
4. 若失败，截图失败 job 的 summary、失败 step、最后 40-80 行日志。
5. 不要截图任何 secret、token、账号隐私或本机配置。

## 3. APK 信息与安装步骤

当前本机 APK：

```text
D:\Edge Download\AIGC\ClassMate\app\build\outputs\apk\debug\app-debug.apk
Length: 9644925
LastWriteTime: 2026/6/2 13:20:18
```

### adb 可用时

确认设备：

```powershell
adb devices
```

安装：

```powershell
adb install -r "D:\Edge Download\AIGC\ClassMate\app\build\outputs\apk\debug\app-debug.apk"
```

如果安装失败，可先卸载再装：

```powershell
adb uninstall com.classmate.app
adb install "D:\Edge Download\AIGC\ClassMate\app\build\outputs\apk\debug\app-debug.apk"
```

### adb 不可用时

1. 用 USB、微信文件传输、网盘或浏览器下载把 `app-debug.apk` 传到手机。
2. 在手机文件管理器中打开 APK。
3. 按提示允许“安装未知来源应用”。
4. 安装后从桌面启动 ClassMate。

## 4. 真机 smoke test 最短路径

### 4.1 启动 App

- 操作：打开 ClassMate。
- 期望：进入 Home，界面正常，无崩溃。
- 截图：Home 首屏。

### 4.2 Settings 检查 Official BlueLM / Compatible / Local

- 操作：进入 Settings。
- 期望：能看到 Official BlueLM、Compatible Demo、Local Fallback 的状态和 provider order。
- 截图：Provider profile 卡片。
- 注意：不要录到 debug import 明文。

### 4.3 测试 BlueLM 连接

- 操作：确认本机 debug import 已导入 masked credential 后，点击 Test BlueLM connection。
- 期望：provider=BLUELM，status=OK，http_status=200 或安全短诊断。
- 截图：BlueLM diagnostic OK。
- 注意：不拍 AppID/AppKEY 明文。

### 4.4 Home 检查 Focus 风格

- 操作：回到 Home。
- 期望：Focus 风格清晰，入口可用，当前 mode 显示合理。
- 截图：Home。

### 4.5 Live Companion 添加片段、暂停、继续、结束

- 操作：Home -> Live Companion，输入标题，Start，添加 2-3 条公开样例片段，Pause，Continue，End class。
- 期望：明确显示手动/模拟转写；片段数正确；没有录音权限弹窗。
- 截图：Live status 和 honest ASR 提示。

### 4.6 生成知识时间线

- 操作：Live 结束后点击 Generate timeline。
- 期望：进入 Analyze，完成后进入 Knowledge Timeline。
- 截图：Analyze 和 Timeline 顶部。

### 4.7 Timeline 查看证据链

- 操作：点击一个知识点。
- 期望：进入 Evidence detail，能看到证据链和原文片段引用。
- 截图：Evidence detail。

### 4.8 Quiz 答题

- 操作：从 Timeline 进入 Quiz，回答至少 1 题。
- 期望：显示正确/错误反馈、解释和证据。
- 截图：Quiz 答题后页面。

### 4.9 Review 查看任务

- 操作：进入 Review。
- 期望：有 ReviewTask；Weakness Hub 可见或空态合理。
- 截图：Review 任务和 Weakness Hub。

### 4.10 History / Course Library 回看

- 操作：进入 History，查看最近记录和 Course Library。
- 期望：最近课堂可打开；课程聚合显示课堂次数、知识点、微测和待复习任务。
- 截图：History / Course Library。

### 4.11 Export 导出

- 操作：从 Home、History 或 Review 触发 Export。
- 期望：导出成功，显示文件名和安全路径摘要。
- 截图：Export 成功提示和报告片段。
- 检查：导出文件不含 key、Authorization、prompt/messages、reasoning_content。

## 5. 必须截图的 proof

1. BlueLM diagnostic OK。
2. qwen 分析成功日志或结果页。
3. Live 手动/模拟转写诚实提示。
4. Review 任务。
5. Weakness Hub。
6. History / Course Library。
7. Export 文件不含密钥的检查结果或脱敏报告片段。
8. Settings Capability Roadmap。

## 6. 不要拍到的内容

绝对不要出现在截图或录屏中：

- AppID/AppKEY/API key 明文。
- Debug import 输入框明文。
- `config.local.json`。
- Authorization 或 Bearer header。
- 完整 request body。
- 完整 prompt/messages。
- vendor response body。
- reasoning_content 全文。
- 本机隐私路径、账号、手机号、通知、浏览器账号信息。

## 7. 本地 post-push 轻量检查

已执行或建议执行：

```powershell
git status --short
git log --oneline -8
git branch --show-current
git diff --check
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
scripts\secrets_scan\secrets_scan.ps1
Get-Item app\build\outputs\apk\debug\app-debug.apk | Select-Object FullName,Length,LastWriteTime
```

期望：

- `git status --short` 干净，或只显示预期 docs 修改。
- `git diff --check` 无输出。
- 敏感追踪文件检查无输出。
- PowerShell secrets scan 显示 OK。

## 8. 当前风险备注

- 最新 Android CI 在本次检查时仍为 in_progress，需要刷新确认最终 success。
- 旧分支 `foundation/opus48-rebuild` 有一个历史 failed run，根因是早期 CI secret pattern allowlist 命中 README 安全文案，和当前 push 分支无关。
- 真机 proof 截图是下一步优先事项。
