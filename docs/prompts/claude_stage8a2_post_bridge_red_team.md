# Claude Stage 8A-2 Post-Bridge Red Team Review Prompt

> 在 Claude 完成真实 SDK reflection bridge 之后执行此审查。
> 审查定位：红队（adversarial），目标是在提交前找出所有风险点。
> 不要修改代码（除非发现 blocker），只输出审查报告。

---

## 审查范围

审查 bridge 实现涉及的所有文件，重点关注以下 13 个维度。

---

## 1. Direct Import 检查

- **问题**：生产代码中是否存在 `import com.vivo.llmsdk.*` 的直接引用？
- **为什么重要**：直接 import 会使编译依赖 AAR，如果 AAR 缺失则构建失败（破坏了 optional AAR 设计）
- **检查方法**：
  ```
  grep -r "import com\.vivo\.llmsdk" app/src core/src --include="*.kt"
  ```
- **通过标准**：无匹配。所有 SDK 调用通过 reflection bridge 间接进行。
- **发现时**：标记为 **BLOCKER**，要求改为 reflection 调用

---

## 2. Optional AAR CI-Safe 检查

- **问题**：如果 `app/libs/llm-sdk-release.aar` 不存在，项目是否能正常编译？
- **为什么重要**：CI 环境不能假设 AAR 存在（AAR 在 `.gitignore` 中）
- **检查方法**：
  - 临时重命名 `app/libs/llm-sdk-release.aar` → `app/libs/llm-sdk-release.aar.bak`
  - `./gradlew :app:assembleDebug`（或至少 `:app:compileDebugKotlin`）
  - 恢复 AAR
- **通过标准**：编译成功（或至少在端侧相关代码有防御性 `try-catch`/`Class.forName` 时不崩溃）
- **发现时**：标记为 **BLOCKER**，要求添加 AAR 缺失的编译期防御

---

## 3. TokenCallback 签名检查

- **问题**：bridge 中的 `TokenCallback` 实现是否使用新签名？
- **新签名**（正确）：
  - `onToken(String token)`
  - `onComplete()` — **无参数**
  - `onError(int code, String message)`
- **旧签名**（错误，会导致 NoSuchMethodError）：
  - `onComplete(LlmStats stats)` — 带 LlmStats 参数
- **检查方法**：
  ```
  grep -rn "onComplete" app/src core/src --include="*.kt"
  ```
- **通过标准**：所有 `onComplete` 实现/覆写无参数
- **发现时**：标记为 **BLOCKER**，要求修改为无参签名

---

## 4. callVit / multimodal 使用范围审查

- **问题**：`callVit` 和 `multimodal` 是否仅限于 diagnostic 页面使用？
- **为什么重要**：多模态目前是实验性功能，不应进入 Ask/Report/Practice 主链路
- **检查方法**：
  ```
  grep -rn "callVit\|multimodal" app/src core/src --include="*.kt"
  ```
- **通过标准**：
  - `callVit` 仅在 `*Diagnostic*` 或 `*Settings*` 相关文件中出现
  - `multimodal = true` 不硬编码在非 diagnostic 路径中
- **发现时**：标记为 **WARNING**（如果意外进入主链路则升级为 BLOCKER）

---

## 5. AAR 不提交确认

- **问题**：bridge 变更是否引入任何新的 `app/libs/` 追踪文件？
- **检查方法**：
  ```
  git ls-files app/libs/
  git check-ignore -v app/libs/llm-sdk-release.aar
  ```
- **通过标准**：
  - `git ls-files app/libs/` 无输出
  - `git check-ignore -v` 输出匹配 `.gitignore` 规则
- **发现时**：标记为 **BLOCKER**，`git rm --cached` 并确认 `.gitignore`

---

## 6. 权限审计

- **问题**：bridge 实现是否导致 Manifest 中新增危险权限？
- **检查方法**：
  ```
  grep -n "permission" app/src/main/AndroidManifest.xml
  ```
- **通过标准**：
  - 无 `MANAGE_EXTERNAL_STORAGE`
  - 无 `WRITE_EXTERNAL_STORAGE`
  - `READ_EXTERNAL_STORAGE` 仅在前已有（若无则不新增）
  - 无 `CAMERA`（除非前已存在）
- **发现时**：标记为 **BLOCKER**，移除新增的危险权限

---

## 7. Prompt / Output 不记录审查

- **问题**：bridge 实现是否在任何地方记录/导出原始 prompt 或完整模型输出？
- **检查方法**：
  - 搜索 `Log.d` / `Log.i` / `Log.v` 中是否包含 token 内容或 prompt 文本
  - 搜索 `println` / `print` 调试输出
  - 搜索文件写入（`FileWriter`、`writeText`、`appendText`）是否包含 prompt/output
