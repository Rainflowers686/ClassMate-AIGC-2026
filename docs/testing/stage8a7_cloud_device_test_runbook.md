# Stage 8A-7 Cloud Device Test Runbook

> 云真机端侧 BlueLM 3B 测试操作手册。按顺序执行。
> 当前状态：bridge 生产代码已完成，云真机排队中。**不代表真机已跑通。**

---

## A. 排队前准备

### A.1 代码确认

- [ ] 确认生产代码 commit 已提交到 `feature/product-review-compatible`
- [ ] 确认已推送（不需要 push 到 upstream 的话至少确认本地有最新 commit）
- [ ] 运行：
  ```
  git branch --show-current
  git log --oneline -3
  ```
- [ ] 记录当前 commit hash（用于截图编号和 bug triage）：
  ```
  git rev-parse --short HEAD
  ```

### A.2 APK 确认

- [ ] 确认 APK 存在：
  ```
  ls -la app/build/outputs/apk/debug/app-debug.apk
  ```
- [ ] 记录 APK 大小和时间。
- [ ] 如果不是最新的 APK，需要先本地 build（参考项目 README 或 CI）。

### A.3 AAR 确认

- [ ] 确认 AAR 在本地但未提交：
  ```
  ls -la app/libs/llm-sdk-release.aar
  git check-ignore -v app/libs/llm-sdk-release.aar
  ```
- [ ] 预期：AAR 存在、被 `.gitignore` 忽略、`git status` 不显示。

### A.4 Helper 脚本确认

- [ ] 运行 Info 测试：
  ```
  powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Info
  ```
- [ ] 运行 Preflight 测试：
  ```
  powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Preflight
  ```

### A.5 截图编号准备

- [ ] 按命名规范准备截图编号前缀（参考 `stage8a7_device_test_proof_naming.md`）
- [ ] 推荐格式：`{PROOF_TYPE}_{YYYYMMDD}_{SEQ}`，如 `SETTINGS_TEXT_INIT_SUCCESS_20260606_01`

---

## B. 进入云真机后

### B.1 连接确认

- [ ] 确认设备已连接：
  ```
  adb devices
  ```
  预期输出包含至少一台设备且状态为 `device`。
- [ ] 如果 `adb` 不存在，使用云真机平台自带的文件传输和控制界面。

### B.2 安装 APK

