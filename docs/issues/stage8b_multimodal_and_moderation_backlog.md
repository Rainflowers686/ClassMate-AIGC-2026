# Stage 8B Multimodal Learning Pipeline + Local Moderation — Issue Backlog

> 30 个 GitHub Issue 草案，覆盖多模态图片学习管线和端侧文本审核。
> 当前不实现，仅记录规划。

---

### Issue 01: ImageRgbConverter Production Hardening

- **目标**：将 ARGB_8888 Bitmap 到 RGB byte[] 的转换逻辑做生产级加固
- **文件范围**：`core/src/.../image/ImageRgbConverter.kt`
- **验收标准**：支持多种 Bitmap 配置的自动转换；大图 (>4096px) 降采样；内存可控
- **非目标**：不做 GPU 加速
- **风险**：大图 OOM

---

### Issue 02: Multimodal Prompt Templates

- **目标**：设计并实现 5 类多模态 prompt 模板
- **文件范围**：`core/src/.../prompt/MultimodalPromptTemplates.kt`
- **验收标准**：课件/板书/题目/图表/公式 5 种模板；每种有 system + user prompt；支持参数化
- **非目标**：不做 prompt 自动选择和优化
- **风险**：端侧模型对中文 prompt 的响应质量不确定

---

### Issue 03: Image Understanding Diagnostic

- **目标**：在 Settings 诊断页完成 callVit + generate 端到端基础链
- **文件范围**：`app/src/.../settings/DiagnosticScreen.kt`
- **验收标准**：用户可选内置测试图片 → callVit → generate → 显示理解文本
- **非目标**：不做实际学习资料接入
- **风险**：callVit 在部分设备返回非 0

---

### Issue 04: Slide Image Source

- **目标**：实现 `SLIDE_IMAGE_UNDERSTANDING` 来源类型
- **文件范围**：`core/src/.../image/SlideImageSource.kt`
- **验收标准**：课件截图 → 理解文本；sourceLabel="课件截图·端侧理解"；携带元数据
- **非目标**：不做 PPT 原生格式解析
- **风险**：课件文字密度大，端侧 VIT 识别率低

---

### Issue 05: Blackboard Image Source

- **目标**：实现 `BLACKBOARD_IMAGE_UNDERSTANDING` 来源类型
- **文件范围**：`core/src/.../image/BlackboardImageSource.kt`
- **验收标准**：板书照片 → 理解文本；sourceLabel="板书照片·端侧理解"
- **非目标**：不做实时板书跟踪识别
- **风险**：手写板书识别远难于印刷体

---

### Issue 06: Exercise Image Source

- **目标**：实现 `EXERCISE_IMAGE_UNDERSTANDING` 来源类型
- **文件范围**：`core/src/.../image/ExerciseImageSource.kt`
- **验收标准**：题目截图 → 理解文本；sourceLabel="题目截图·端侧理解"
- **非目标**：不做题目自动解答
- **风险**：复杂公式/图表超出端侧 VIT 能力

---

### Issue 07: PDF Page Image Source

- **目标**：实现 `PDF_PAGE_IMAGE_UNDERSTANDING` 来源类型
- **文件范围**：`core/src/.../image/PdfPageImageSource.kt`
- **验收标准**：PDF 页渲染为 Bitmap → 理解文本；sourceLabel="PDF页面·端侧理解"
- **非目标**：不做 PDF 文本层解析（已有 OCR 路径）
- **风险**：PDF 渲染库兼容性

---

### Issue 08: Image Source Metadata

- **目标**：设计并实现图片理解来源的元数据结构
- **文件范围**：`core/src/.../image/ImageUnderstandingMetadata.kt`
- **验收标准**：包含 sourceLabel/originalImageHash/vitModelVersion/timestamp/confidenceLevel
- **非目标**：不做元数据搜索和索引
- **风险**：hash 计算可能耗时

---

### Issue 09: Image Understanding Redaction

