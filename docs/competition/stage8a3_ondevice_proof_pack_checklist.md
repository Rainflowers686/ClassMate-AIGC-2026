# Stage 8A-3 端侧模型 Proof Pack 清单

> 复赛材料 proof pack：证明端侧 BlueLM 3B 真实接入 SDK 并具有基本推理能力。
> 不声称端侧模型已经完整接入学习主链路。不展示 DeepSeek/Compatible 作为复赛主路径。

---

## 1. AAR 构建过程截图

- **文件名建议**：`01_aar_build_terminal.png`
- **截图位置**：终端窗口，显示 `./gradlew :llm-sdk:assembleRelease` 执行过程和 `BUILD SUCCESSFUL`
- **证明点**：AAR 由官方 demo 源码在本地构建，非预编译下载
- **不能暴露的信息**：
  - NDK/SDK 路径中的用户全名（如有，打码）
  - local.properties 中的绝对路径（截掉或虚化终端下半部分）
- **如果失败如何替代**：提供 AAR 文件目录的 `dir` 截图 + `javap` 验证截图作为间接证据

---

## 2. 英文路径构建截图

- **文件名建议**：`02_english_build_path.png`
- **截图位置**：文件管理器或终端，显示 `D:\AIGC_SDK\ondevice_llm_demo_src` 目录结构
- **证明点**：构建在无中文/无空格的英文路径下完成（避免 NDK 路径 bug）
- **不能暴露的信息**：
  - 上级目录中与项目无关的文件
- **如果失败如何替代**：提供 `pwd` 输出截图（终端）

---

## 3. local.properties 隐去敏感路径后的说明

- **文件名建议**：`03_local_properties_redacted.png` 或 `03_local_properties_redacted.txt`
- **截图位置**：编辑器打开 `local.properties`，用矩形覆盖敏感路径后截图
- **证明点**：
  - 文件存在且格式正确
  - sdk.dir 已配置
  - 路径已遮盖处理
- **不能暴露的信息**：
  - 完整的 SDK 路径（遮盖后只显示到 `...Android\Sdk`）
  - NDK 路径（遮盖后只显示到 `...ndk\版本号`）
- **如果失败如何替代**：文字说明 "local.properties 存在，sdk.dir 和 ndk.dir 已配置"

---

## 4. javap 多模态签名截图

- **文件名建议**：`04_javap_multimodal_signature.png`
- **截图位置**：终端，连续显示三组 javap 输出：
  - `LlmConfig` → 含 `multimodal` 字段
  - `LlmManager` → 含 `callVit(byte[], int, int)` 方法
  - `TokenCallback` → `onComplete()` 无参数
- **证明点**：SDK API 真实存在且匹配对接代码中的调用签名
- **不能暴露的信息**：
  - 除上述三组外的其他类（可能包含未公开内部实现）
- **如果失败如何替代**：分别截三张图，确保每张都包含 javap 命令和输出

---

## 5. git check-ignore AAR 截图

- **文件名建议**：`05_git_check_ignore_aar.png`
- **截图位置**：终端，显示 `git check-ignore -v app/libs/llm-sdk-release.aar` 输出
- **证明点**：AAR 被 `.gitignore` 规则覆盖，不会误提交
- **不能暴露的信息**：无
- **如果失败如何替代**：截图 `.gitignore` 文件中的 `app/libs/*.aar` 行

---

## 6. git status 不追踪 AAR 截图

- **文件名建议**：`06_git_status_no_aar.png`
- **截图位置**：终端，显示 `git status --short` 输出不包含 `app/libs/`
- **证明点**：AAR 在本地存在但未被 Git 追踪
- **不能暴露的信息**：无
- **如果失败如何替代**：截图 `git ls-files app/libs/` 无输出

---

## 7. Settings SDK_PRESENT 截图

- **文件名建议**：`07_settings_sdk_present.png`
- **截图位置**：手机 Settings → 模型管理 → 端侧诊断页面
- **证明点**：应用检测到端侧 SDK 可用，显示 SDK_PRESENT
- **不能暴露的信息**：
  - AppID / AppKEY 输入框中的内容（清空后再截图）
  - 其他模型配置中的真实值
- **如果失败如何替代**：
  - 使用诊断 API 的文本输出代替 UI 截图
  - 说明 SDK 检测结果（SDK_PRESENT / SDK_MISSING / SDK_VERSION_MISMATCH）

---

## 8. Settings init 成功截图

- **文件名建议**：`08_settings_init_success.png`
- **截图位置**：手机 Settings → 端侧诊断 → 初始化结果
- **证明点**：`LlmManager.init()` 返回 0，端侧模型加载成功
- **不能暴露的信息**：
  - 模型路径中的绝对路径（如有，遮盖）
- **如果失败如何替代**：
  - 截图错误码和错误描述
  - 文字说明失败原因并准备替代话术
  - 不影响其余 proof 材料 —— init 失败时 B/C 组测试均走 fallback

---

## 9. Settings 纯文本 generate 成功截图

- **文件名建议**：`09_settings_text_generate.png`
- **截图位置**：手机 Settings → 端侧诊断 → 纯文本测试结果
- **证明点**：端侧模型成功生成文本回复，显示 token 输出
- **不能暴露的信息**：
  - 如果 prompt 包含真实课程内容则打码
- **如果失败如何替代**：
  - 截图 onError 返回的错误码
  - 准备"端侧初始化成功但 generate 超时"的话术

---

## 10. Settings 多模态 callVit 成功截图

- **文件名建议**：`10_settings_callvit_success.png`
- **截图位置**：手机 Settings → 端侧诊断 → 多模态 → callVit 测试
- **证明点**：`LlmManager.callVit()` 返回 0，VIT 编码成功
- **不能暴露的信息**：
  - 测试图片内容（如果包含真实学生材料）