- [ ] 使用 helper 安装：
  ```
  powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Install
  ```
  或手动：
  ```
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- [ ] 确认安装成功（屏幕出现 ClassMate 图标）。

### B.3 打开 App

- [ ] 手动点击 ClassMate 图标打开，或用 helper：
  ```
  powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Launch
  ```
- [ ] 确认主界面正常加载。
- [ ] **截图 1**：主界面正常。（命名：`LAUNCH_SUCCESS_{DATE}_{SEQ}`）

### B.4 进入 Settings

- [ ] 从主界面导航到 Settings。
- [ ] 找到端侧模型（OnDevice / BlueLM 3B）相关区域。
- [ ] **截图 2**：Settings 端侧模型区域。（命名：`SETTINGS_ONDEVICE_SECTION_{DATE}_{SEQ}`）

### B.5 检查端侧模型路径

- [ ] 确认模型路径显示正确，通常为 `/sdcard/1225/` 或对应路径。
- [ ] 如果路径为空或错误，记录为 **model path not found** 失败。
- [ ] **截图 3**：模型路径显示。（命名：`SETTINGS_MODEL_PATH_{DATE}_{SEQ}`）

---

## C. 端侧纯文本测试

### C.1 操作步骤

1. 在 Settings → 端侧模型区域找到"纯文本测试"或"init/generate"入口。
2. 点击初始化（Init）按钮。
3. 观察状态提示。
4. 初始化成功后，在输入框输入固定测试问题：
   > "你好，请用一句话介绍你自己。"
5. 点击生成（Generate）按钮。
6. 等待输出 Token（注意超时时间约 10-15 秒）。
7. 观察 output 区域是否出现回复文本。

### C.2 预期 UI 状态

| 阶段 | 预期状态文案 | 预期颜色 |
|------|-------------|---------|
| 初始 | "SDK 已发现" / "模型路径: /sdcard/1225" | 绿色 |
| 初始化中 | "正在初始化..." | 黄色 |
| 初始化成功 | "初始化成功" | 绿色 |
| 生成中 | "生成中..." | 黄色 |
| 生成成功 | 显示 AI 回复文本 | 白色/默认 |
| 生成完成 | "完成" / TokenCallback.onComplete() | 绿色 |

### C.3 截图要求

- [ ] **截图 4**：初始化成功状态。（命名：`SETTINGS_TEXT_INIT_SUCCESS_{DATE}_{SEQ}`）
- [ ] **截图 5**：纯文本生成成功状态，含 input 和 output 文本。（命名：`SETTINGS_TEXT_GENERATE_SUCCESS_{DATE}_{SEQ}`）
- [ ] **截图 6（失败时）**：错误状态，含错误码和提示文案。（命名：`SETTINGS_TEXT_{ERROR_CODE}_{DATE}_{SEQ}`）

### C.4 错误码记录

如遇错误，记录：
- 错误码（如有数字 code）
- 错误文案（UI 上显示的完整文案）
- 发生的阶段（init / generate / complete）
- logcat 中对应的行

---

## D. 多模态 Diagnostic 测试

> **重要**：多模态当前仅在 Settings diagnostic 入口可用，**未接入学习主链**。

### D.1 前置条件

- [ ] 纯文本 init 成功（多模态共用同一个 LlmManager 实例）。
- [ ] Settings 中 multimodal 开关已打开（`LlmConfig.multimodal = true`）。

### D.2 测试步骤

1. 在 Settings 端侧模型区域打开多模态开关（如果有）。
2. 点击"多模态诊断"或类似入口。
3. 内置 2×2 像素测试图会自动生成（`Bitmap.createBitmap(2, 2, RGB_565)`）。
4. 点击 callVit 按钮。
5. 观察返回码。

### D.3 callVit 返回码判断

| 返回码 | 含义 | 后续操作 |
|--------|------|---------|
| 0 | VIT 编码成功 | 可以继续 generate |
| -1 或非 0 | VIT 编码失败 | **不要继续 generate**，记录失败 |

### D.4 callVit 成功时

- [ ] **截图 7**：callVit 成功状态，显示返回码 0。（命名：`SETTINGS_MULTIMODAL_CALLVIT_SUCCESS_{DATE}_{SEQ}`）
- [ ] 继续点击 generate，观察是否正常输出 Token。
- [ ] **截图 8**：多模态 generate 成功。（命名：`SETTINGS_MULTIMODAL_GENERATE_SUCCESS_{DATE}_{SEQ}`）

### D.5 callVit 失败时

- [ ] **不要继续 generate**。
- [ ] **截图 9**：callVit 失败状态，含返回码。（命名：`SETTINGS_MULTIMODAL_CALLVIT_FAILED_{DATE}_{SEQ}`）
- [ ] 记录返回码、logcat 行、UI 错误文案。
- [ ] 填写 bug triage 模板。

### D.6 截图要求

- 截图需包含完整 Settings 界面，能看到状态文案和返回码。
- 不暴露 `config.local.json` 中的密钥、AppID。
- 不暴露设备序列号、IMEI 等个人标识。

---

## E. Fallback 测试

### E.1 测试步骤

1. 如果端侧模型初始化失败，观察是否自动 fallback 到 LocalRule。
2. 在 LocalRule 模式下输入测试问题。
3. 观察是否返回基于规则的兜底回复（而非 AI 生成文本）。

### E.2 预期结果

- [ ] 端侧失败后 LocalRule 自动接管。
- [ ] UI 提示"当前使用本地规则兜底"或类似文案。
- [ ] **截图 10**：Fallback 模式。（命名：`FALLBACK_LOCAL_RULE_{DATE}_{SEQ}`）

### E.3 注意事项

- 端侧模型可用时不应触发 LocalRule。
- LocalRule 回复应有明确标识（如 `[LocalRule]` 前缀），避免与 AI 生成混淆。

---

## F. 普通学习主链回归

### F.1 测试范围

- [ ] 确认云端 BlueLM 正常可用（主路径）。
- [ ] 确认 Ask This Lesson 功能正常。
- [ ] 确认 Practice / Quiz 生成正常。
- [ ] 确认 Study Report 导出正常。
- [ ] 确认 ASR（录音转写）功能正常。
- [ ] 确认 OCR 导入功能正常。
- [ ] 确认 Export Center 正常。

### F.2 目的

确保端侧 bridge 的改动**没有破坏云端主路径和学习主链**。

- [ ] **截图 11-13**：主链路正常使用截图。（命名：`MAIN_PATH_{FEATURE}_OK_{DATE}_{SEQ}`）

---

## G. 失败分类与处理

### G.1 失败类型速查

| 失败类型 | 现场处理 | 是否阻塞演示 | Fallback 话术 | 后续 Issue |
|---------|---------|-------------|--------------|-----------|
| **SDK missing** | 确认 AAR 在 app/libs/ 下 | 是 | "端侧 SDK 文件未就绪，今天使用云端 BlueLM 演示" | Issue #1 |
| **native load failed** | 确认 arm64-v8a 设备，检查 `.so` | 是 | "设备芯片不兼容，端侧模型在 X300 Pro 需进一步适配" | Issue #3 |
| **model path not found** | 检查 `/sdcard/1225/` | 是 | "模型文件需预置到设备，今天演示使用云端" | Issue #1 |
| **config file not found** | 检查模型目录下配置文件 | 是 | "模型配置文件缺失，今天演示使用云端" | Issue #1 |
| **NPU/APU unavailable** | 确认设备芯片型号 | 否（可 CPU fallback） | "当前设备使用 CPU 推理，速度较慢但功能正常" | Issue #4 |
| **init timeout** | 检查 logcat native 加载部分 | 是 | "初始化超时，今天使用云端 BlueLM 演示" | Issue #2 |
| **generate timeout** | 检查模型是否正常 init | 是 | "生成超时，今天使用云端 BlueLM 演示" | Issue #6 |
| **callVit failed** | 确认 multimodal=true，Bitmap 格式 | 否（仅 diagnostic 入口） | "多模态编码测试未通过，纯文本推理正常" | Issue #5 |
| **permission denied** | 检查 Manifest，不添加危险权限 | 是 | "权限受限，这是设计预期的安全边界" | Issue #16 |
| **unknown** | 完整记录 logcat + UI + 步骤 | 视情况 | "端侧模型出现意外错误，正在排查" | Issue #20 |

### G.2 现场处理原则

1. **不慌**：端侧模型在云真机失败是正常情况。
2. **不修**：云真机上不修改任何生产代码。
3. **记录**：用 logcat、截图、bug triage 模板完整记录。
4. **不掩盖**：如实记录失败，不声称成功。

---

## H. 测后整理

### H.1 截图整理

- [ ] 所有截图按 proof 命名规范重命名。
- [ ] 放入 `qa_out/stage8a7_logs/screenshots/`。
- [ ] 对照 `stage8a7_device_test_proof_naming.md` 检查是否有遗漏。
- [ ] 标记成功的 screenshot 用于 proof pack。
- [ ] 标记失败的 screenshot 用于 bug triage。

### H.2 日志整理

- [ ] 确保 logcat 文件在 `qa_out/stage8a7_logs/` 下。
- [ ] 扫描 logcat 文件，确认未泄露密钥/AppID/AppKEY。
- [ ] 截取关键 logcat 行放入 bug triage 模板。

### H.3 Proof Pack 更新

- [ ] 如果有新的成功截图，更新 proof pack（参考 `scripts/proof/build_stage8_ondevice_proof_pack.ps1`）。
- [ ] 更新 proof 截图清单。

### H.4 Bug Report 输出

- [ ] 每个失败项填写 `stage8a7_ondevice_bug_triage_template.md`。
- [ ] 关联对应的 screenshot 和 logcat 文件。
- [ ] 判断是否阻塞复赛演示。

---

## I. 快速命令速查

```bash
# 查看项目信息
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Info

# 运行预检
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Preflight

# 全部轻量检查
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -AllLight

# 安装 APK
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Install

# 启动 App
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -Launch

# 抓取 logcat
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -LogcatOnDevice

# 拉取截图
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a7_device_wait_helper.ps1 -PullScreenshots

# 手动 adb
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -e "ondevice|llm|vivo|classmate"
```

---

## J. 完成标志

- [ ] 所有可执行的测试项均已尝试。
- [ ] 成功的项目有截图 proof。
- [ ] 失败的项目有 bug triage 记录。
- [ ] 学习主链回归通过。
- [ ] 日志和截图已整理归档。
- [ ] 更新 proof pack 清单。
