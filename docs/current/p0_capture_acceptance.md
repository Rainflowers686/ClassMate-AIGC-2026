# P0 Capture Acceptance QA Plan

本文件用于验证 P0 主链是否在 App 侧真实可走。它不是功能设计文档，也不替代 Gradle 测试。测试目标是确认：

- 资料输入可以进入统一 AI Router。
- 图片 / 拍照可以进入 OCR + 端侧草稿的用户确认流。
- 音频 / 转写文本可以进入 ASR 或手动转写草稿的用户确认流。
- 确认后的内容进入 CourseAnalysis、Ask、Quiz、Review、Export 等既有学习链路。
- 未确认草稿不会落库。
- UI 文案保持诚实，不夸大未接入或实验能力。

## 1. 当前 P0 功能清单

已完成并需要验收：

- AI Capability Router 已接入 CourseAnalysis source。
- Ask source routing 使用统一 Cloud / On-device / Safe-placeholder 口径。
- 图片选择 / 拍照结果接入 `CaptureGateway.createImageStudyDraftRouted`。
- 图片草稿支持官方 OCR 文本与端侧蓝心语义草稿双轨展示。
- OCR 缺配置或失败时保留端侧草稿或手动编辑入口。
- 音频文件接入 `CaptureGateway.transcribeAudio`，用于 1739 长语音转写路径。
- ASR 缺配置或失败时提供粘贴转写文本入口。
- 手动转写文本可生成 `TranscriptDraft`，经编辑确认后进入资料篮。
- `AiProcessingDialog` 显示长任务步骤、来源、降级提示和手动编辑入口。
- Settings 已拆为设置首页、主题设置、模型接入、学习与导出、开发者选项。

暂不验收为已完成能力：

- vivo ASR / OCR 真实网络成功率，只有在本地配置和真机网络满足时才测。
- 实时 ASR 正式产品能力。
- 自动 OCR 产品闭环。
- Practice / Weakness / Export P1/P2 增强。
- Flow / 全局视觉重构。

## 2. 云真机 / 真机手动测试路径

建议设备：

- vivo 真机或云真机。
- 已安装当前 `app-debug.apk`。
- 若测端侧蓝心，确认模型目录授权与端侧诊断已通过。
- 若测官方 OCR / ASR，确认本地调试配置已导入，但测试时不要截图密钥输入页。

通用路径：

1. 安装 APK。
2. 打开 App。
3. 进入 Settings。
4. 检查模型接入页。
5. 返回首页。
6. 进入导入课堂资料。
7. 分别执行图片、拍照、音频、转写文本路径。
8. 每个草稿都必须先确认，再进入知识地图分析。
9. 生成 Timeline 后继续测 Ask / Quiz / Review / Export。

## 3. 图片 / 拍照导入测试

### 3.1 OCR configured

操作：

1. 确认官方 OCR / ASR 配置已导入且 Settings 模型接入页显示可用状态。
2. Import 选择图片学习输入。
3. 选择一张含课堂文字的课件截图或板书图片。
4. 等待 AI 处理弹窗完成。
5. 查看图片学习草稿。
6. 编辑草稿中的明显错字。
7. 点击确认并进入学习资料。
8. 点击生成知识地图。

预期：

- 出现“正在识别图片文字”一类处理状态。
- 草稿中能看到 OCR 文本，端侧语义草稿可作为辅助。
- 草稿必须可编辑。
- 未点击确认前，History / Course Library 不新增本次课程。
- 点击确认后，文本进入 CourseAnalysis。
- Timeline 中知识点证据能定位到确认后的文本。

失败记录字段：

- 设备型号 / 系统版本。
- 图片来源和大致内容。
- OCR 状态：configured / missing / failed。
- 处理弹窗停在哪一步。
- 是否生成草稿。
- 是否进入 CourseAnalysis。

截图命名：

- `p0_image_ocr_configured_01_picker.png`
- `p0_image_ocr_configured_02_processing.png`
- `p0_image_ocr_configured_03_draft.png`
- `p0_image_ocr_configured_04_timeline.png`

### 3.2 OCR ConfigMissing

操作：

1. 使用未配置官方 OCR 的环境，或切到不含 capture 配置的本地测试环境。
2. Import 选择图片学习输入。
3. 选择课件 / 板书图片。
4. 等待处理结束。

预期：

- 不崩溃。
- 出现“官方 OCR 未配置时，可继续使用端侧蓝心草稿或手动补充”同类提示。
- 如果端侧图片理解可用，应生成端侧草稿。
- 如果端侧不可用，应进入手动编辑。
- 仍需用户确认后才进入 CourseAnalysis。

截图命名：

- `p0_image_ocr_missing_01_processing.png`
- `p0_image_ocr_missing_02_ondevice_draft.png`
- `p0_image_ocr_missing_03_confirm.png`

### 3.3 On-device draft fallback

操作：

1. 准备无官方 OCR 配置但端侧蓝心可用的设备。
2. 选择一张含公式或板书结构的图片。
3. 等待端侧草稿。
4. 编辑草稿，把错误内容改正。
5. 确认进入 CourseAnalysis。