- **如果失败如何替代**：
  - 截图 callVit 返回码和错误信息
  - 说明多模态为实验性功能，callVit 在特定设备上可能不可用
  - 不影响纯文本端侧演示

---

## 11. Settings 多模态 generate 截图

- **文件名建议**：`11_settings_multimodal_generate.png`
- **截图位置**：手机 Settings → 端侧诊断 → 多模态 → generate 结果
- **证明点**：callVit 成功后，端侧模型生成图片相关描述
- **不能暴露的信息**：
  - 测试图片内容（使用内置色块或几何图形测试图）
- **如果失败如何替代**：
  - 展示纯文本生成成功 + callVit 成功
  - 说明 generate 阶段需要多模态 prompt 模板（明天 bridge 接入）
  - 不影响评分——多模态为加分项，非基础项

---

## 12. fallback LocalRule 截图

- **文件名建议**：`12_fallback_localrule.png`
- **截图位置**：手机 Settings → 模型管理 → 当前 Provider 路径显示
- **证明点**：端侧不可用时自动显示 LocalRule 兜底
- **不能暴露的信息**：
  - 错误日志中的完整调用栈（如有，截掉）
- **如果失败如何替代**：
  - 手动断网 + 禁用端侧，截图 Provider 选择器显示"本地规则"

---

## 13. 断网仍可端侧生成截图

- **文件名建议**：`13_offline_generate.png`
- **截图位置**：
  - 左/上：飞行模式图标
  - 右/下：端侧模型生成的文本
- **证明点**：端侧推理不依赖网络，离线可用
- **不能暴露的信息**：
  - 无
- **如果失败如何替代**：
  - 文字说明 + 抓包截图证明无外发请求
  - 截图端侧 generate 过程中 `adb shell dumpsys netstats` 显示无流量

---

## 14. 不新增危险存储权限截图

- **文件名建议**：`14_no_dangerous_storage_permission.png`
- **截图位置**：手机系统 Settings → 应用 → ClassMate → 权限列表
- **证明点**：不包含 `MANAGE_EXTERNAL_STORAGE` 或其他非必要权限
- **不能暴露的信息**：无
- **如果失败如何替代**：
  - 截图 `AndroidManifest.xml` 中权限声明部分
  - 截图 Settings → 应用权限 → 存储 = "未授予"或不存在

---

## 15. qwen enable_thinking=false guard 截图

- **文件名建议**：`15_qwen_guard.png`
- **截图位置**：终端，显示静态审计脚本的 qwen guard 检查结果
- **证明点**：`qwen3.5-plus` 和 `enable_thinking` 未在生产代码中残留
- **不能暴露的信息**：
  - 如果审计脚本在其他部分输出敏感行，截掉
- **如果失败如何替代**：
  - 截图源码中关键 guard 位置（如 request builder 中 `enable_thinking=false`）
  - 截图 `grep -r "enable_thinking" app/src core/src` 仅在 guard 相关文件出现

---

## 16. secrets scan 通过截图

- **文件名建议**：`16_secrets_scan_pass.png`
- **截图位置**：终端，显示 `OK: no secrets or forbidden files detected.`
- **证明点**：仓库中无硬编码密钥、无敏感配置被追踪
- **不能暴露的信息**：
  - 无（secrets_scan 本身不输出密钥内容）
- **如果失败如何替代**：
  - 如扫描发现命中，先排查是否为测试文件/文档示例（标注非真实密钥），修正后重新截图
  - 可附文字说明命中项的处理方式

---

## 17. app 不显示 DeepSeek / Compatible 作为复赛主路径截图

- **文件名建议**：`17_no_external_model_main_path.png`
- **截图位置**：
  - 手机 Settings → 模型管理 → 模型选择或 Provider 路径
  - 终端 `grep -n "DeepSeek\|Compatible" app/src/...` 输出（如果命中仅出现在非主路径或注释中）
- **证明点**：复赛展示路径中不出现 DeepSeek / Compatible 作为主路径
- **不能暴露的信息**：
  - 如 grep 命中在架构预留代码（非展示路径），需要解释上下文
- **如果失败如何替代**：
  - 截图代码中 provider chain 的展示分支逻辑
  - 截图 competition 展示用的配置代码片段

---

## Proof Pack 最终目录结构

```
proof_out/stage8a3_ondevice_proof_YYYYMMDD_HHmmss/
├── README.md
├── MANIFEST.txt
├── 00_status/
│   ├── git_status.txt
│   └── local_artifacts.txt
├── 01_screenshots/
│   ├── 01_aar_build_terminal.png
│   ├── 02_english_build_path.png
│   ├── 03_local_properties_redacted.png
│   ├── 04_javap_multimodal_signature.png
│   ├── 05_git_check_ignore_aar.png
│   ├── 06_git_status_no_aar.png
│   ├── ... (07-17 同上)
├── 02_docs/
│   ├── stage8a3_real_device_test_sheet.md
│   ├── stage8a3_ondevice_demo_script.md
│   ├── stage8a3_ondevice_error_copy.md
│   └── stage8a3_after_bridge_followups.md
├── 03_security/
│   └── security_summary.txt
└── 04_scripts/
    └── stage8_ondevice_static_audit.ps1
```

---

## 注意事项

1. 所有截图在拍摄前确认不包含真实密钥、完整路径、学生数据。
2. 如果某张截图对应的功能在真机上未通过，填写"实际结果"列并准备替代话术，不造假。
3. 录屏与截图分开管理，录屏文件较大不建议放入 proof pack zip。
4. 材料提交前运行 `scripts\secrets_scan\secrets_scan.ps1` 确认无敏感信息。
