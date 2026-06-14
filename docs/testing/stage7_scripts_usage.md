# Stage 7 QA Scripts Usage

这些脚本只做轻量检查，不运行 Gradle，不替代本地单测、assemble 或 GitHub Actions。

## 推荐顺序

1. 状态快照：

```powershell
.\scripts\qa\stage7_status_snapshot.ps1
```

2. 演示前检查：

```powershell
.\scripts\qa\stage7_demo_preflight.ps1
```

3. 敏感文本和夸大宣传扫描：

```powershell
.\scripts\qa\stage7_sensitive_text_audit.ps1
```

## stage7_status_snapshot.ps1

用途：

- 打印当前分支和最近 commit。
- 打印工作区状态。
- 打印最新 debug APK 路径、大小、时间。
- 检查 `docs/competition`、`docs/testing`、`scripts/qa`。
- 只检查 `config.local.json` 是否存在，不读取内容。
- 检查常见敏感追踪文件是否被 Git 追踪。
- 检查 qwen guard。
- 检查 Manifest 中关键权限字符串。

结果处理：

- PASS：可以继续下一步。
- WARN：需要人工判断，比如本地配置存在但未读取。
- FAIL：先处理失败项。

## stage7_demo_preflight.ps1

用途：

- 检查工作区是否干净。
- 如果 `gh` 可用，列出当前分支最近 GitHub Actions run。
- 检查 APK 是否存在，以及是否可能旧于源码。
- 调用 secrets scan。
- 检查复赛演示关键文档是否存在。
- 给出“可演示 / 谨慎演示 / 不建议演示”的建议。

注意：

- 它不构建 APK。
- 如果工作区有 Claude 正在修改的半成品，可能会 WARN。
- 如果 APK 比源码旧，建议等构建恢复后重新 assemble。

## stage7_sensitive_text_audit.ps1

用途：

- 扫描 app/core/docs 中的敏感词和夸大宣传风险。
- 只输出 WARN，不直接阻断。
- 不读取本地配置。
- 不保存日志文件。

命中后怎么处理：

- 如果命中的是安全说明、扫描脚本或测试断言，人工确认即可。
- 如果命中真实凭据、完整鉴权信息、内部模型交互或夸大 ASR/OCR/声纹能力的文案，必须修正后再截图或提交。

## 不替代的检查

最终提交或真机测试前仍需运行：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
git diff --check
```

如果 Claude 正在并发改生产代码，先不要抢构建资源，等生产改动稳定后再跑完整验证。

