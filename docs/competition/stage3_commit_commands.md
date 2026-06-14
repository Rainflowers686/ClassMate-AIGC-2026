# Stage 3 Commit Commands

本文只提供命令模板，不执行 commit/push。  
不要 add `config.local.json`、`local.properties`、`.env`、keystore、APK/AAB 或 build outputs。

## 提交前必须跑的检查

Docs-only commit 前：

```powershell
git status --short
git diff --check
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
scripts\secrets_scan\secrets_scan.ps1
```

生产代码 commit 前再跑：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
git diff --check
```

## 方案 A：推荐，拆两个 commit

### Commit 1: docs commit

```powershell
git status --short
git add docs/competition/stage3-device-smoke-checklist.md
git add docs/competition/review-readiness.md
git add docs/competition/capability-matrix.md
git add docs/competition/security-proof-checklist.md
git add docs/competition/rematch-demo-script.md
git add docs/competition/device-recording-shot-list.md
git add docs/competition/judge-qna.md
git add docs/competition/feature-acceptance-matrix.md
git add docs/competition/competitive-reference.md
git add docs/competition/commit-split-suggestions.md
git add docs/competition/stage3_release_handoff.md
git add docs/competition/stage3_pr_description.md
git add docs/competition/stage3_commit_commands.md
git add docs/competition/stage3_issue_backlog.md
git add docs/design_refs/design-handoff-summary.md
git commit -m "docs(proof): add stage 3 competition materials"
```

### Commit 2: ui/product commit

确认 Claude Stage 3 文件无误后：

```powershell
git status --short
git add app/src/main/java/com/classmate/app/ui/screens/live/LiveCompanionScreen.kt
git add app/src/main/java/com/classmate/app/ui/components/ProductComponents.kt
git add app/src/main/java/com/classmate/app/ui/components/FlowComponents.kt
git add app/src/main/java/com/classmate/app/ui/design/Tokens.kt
git add app/src/main/java/com/classmate/app/ui/flow/
git add app/src/test/java/com/classmate/app/ui/flow/
git commit -m "feat(ui): productize Live Companion with Flow visuals and design tokens"
```

### Push

```powershell
git push
```

## 方案 B：赶时间，一个 commit

只有在时间非常紧、且所有检查都通过时使用：

```powershell
git status --short
git add docs/competition/stage3-device-smoke-checklist.md
git add docs/competition/review-readiness.md
git add docs/competition/capability-matrix.md
git add docs/competition/security-proof-checklist.md
git add docs/competition/rematch-demo-script.md
git add docs/competition/device-recording-shot-list.md
git add docs/competition/judge-qna.md
git add docs/competition/feature-acceptance-matrix.md
git add docs/competition/competitive-reference.md
git add docs/competition/commit-split-suggestions.md
git add docs/competition/stage3_release_handoff.md
git add docs/competition/stage3_pr_description.md
git add docs/competition/stage3_commit_commands.md
git add docs/competition/stage3_issue_backlog.md
git add docs/design_refs/design-handoff-summary.md
git add app/src/main/java/com/classmate/app/ui/screens/live/LiveCompanionScreen.kt
git add app/src/main/java/com/classmate/app/ui/components/ProductComponents.kt
git add app/src/main/java/com/classmate/app/ui/components/FlowComponents.kt
git add app/src/main/java/com/classmate/app/ui/design/Tokens.kt
git add app/src/main/java/com/classmate/app/ui/flow/
git add app/src/test/java/com/classmate/app/ui/flow/
git commit -m "feat(stage3): productize Live Companion and add proof materials"
git push
```

## 异常处理

### 如果 git ls-files 敏感追踪有输出

不要 commit。先确认输出文件是否应该被 Git 跟踪。若是本地配置、密钥、构建产物或 APK/AAB，必须从暂存区移除并修正 `.gitignore`：

```powershell
git restore --staged <file>
```

不要删除用户本地文件，除非用户明确要求。

### 如果 secrets_scan 红了

不要 commit。查看命中的文件名和字段，删除真实值或改为占位符。若真实 key 已经暴露给远端或第三方，先重置 key。

### 如果 Gradle 红了

不要把生产代码 commit 和 docs commit 混在一起。先提交 docs，生产代码等 Claude 修复后再提交。若失败来自测试期望变化，先确认没有改坏 provider、resolver、validators。

### 如果 GitHub Actions 红了

先不要打 tag 或发布 proof。读取失败 job 日志，优先检查：

- Android CI 是否是测试或构建失败。
- Secrets Scan 是否命中敏感字段。
- 是否误提交 build outputs、APK/AAB、本地配置。

修复后重新 push。