- **通过标准**：日志中仅记录错误码、状态变更，不记录用户输入或模型输出内容
- **发现时**：标记为 **BLOCKER**，移除或替换为脱敏日志

---

## 8. BlueLM 云端主路径未改动审查

- **问题**：bridge 是否修改了云端 BlueLM 的请求路径/参数/Provider 实现？
- **检查方法**：
  - `git diff main -- core/src/.../provider/BlueLM*.kt`
  - 审查官方 BlueLM Provider 实现是否被改动
- **通过标准**：云端 BlueLM Provider 的请求逻辑未被改动（仅可能新增类，不修改已有类）
- **发现时**：标记为 **BLOCKER**，回退对云端 BlueLM Provider 的修改

---

## 9. qwen enable_thinking=false 保留审查

- **问题**：bridge 是否覆盖或移除了 `enable_thinking=false` 的 guard？
- **检查方法**：
  ```
  grep -rn "enable_thinking" app/src core/src --include="*.kt"
  ```
- **通过标准**：`enable_thinking` 设置存在于原有位置（通常为 `false`），未被 bridge 删除
- **发现时**：标记为 **BLOCKER**，恢复 `enable_thinking=false`

---

## 10. ProviderResolver / validators 未削弱审查

- **问题**：bridge 是否修改了 ProviderResolver 的链顺序或削弱了 validators？
- **检查方法**：
  - `git diff main -- core/src/.../provider/ProviderResolver.kt`
  - `git diff main -- core/src/.../safety/*.kt`
  - `git diff main -- core/src/.../export/*Validator*.kt`
- **通过标准**：
  - ProviderResolver 的解析顺序与 main 分支一致（仅可能新增端侧 Provider 注册，不改变优先级）
  - validators 的检查逻辑未被修改或弱化
  - 默认 Provider 仍为云端 BlueLM
- **发现时**：标记为 **BLOCKER**，回退非预期修改

---

## 11. Settings 不暴露外部模型作为主路径

- **问题**：Settings / 诊断页面是否出现了 "DeepSeek"、"Compatible Demo"、"外部模型" 等作为复赛主路径的文案？
- **检查方法**：
  ```
  grep -rni "DeepSeek\|Compatible\|外部模型" app/src core/src --include="*.kt"
  ```
- **通过标准**：
  - 如果命中出现在架构预留代码或注释中 → **WARNING**（确认不在 UI 展示路径中）
  - 如果命中出现在 Settings UI 文案中 → **BLOCKER**
- **发现时**：标记 Warning/Blocker 如上

---

## 12. Fallback 正确性审查

- **问题**：端侧不可用时是否正确 fallback 到下一级 Provider？
- **检查方法**：
  - 阅读 bridge 中 Provider.onDevice 不可用时的 fallback 逻辑
  - 确认异常被正确捕获且不向上抛出
- **通过标准**：
  - 端侧 Provider 失败 → 自动尝试云端 BlueLM
  - 云端不可用 → 自动尝试 LocalRule
  - 任何环节不崩溃，UI 显示当前 Provider 状态
  - fallback 日志仅记录错误码，不记录 prompt
- **发现时**：标记为 **BLOCKER**（如果 fallback 缺失或不正确）

---

## 13. 真机失败时文案诚实审查

- **问题**：Settings 诊断页面文案是否诚实反映了端侧的真实状态？
- **检查方法**：阅读 Settings 诊断页面的 UI 文案
- **通过标准**：
  - 不声称"端侧模型运行正常"当 init 失败时
  - 不声称"多模态就绪"当 callVit 不可用时
  - 使用条件判断显示状态（SDK_PRESENT / SDK_MISSING / init 返回码等）
  - 不使用硬编码的"成功"文案
- **发现时**：标记为 **WARNING**（需要改为条件化文案）

---

## 输出格式

请按以下格式输出审查报告：

```
## Red Team Review Report

### Blockers (must fix before commit)
- [ ] B-01: [问题描述] — 文件:行号 — 修复建议

### Warnings (should fix, not blocking)
- [ ] W-01: [问题描述] — 文件:行号 — 修复建议

### Passed
- ✓ 01: [维度名称] — 通过原因

### Summary
- Blockers: N
- Warnings: M
- Recommendation: [是否建议提交]
```

---

## 执行约束

- 只读审查，不修改代码（除非发现并确认要修复 blocker）
- 如果发现 BLOCKER，报告后等待人工确认再修复
- 不读取 `config.local.json` 内容
- 不运行 Gradle（审查为纯代码阅读）