预期：

- 来源标签显示端侧。
- UI 不声称端侧图片语义就是 OCR。
- 用户编辑后的文本成为 CourseAnalysis 输入。
- Timeline 证据来自用户确认后的文本。

截图命名：

- `p0_image_ondevice_fallback_01_source.png`
- `p0_image_ondevice_fallback_02_edit.png`
- `p0_image_ondevice_fallback_03_analysis.png`

### 3.4 Confirm to CourseAnalysis

操作：

1. 图片草稿生成后不确认，直接返回。
2. 查看 History / Course Library。
3. 再次进入图片草稿，确认后生成知识地图。

预期：

- 未确认时不新增课程记录。
- 确认后才能进入 CourseAnalysis。
- CourseAnalysis 结果带来源状态，不显示为安全占位，除非双模型失败。

截图命名：

- `p0_image_confirm_gate_01_before_confirm.png`
- `p0_image_confirm_gate_02_after_confirm.png`

## 4. 音频 / 转写测试

### 4.1 ASR configured

操作：

1. 确认官方 ASR 配置可用。
2. Import 进入课堂转写 / 字幕入口。
3. 选择一段短课堂音频文件。
4. 等待转写任务状态完成。
5. 进入转写编辑器。
6. 修改 speaker、文本、分段。
7. 保存进入资料篮。
8. 生成知识地图。

预期：

- 音频文件进入 ASR 任务状态。
- 成功后生成 `TranscriptDraft`。
- 转写草稿可编辑。
- 保存后进入 Material Tray。
- 生成 Timeline 后，Evidence 可引用转写文本。

截图命名：

- `p0_audio_asr_configured_01_select.png`
- `p0_audio_asr_configured_02_processing.png`
- `p0_audio_asr_configured_03_editor.png`
- `p0_audio_asr_configured_04_timeline.png`

### 4.2 ASR ConfigMissing

操作：

1. 使用未配置官方 ASR 的环境。
2. 选择音频文件。
3. 观察提示。

预期：

- 不崩溃。
- 显示“官方 ASR 未配置，可粘贴转写文本继续”同类提示。
- 不把手动粘贴称作 ASR 成功。
- 可继续粘贴转写文本。

截图命名：

- `p0_audio_asr_missing_01_notice.png`
- `p0_audio_asr_missing_02_manual_entry.png`

### 4.3 Manual transcript fallback

操作：

1. 在转写页粘贴课堂转写文本。
2. 点击生成手动转写草稿。
3. 进入转写编辑器。
4. 编辑后保存进入资料篮。
5. 生成知识地图。

预期：

- 粘贴文本生成 `TranscriptDraft`。
- 来源是手动 / pasted transcript。
- 保存后进入资料篮。
- 确认后进入 CourseAnalysis。
- 未保存前不进入课程库。

截图命名：

- `p0_manual_transcript_01_paste.png`
- `p0_manual_transcript_02_editor.png`
- `p0_manual_transcript_03_tray.png`
- `p0_manual_transcript_04_timeline.png`

### 4.4 Confirm to CourseAnalysis

操作：

1. 生成转写草稿后不保存，返回资料篮。
2. 查看资料篮和 History。
3. 再次进入编辑器保存。
4. 生成知识地图。

预期：

- 未确认 / 未保存的草稿不参与分析。
- 保存后才进入 MaterialBundle。
- CourseAnalysis 使用保存后的转写文本。

截图命名：

- `p0_transcript_confirm_gate_01_before_save.png`
- `p0_transcript_confirm_gate_02_after_save.png`

## 5. AI Processing Dialog 测试

覆盖路径：

- 图片识别。
- 音频转写。
- CourseAnalysis 长任务。
- Ask 长任务，如当前页面已接入。

操作：

1. 触发长任务。
2. 截图处理弹窗。
3. 检查步骤、来源、降级提示。
4. 点击取消。
5. 重新触发并点击重试。
6. 在失败或缺配置时点击继续手动编辑。

预期：

- 标题清楚，例如正在识别图片文字、正在转写课堂音频、正在生成知识地图。
- 步骤包括准备资料、云端处理中、端侧兜底、整理结果或等待确认。
- 来源显示云端 / 端侧 / 手动。
- 失败提示不包含密钥、请求体、响应体。
- 继续手动编辑不丢失已有草稿。

截图命名：

- `p0_ai_processing_01_image.png`
- `p0_ai_processing_02_audio.png`
- `p0_ai_processing_03_fallback.png`

## 6. Settings 多级页面测试

操作：

1. 打开 Settings。
2. 检查默认显示设置首页，不再一屏堆满全部诊断。
3. 进入主题设置。
4. 进入模型接入。
5. 进入学习与导出。
6. 进入开发者选项。

预期：

- 设置首页有四个入口：主题设置、模型接入、学习与导出、开发者选项。
- 主题设置包含 Focus / Flow / Vitality 与授权循环背景音说明。
- 模型接入显示云端、端侧、OCR、ASR 状态与路由说明。
- 学习与导出不实现 P1 功能，只保留结构和偏好说明。
- 开发者选项收纳诊断、smoke、日志、BuildInfo。
- 不显示任何 key 内容。