- **目标**：对端侧图像理解文本进行敏感信息脱敏
- **文件范围**：`core/src/.../safety/ImageUnderstandingRedaction.kt`
- **验收标准**：移除理解文本中的 PII；移除可能的路径/URL；保留语义
- **非目标**：不做图片本身的脱敏
- **风险**：过于激进的脱敏可能破坏公式/代码内容

---

### Issue 10: Image Understanding Validator

- **目标**：验证端侧图像理解输出的质量和安全性
- **文件范围**：`core/src/.../validation/ImageUnderstandingValidator.kt`
- **验收标准**：检查输出非空、置信度阈值、不包含不当内容、长度合理
- **非目标**：不做图像理解准确率自动评估
- **风险**：置信度阈值设定依赖经验值

---

### Issue 11: MaterialBundle Image Source Integration

- **目标**：将 ImageUnderstandingSource 接入 MaterialBundle
- **文件范围**：`core/src/.../material/MaterialBundle.kt`
- **验收标准**：MaterialBundle 支持 ImageUnderstandingSource 作为新素材类型
- **非目标**：不改动现有文本素材的处理逻辑
- **风险**：MaterialBundle 接口可能需重大改动

---

### Issue 12: Timeline Image Evidence Chip

- **目标**：在 Timeline 中显示图像理解来源的证据标签
- **文件范围**：`app/src/.../ui/timeline/`, `core/src/.../timeline/`
- **验收标准**：图像理解条目在 Timeline 中以独特图标+标签显示；可点击查看详情
- **非目标**：不显示原始图片
- **风险**：UI 设计需与现有 Timeline 布局兼容

---

### Issue 13: Ask Image Citation

- **目标**：Ask 回答时引用图像理解来源
- **文件范围**：`core/src/.../ask/`, `app/src/.../ui/ask/`
- **验收标准**：Ask 回答中包含"[来源：端侧课件截图理解]"引用
- **非目标**：不做图像理解内容的实时 Ask
- **风险**：引用格式需与现有 Evidence 系统兼容

---

### Issue 14: Quiz from Image Understanding

- **目标**：基于图像理解结果生成练习题目
- **文件范围**：`core/src/.../practice/`
- **验收标准**：从 ImageUnderstandingSource 提取知识点生成选择题/填空题
- **非目标**：不做图像理解结果的自动难度分级
- **风险**：理解文本质量不足以支撑出题

---

### Issue 15: StudyReport Image Source Section

- **目标**：在 StudyReport 中增加"图像理解来源"章节
- **文件范围**：`core/src/.../report/`
- **验收标准**：报告列出所有图像理解来源及摘要；标注"端侧图像理解·实验性"
- **非目标**：不嵌入原始图片
- **风险**：报告体量增大

---

### Issue 16: Image Privacy UI Copy

- **目标**：编写图片理解功能的隐私提示文案
- **文件范围**：`app/src/.../ui/i18n/Strings.kt`（或独立隐私文案文件）
- **验收标准**：每个图片操作步骤有对应隐私提示："端侧处理·不上传"
- **非目标**：不做隐私政策的完整法律文本
- **风险**：文案过于冗长影响体验

---

### Issue 17: No Original Image Export Guard

- **目标**：确保导出的报告中不包含原始图片
- **文件范围**：`core/src/.../export/`
- **验收标准**：Export pipeline 白名单检查，禁止图片二进制数据出现在导出中
- **非目标**：不做图片加密
- **风险**：whitelist 可能遗漏新的导出路径

---

### Issue 18: Local Text Moderation SDK Seam

- **目标**：定义端侧文本审核 SDK 的抽象接口
- **文件范围**：`core/src/.../moderation/LocalModerationSeam.kt`
- **验收标准**：接口不依赖 CmsLocalFrame 的具体类；支持 mock 实现用于测试
- **非目标**：不做真实 SDK 集成
- **风险**：接口设计需兼容未来 SDK 版本变更

---

### Issue 19: Export Moderation Check

- **目标**：在导出前接入端侧文本审核
- **文件范围**：`core/src/.../export/`, `core/src/.../moderation/`
- **验收标准**：导出流程中审核所有 AI 生成文本；result=2 自动移除
- **非目标**：不做用户手动输入内容的审核
- **风险**：审核可能误判正常学术内容

