# Stage 8A-4 Post-Bridge Complete Regression Plan

> Claude 完成真实 reflection bridge 后的完整回归测试路线。
> 按依赖顺序排列。不声称端侧真机已跑通。不运行 Gradle（仅列出命令）。

**执行时间**：bridge 完成后
**执行人**：Claude / Opus
**预计耗时**：30–45 分钟（含真机）

---

## R-01: core:test

- **命令**：`./gradlew :core:test`
- **检查**：所有 core 模块单元测试通过
- **关键验证**：bridge 代码不破坏已有 Provider / Validator / Redaction 测试
- **失败处理**：回退 bridge 变更，确认是 bridge 引入的新失败再修复

---

## R-02: app:testDebugUnitTest

- **命令**：`./gradlew :app:testDebugUnitTest`
- **检查**：所有 app 模块单元测试通过
- **关键验证**：Settings ViewModel /诊断页面相关测试不因 bridge 修改而失败
- **失败处理**：同 R-01

---

## R-03: assembleDebug

- **命令**：`./gradlew :app:assembleDebug`
- **检查**：编译通过，无 unresolved reference 或 NoClassDefFoundError
- **关键验证**：
  - 无 `import com.vivo.llmsdk.*` 直接引用
  - AAR 通过 `implementation` 依赖正确链接
  - 无重复类冲突
- **失败处理**：检查 build.gradle.kts 中的 AAR 依赖配置

---

## R-04: secrets scan

- **命令**：`powershell -ExecutionPolicy Bypass -File scripts/secrets_scan/secrets_scan.ps1`
- **检查**：`OK: no secrets or forbidden files detected.`
- **关键验证**：bridge 代码不引入新密钥
- **失败处理**：排查引入密钥的文件，确认是误报还是真实泄露

---

## R-05: qwen guard

- **命令**：`powershell -ExecutionPolicy Bypass -File scripts/qa/stage8_ondevice_static_audit.ps1`
- **检查**：qwen guard 部分无新增 WARN
- **关键验证**：
  - bridge 代码中不出现 `qwen3.5-plus`
  - bridge 代码中不出现 `enable_thinking`（除非在 guard 注释中）
- **失败处理**：移除误引入的 qwen 参数

---

## R-06: no direct import com.vivo.llmsdk

- **命令**：
  ```bash
  rg "import com\.vivo\.llmsdk" app/src core/src --type kotlin 2>$null
  ```
- **检查**：无匹配
- **关键验证**：所有 SDK 调用通过 reflection bridge 间接进行，不直接 import SDK 类
- **失败处理**：将直接 import 改为 reflection bridge 调用

---

## R-07: AAR gitignored

- **命令**：
  ```bash
  git check-ignore -v app/libs/llm-sdk-release.aar
  git ls-files app/libs/llm-sdk-release.aar
  ```
- **检查**：
  - `git check-ignore -v` 输出匹配 `.gitignore:24:app/libs/*.aar`
  - `git ls-files` 无输出
- **失败处理**：如果 AAR 被追踪，`git rm --cached` 并确认 `.gitignore` 规则

---

## R-08: Settings text generation

- **操作**：Settings → 端侧诊断 → 纯文本测试
- **前置**：真机连接，APK 已安装
- **检查**：
  - SDK_PRESENT = true
  - init 返回 0
  - generate 触发 onToken 流式输出
  - onComplete() 无参数触发
- **失败处理**：按 `docs/product/stage8a3_ondevice_error_copy.md` 错误码分类处理

---

## R-09: Settings multimodal diagnostic

- **操作**：Settings → 端侧诊断 → 多模态测试
- **前置**：设备支持 VIT（以 SDK 返回为准）
- **检查**：
  - multimodal=true init 成功
  - callVit 返回 0
  - generate 输出图片描述
- **失败处理**：记录 callVit 返回码；若不可用，准备替代话术

---

## R-10: Ask fallback

- **操作**：Ask This Lesson 提问（确保端侧不可用）
- **检查**：自动 fallback 到云端 BlueLM，功能无变化
- **关键验证**：Ask 回答质量不因 bridge 引入而降级
- **失败处理**：检查 ProviderChain 顺序是否被 bridge 改动

---

## R-11: Report fallback

- **操作**：生成 StudyReport（确保端侧不可用）
- **检查**：报告正常生成，端侧不可用时走云端
- **关键验证**：报告内容结构和格式与 bridge 前一致
- **失败处理**：同 R-10

---

## R-12: Practice fallback

- **操作**：进入 Practice 模式做题（确保端侧不可用）
- **检查**：题目生成正常，端侧不可用时走云端
- **关键验证**：题目质量和类型与 bridge 前一致
- **失败处理**：同 R-10

---

## R-13: Export redaction