截图命名：

- `p0_settings_01_home.png`
- `p0_settings_02_theme.png`
- `p0_settings_03_model_access.png`
- `p0_settings_04_learning_export.png`
- `p0_settings_05_developer.png`

## 7. CourseAnalysis Router 测试

操作：

1. 导入普通文本课程，生成知识地图。
2. 断网或使云端不可用，再导入文本课程。
3. 若端侧可用，观察是否切换端侧。
4. 若双模型都不可用，观察安全占位。

预期：

- 云端成功时，结果 source 为云端。
- 云端失败后，端侧可用时使用端侧学习草稿或分析。
- 双模型失败时显示安全占位，不把占位当智能结果。
- validator 不通过的结果不落库。

截图命名：

- `p0_router_01_cloud.png`
- `p0_router_02_ondevice_fallback.png`
- `p0_router_03_safe_placeholder.png`

## 8. Ask evidence-grounded 测试

操作：

1. 生成一个包含清楚证据的课程。
2. 问一个本节课有明确依据的问题。
3. 问一个只有部分依据的问题。
4. 问一个本节课没有依据的问题。
5. 在云端不可用时重复第 2 项。

预期：

- 有依据问题返回 grounded，并显示 evidence。
- 部分依据问题返回 partial 或同类状态。
- 无依据问题温和提示本节课资料中没有明确依据。
- 云端失败时可端侧基于 evidence 回答，或进入安全占位。
- 不记录完整 prompt、messages、vendor body、reasoning_content。

截图命名：

- `p0_ask_01_grounded.png`
- `p0_ask_02_partial.png`
- `p0_ask_03_not_found.png`
- `p0_ask_04_ondevice.png`

## 9. 禁止文案检查

测试目标：检查 UI、导出、History、日志中不要出现夸大或误导用户的表达。

不要出现的口径包括：

- 把端侧多模态描述成 OCR 的替代能力。
- 把 OCR 写成已经全自动完成。
- 把未正式接入的实时语音能力写成已经完成。
- 把手动粘贴转写文本写成 ASR 成功。
- 把安全占位写成智能分析结果。
- 把 demo-compatible 模式写成比赛主线。
- 把任何本地规则路径写成智能兜底。

允许出现的口径包括：

- 云端优先。
- 端侧兜底。
- 用户确认后生成知识地图。
- 官方 OCR 未配置时，可继续编辑端侧蓝心草稿。
- 官方 ASR 未配置时，可粘贴转写文本。
- 当前来源：云端 / 端侧 / 手动 / 安全占位。

截图命名：

- `p0_copy_01_import_honest.png`
- `p0_copy_02_settings_honest.png`
- `p0_copy_03_export_honest.png`

## 10. 截图命名清单

推荐目录：

`proof_assets/p0_capture_acceptance/yyyyMMdd_device/`

必拍：

- `p0_settings_01_home.png`
- `p0_settings_03_model_access.png`
- `p0_image_ocr_configured_03_draft.png`
- `p0_image_ondevice_fallback_02_edit.png`
- `p0_audio_asr_configured_03_editor.png`
- `p0_manual_transcript_03_tray.png`
- `p0_ai_processing_03_fallback.png`
- `p0_router_01_cloud.png`
- `p0_router_02_ondevice_fallback.png`
- `p0_ask_01_grounded.png`

不要拍：

- 完整 AppID / AppKEY / API key。
- Debug import 明文输入框。
- `config.local.json`。
- 完整日志。
- 完整 prompt / messages / reasoning_content。

## 11. 失败记录模板

| 编号 | 模块 | 场景 | 设备 | 网络 | 配置状态 | 操作步骤 | 预期 | 实际 | 截图文件 | 日志关键词 | 严重级别 | 是否阻塞 P0 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0-001 | Image Capture | OCR configured |  |  |  |  |  |  |  |  | High / Medium / Low | Yes / No |
| P0-002 | Audio Capture | ASR ConfigMissing |  |  |  |  |  |  |  |  | High / Medium / Low | Yes / No |
| P0-003 | Ask | no evidence |  |  |  |  |  |  |  |  | High / Medium / Low | Yes / No |

排查顺序：

1. 确认当前 APK 是否最新。
2. 确认 Settings 模型接入状态。
3. 确认端侧诊断是否通过。
4. 确认是否已用户确认草稿。
5. 确认 History / Course Library 是否新增记录。
6. 确认 preflight 是否通过。

## 12. 回归测试命令

轻量检查：

```powershell
scripts\qa\current_preflight.ps1 -Quick
git diff --check
```

完整检查：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\qa\current_preflight.ps1
```

安全检查：

```powershell
scripts\secrets_scan\secrets_scan.ps1
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle .codex_work app/libs/llm-sdk-release.aar
```

预期：

- preflight PASS。
- qwen `enable_thinking=false` guard PASS。
- secret scan PASS。
- forbidden tracked files 无输出。
- `.codex_work` 与 AAR 不入库。
