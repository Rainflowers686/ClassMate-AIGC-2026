# Stage 8A OnDevice — 生产代码提交前 Checklist

> 明天 Claude/Opus 完成真实 reflection bridge 后、commit 前逐项核对。
> 全部通过后再 `git commit`。不运行 Gradle（仅列出命令）。

---

## A. 必跑命令

### 编译与测试

- [ ] A-01 `./gradlew :core:test` — core 模块测试全部通过
- [ ] A-02 `./gradlew :app:testDebugUnitTest` — app 模块测试全部通过
- [ ] A-03 `./gradlew :app:assembleDebug` — 编译通过（AAR 存在时）
- [ ] A-04 临时移除 AAR 后 `./gradlew :app:assembleDebug` — 编译通过（AAR 缺失时端侧自动降级）

### 安全扫描

- [ ] A-05 `powershell -ExecutionPolicy Bypass -File scripts/secrets_scan/secrets_scan.ps1` — `OK: no secrets`
- [ ] A-06 `powershell -ExecutionPolicy Bypass -File scripts/qa/stage8_ondevice_static_audit.ps1` — 无新增 BLOCKER
- [ ] A-07 `powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a4_post_bridge_static_check.ps1` — 关键 BLOCKER 项全部 PASS

### Git

- [ ] A-08 `git diff --check` — 无空白问题

---

## B. 必查文件

### 不应被修改的文件

- [ ] B-01 `app/src/main/AndroidManifest.xml` — 无新增危险权限
- [ ] B-02 `core/src/.../provider/BlueLMProvider.kt` — 云端主路径未被改动
- [ ] B-03 `core/src/.../provider/ProviderResolver.kt` — 链顺序未被改动
- [ ] B-04 `core/src/.../safety/*.kt` — validators 未被削弱
- [ ] B-05 `core/src/.../export/*.kt` — redaction pipeline 未被削弱

### 应被修改/新增的文件（预期范围）

- [ ] B-06 `core/src/.../ondevice/OnDeviceLlmProvider.kt` — 端侧 Provider（新增/修改）
- [ ] B-07 `core/src/.../ondevice/LocalProviderChain.kt` — Provider Chain 注册端侧（修改）
- [ ] B-08 `core/src/.../ondevice/*Reflection*.kt` — reflection bridge 类（新增）
- [ ] B-09 `app/src/.../settings/*Diagnostic*.kt` — Settings 诊断页面（新增/修改）

### 禁止出现的模式

- [ ] B-10 `import com.vivo.llmsdk.*` — 生产代码中不应存在
- [ ] B-11 `onComplete(LlmStats` — 旧签名不应存在
- [ ] B-12 `MANAGE_EXTERNAL_STORAGE` — Manifest 中不应存在
- [ ] B-13 `"apiKey"` / `"appKey"` 等真实密钥 — 源码中不应存在（测试 fixture 除外）
- [ ] B-14 `enable_thinking = true` — 不应存在（guard 应为 `false`）

---

## C. 禁止暂存的文件

以下文件**绝对不能** `git add`：

- [ ] C-01 `app/libs/llm-sdk-release.aar` — gitignored
- [ ] C-02 `app/libs/**/*.so` — gitignored
- [ ] C-03 `config.local.json` — gitignored
- [ ] C-04 `local.properties` — gitignored
- [ ] C-05 `secrets.properties` — gitignored
- [ ] C-06 `.env` / `.env.*` — gitignored
- [ ] C-07 `*.jks` / `*.keystore` — gitignored
- [ ] C-08 `*.apk` / `*.aab` — gitignored
- [ ] C-09 `app/build/` / `core/build/` / `build/` / `.gradle/` — gitignored

验证：
```bash
git status --short
# 确认以上路径均未出现在输出中
```

---

## D. git add 范围建议

### Commit A: 生产代码

```bash
# 仅暂存端侧 bridge 相关的生产代码
git add core/src/main/kotlin/com/classmate/core/ondevice/
git add core/src/main/kotlin/com/classmate/core/provider/   # 仅如果 ProviderResolver/Chain 有改动
git add app/src/main/java/com/classmate/app/ui/screens/settings/  # 仅 Settings 诊断页面

# 审慎检查：逐个确认变更
git diff --cached --stat
git diff --cached
```

### Commit B: 文档与脚本（如果本轮也提交文档）

```bash
# 暂存本轮 docs 和 scripts
git add docs/testing/stage8a*.md
git add docs/competition/stage8a*.md
git add docs/product/stage8a*.md
git add docs/architecture/stage8a*.md
git add docs/prompts/claude_stage8a*.md
git add docs/issues/stage8a*.md
git add docs/pr/stage8*.md
git add docs/release/stage8*.md
git add scripts/qa/stage8a*.ps1
git add scripts/proof/build_stage8_ondevice_proof_pack.ps1
git add docs/INDEX.md
```

---

## E. Commit Message 建议

### Commit A
```
feat(ondevice): wire real BlueLM 3B SDK bridge via reflection

- Add LlmManager/LlmConfig/TokenCallback reflection bridge
- Wire OnDeviceBlueLM provider into ProviderChain (priority: ondevice > cloud > local)
- Add Settings diagnostic page with real-time SDK status
- Support experimental multimodal VIT encoding (diagnostic only)
- Ensure CI-safe: AAR absence triggers graceful degradation, not build failure

Security:
- No direct import of com.vivo.llmsdk
- No new dangerous permissions in Manifest
- Ondevice text/images never leave device
- Prompt/output not persisted or exported

Does NOT:
- Commit llm-sdk-release.aar (gitignored)
- Wire multimodal into Ask/Report/Practice main path
- Alter cloud BlueLM provider or weaken validators
```

### Commit B
```
docs(ondevice): add Stage 8A milestone PR and release drafts

- Add PR description, release notes, competition one-pager
- Add user story map (5 roles)
- Add production commit checklist
- Add proof pack builder script
- Update INDEX.md
```

---

## F. Push 后 CI 检查

- [ ] F-01 CI 编译通过（`assembleDebug` 在无 AAR 环境下成功）
- [ ] F-02 CI 测试通过（`core:test` + `app:testDebugUnitTest`）
- [ ] F-03 CI secrets scan 通过
- [ ] F-04 PR 描述已填写（使用 `docs/pr/stage8_ondevice_pr_description.md` 内容）
- [ ] F-05 PR label 已添加（`ondevice`, `experimental`, `competition`）

---

## G. 真机 Smoke 检查（Push 后）

- [ ] G-01 APK 安装到云真机
- [ ] G-02 启动不崩溃
- [ ] G-03 Settings → 端侧诊断 → SDK_PRESENT = true
- [ ] G-04 端侧纯文本 generate 成功
- [ ] G-05 断网后纯文本 generate 成功
- [ ] G-06 fallback 正常工作（如果端侧不可用）
- [ ] G-07 截图 proof pack 所需的材料

---

## 最终确认

- [ ] 所有 A 项通过
- [ ] 所有 B 项确认
- [ ] 所有 C 项未暂存
- [ ] D 项范围正确
- [ ] E 项 commit message 准备就绪
- [ ] 签名：__________ 日期：__________