- **操作**：导出学习报告
- **检查**：
  - 报告中不包含原始 prompt
  - 报告中不包含完整模型输出（仅摘要）
  - 报告中不出现密钥
- **关键验证**：端侧 bridge 不破坏 redaction pipeline
- **失败处理**：检查 redaction filter 是否受 bridge 修改影响

---

## R-14: Manifest permission guard

- **命令**：
  ```bash
  Select-String -Path app/src/main/AndroidManifest.xml -Pattern "MANAGE_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE"
  ```
- **检查**：
  - `MANAGE_EXTERNAL_STORAGE` — 不应存在
  - `WRITE_EXTERNAL_STORAGE` — 不应存在
  - `READ_EXTERNAL_STORAGE` — 若存在需确认用途合理
- **失败处理**：移除 bridge 引入的非必要权限声明

---

## R-15: cloud BlueLM unchanged

- **操作**：使用云端 BlueLM 进行一次完整对话
- **检查**：云端模型行为和输出与 bridge 前一致
- **关键验证**：
  - 请求路径没有因 bridge 而被重路由
  - 云端模型配置没有被覆盖
- **失败处理**：检查 ProviderChain 的默认 Provider 是否为云端 BlueLM

---

## R-16: ProviderResolver unchanged

- **操作**：代码审查 + 单元测试
- **检查**：
  - ProviderResolver 的解析顺序未被 bridge 改动
  - 默认 Provider 仍为云端 BlueLM
  - 端侧 Provider 仅在 SDK 可用时注册
- **失败处理**：回退 ProviderResolver 的非预期修改

---

## R-17: validators unchanged

- **操作**：代码审查 + 单元测试
- **检查**：
  - SafetyValidator 未削弱
  - FormatValidator 未削弱
  - RedactionValidator 未削弱
- **关键验证**：bridge 代码不绕过任何 validator
- **失败处理**：确认 bridge 代码走正常 validator pipeline

---

## R-18: true-device smoke

- **操作**：在云真机上执行 `docs/testing/stage8a3_real_device_test_sheet.md` 中的 A/B 组测试
- **检查**：至少通过 A 组全部 + B 组核心项
- **关键验证**：真机上 SDK 初始化、纯文本生成、断网可用
- **失败处理**：记录失败项，按错误码分类，准备评委话术

---

## R-19: proof screenshots

- **操作**：按 `docs/competition/stage8a3_ondevice_proof_pack_checklist.md` 逐项截图
- **检查**：17 张截图全部完成，每张无敏感信息泄露
- **关键验证**：每张截图审查敏感信息（密钥、路径、学生数据）
- **失败处理**：某项功能不可用时，使用 checklist 中记载的替代方案

---

## R-20: fallback when SDK missing

- **操作**：
  1. 将 `app/libs/llm-sdk-release.aar` 移出 `app/libs/`（临时重命名）
  2. 重新构建（或重启应用如果不依赖 Gradle 重编译）
  3. 发送请求
- **检查**：
  - 应用不崩溃
  - SDK_MISSING 状态正确显示
  - 自动 fallback 到云端 BlueLM（或 LocalRule 若云端不可用）
  - 恢复 AAR 后端侧功能恢复正常
- **失败处理**：确保 SDK 缺失不是崩溃的触发条件

---

## 执行顺序建议

```
R-01 → R-02 → R-03 （编译+测试，最快发现桥接破坏）
  ↓
R-04 → R-05 → R-06 → R-07 （静态检查，无 Gradle）
  ↓
R-14 → R-15 → R-16 → R-17 （代码审查，防回退）
  ↓
R-08 → R-09 （真机端侧功能）
  ↓
R-10 → R-11 → R-12 → R-13 （fallback + redaction）
  ↓
R-18 → R-19 （真机 smoke + 截图）
  ↓
R-20 （破坏性测试，最后做）
```

---

## 结果记录

| 编号 | 项目 | 结果 | 备注 |
|------|------|------|------|
| R-01 | core:test | | |
| R-02 | app:testDebugUnitTest | | |
| R-03 | assembleDebug | | |
| R-04 | secrets scan | | |
| R-05 | qwen guard | | |
| R-06 | no direct import | | |
| R-07 | AAR gitignored | | |
| R-08 | Settings text gen | | |
| R-09 | Settings multimodal | | |
| R-10 | Ask fallback | | |
| R-11 | Report fallback | | |
| R-12 | Practice fallback | | |
| R-13 | Export redaction | | |
| R-14 | Manifest guard | | |
| R-15 | cloud BlueLM | | |
| R-16 | ProviderResolver | | |
| R-17 | validators | | |
| R-18 | device smoke | | |
| R-19 | proof screenshots | | |
| R-20 | SDK missing fallback | | |