---

### Issue 20: Share Moderation Check

- **目标**：在分享前接入端侧文本审核
- **文件范围**：`app/src/.../share/`, `core/src/.../moderation/`
- **验收标准**：分享流程中审核分享内容；result=2 阻止分享
- **非目标**：不做分享图片的审核
- **风险**：审核延迟影响分享体验

---

### Issue 21: AI Output Moderation

- **目标**：对 AI 生成的报告润色和练习解析进行审核
- **文件范围**：`core/src/.../moderation/`
- **验收标准**：AI 输出经过审核 pipeline 后才展示给用户
- **非目标**：不做流式输出的实时审核
- **风险**：审核增加端到端延迟

---

### Issue 22: Moderation Result UI

- **目标**：实现审核结果（0/1/2）的用户界面
- **文件范围**：`app/src/.../ui/moderation/`
- **验收标准**：result=1 高亮嫌疑文本；result=2 显示替换说明；审核失败有 fallback 提示
- **非目标**：不做复杂的审核申诉流程
- **风险**：UI 提示可能引起用户困惑

---

### Issue 23: Moderation Failure Fallback

- **目标**：实现审核 SDK 异常时的 fallback 逻辑
- **文件范围**：`core/src/.../moderation/`
- **验收标准**：init 失败/超时/异常时不阻塞用户流程；标注"未经审核"
- **非目标**：不做审核失败的自动重试
- **风险**：fallback 时存在未经审核内容展示的风险

---

### Issue 24: Multimodal Proof Screenshots

- **目标**：拍摄多模态功能的 proof 截图
- **文件范围**：截图文件，不涉及代码
- **验收标准**：14 张截图覆盖图片导入、理解、编辑、入库、Ask、Quiz、Report 全流程
- **非目标**：不做录屏
- **风险**：部分流程依赖真机环境

---

### Issue 25: Real Device Multimodal Performance

- **目标**：在云真机上记录端侧多模态的性能基准
- **文件范围**：`docs/testing/`
- **验收标准**：记录 callVit 耗时、generate 首个 token 耗时、不同图片尺寸的影响
- **非目标**：不做性能优化
- **风险**：云真机性能与用户设备差异大

---

### Issue 26: callVit Timeout Handling

- **目标**：为 callVit 增加超时机制
- **文件范围**：`core/src/.../ondevice/`
- **验收标准**：callVit 超时（如 10 秒）后自动降级；不阻塞 UI
- **非目标**：不做 callVit 性能优化
- **风险**：超时阈值需根据设备性能调整

---

### Issue 27: Memory/OOM Guard

- **目标**：防止大图片导致 OOM
- **文件范围**：`core/src/.../image/`
- **验收标准**：图片超过阈值（如 2048×2048）自动降采样；callVit 前检查可用内存
- **非目标**：不做内存泄漏修复
- **风险**：降采样可能影响 VIT 识别率

---

### Issue 28: Model Path Profile

- **目标**：管理不同模型文件的路径配置
- **文件范围**：`core/src/.../model/`
- **验收标准**：支持多模型路径配置；支持模型版本切换
- **非目标**：不做模型文件的自动下载
- **风险**：路径配置错误导致模型加载失败

---

### Issue 29: No Storage Permission Proof

- **目标**：证明端侧多模态不需要额处存储权限
- **文件范围**：`docs/competition/`（截图 + 说明）
- **验收标准**：截图证明 Manifest 无 MANAGE_EXTERNAL_STORAGE；端侧理解功能在正常权限下工作
- **非目标**：不做权限动态申请优化
- **风险**：部分 Android 版本存储权限行为差异

---

### Issue 30: Stage 8B Final Red Team

- **目标**：对 Stage 8B 所有新增代码进行红队审查
- **文件范围**：Stage 8B 涉及的所有生产代码
- **验收标准**：审查图片不泄露、审核不绕过、source label 不缺失、validators 不削弱
- **非目标**：不做大规模重构
- **风险**：红队审查可能发现需要较大改动的问题
