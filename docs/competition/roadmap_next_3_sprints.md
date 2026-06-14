# ClassMate 下一阶段 3 个 Sprint 路线图

目标：先修稳定性和中文化，再补 ASR/OCR/PPT 识别地基，最后增强学习闭环与 proof。

## Sprint 1：修 bug + 中文化 + Live 闭环 + UI 对齐

### 目标

把 Stage 3 生产改动稳定下来，让真机可演示、可截图、可录视频。

### 功能

- 修复中文 i18n 和乱码。
- 修复 Live -> Timeline 闭环。
- 修复 UI 溢出。
- 落地 Design HTML 中的 Focus / Flow / Vitality 关键视觉。
- 确认 qwen3.5-plus 仍关闭 thinking。
- 保持 ProviderResolver 和 validators 不变。

### 不做什么

- 不接真实 ASR。
- 不接真实 OCR。
- 不做网络视频抓取。
- 不做云同步。

### 验收标准

- App 能安装并完整跑通 Import / Live / Timeline / Quiz / Review / History / Export。
- Live 页面明确手动/模拟转写。
- 不申请录音权限。
- 真机无明显文字溢出。
- Secrets scan 通过。
- Android CI 通过。

### 适合 Claude 做什么

- 生产 UI 修复。
- i18n 和布局修复。
- Compose 组件落地。
- 真机 bug 修复。

### 适合 Codex 做什么

- Smoke checklist。
- proof 文档。
- Q&A。
- 验收矩阵。
- sample lessons。

### 风险

- UI 改动影响多个页面。
- Live 视觉增强可能被误解为真实 ASR。
- 并发改动时容易混入 provider 或 validator 风险。

## Sprint 2：ASR/OCR/PPT 识别地基

### 目标

建立多材料融合地基，为追平听脑类产品的基础输入能力做准备。

### 功能

- `TranscriptionProvider` 抽象。
- `OcrProvider` 抽象。
- `LessonMaterialBundle`。
- `MaterialEvidenceRef`。
- `TermGlossary`。
- 手动 OCR fallback。
- Live manual 抽象为 `ManualTranscriptProvider`。
- 句段级字音同步。

### 不做什么

- 不直接做第三方平台视频解析。
- 不伪造词级时间戳。
- 不声称声纹识别已完成。
- 不做云同步。

### 验收标准

- 手动 transcript、粘贴文本、txt/md、手动 OCR 结果都能进入同一个材料 bundle。
- 每段融合文本保留来源。
- 证据能回到材料来源。
- 句段级时间范围可表示。
- 没有词级时间戳时不生成词级同步。

### 适合 Claude 做什么

- 数据模型和生产接线。
- Import / Live 小范围 UI 接入。
- Material evidence UI 最小展示。

### 适合 Codex 做什么

- ASR/OCR 官方文档调研。
- 架构文档。
- 测试课堂材料。
- 验收用例和 proof 清单。

### 风险

- 官方接口字段未确认。
- ASR/OCR 可能涉及权限和隐私。
- OCR 证据定位难度高。
- 术语纠错可能误改证据。

## Sprint 3：学习闭环增强

### 目标

在追平基础输入能力后，继续强化 ClassMate 的差异化：证据链、问答、复习和导出。

### 功能

- Ask This Lesson 接 provider。
- 资料融合证据链。
- ReviewTask 更智能的优先级和复习解释。
- Weakness Hub 与视频白名单搜索更自然。
- Export 报告包含材料来源、术语表摘要、复习进度和 proof-safe 说明。
- 真机演示视频。
- 复赛/决赛 proof 包。

### 不做什么

- 不做未授权视频平台抓取。
- 不把 Compatible demo 当成官方主路径。
- 不把 LocalFallback 包装成模型分析。
- 不导出敏感模型请求上下文或内部思考字段。

### 验收标准

- Ask This Lesson 回答必须标注 grounded / partial / not found。
- evidence quote 不可定位时不伪造证据。
- ReviewTask 解释清楚“为什么复习这个”。
- Export 文件敏感词检查通过。
- 演示视频不出现密钥、隐私路径或账号信息。

### 适合 Claude 做什么

- Ask provider 接线。
- 资料融合证据 UI。
- Review / Weakness Hub 产品化。
- Export 生产实现增强。

### 适合 Codex 做什么

- Prompt/验收策略文档。
- GitHub Issues 批量规划。
- 演示脚本和 Q&A。
- Proof 截图 checklist。

### 风险

- Ask 接 provider 后可能产生无证据回答。
- 多材料证据链复杂度增加。
- 导出内容更丰富后安全检查压力增加。
- 真机演示视频需要严格避开密钥和隐私。

## 总结

Sprint 1 保稳定和可展示；Sprint 2 补输入地基；Sprint 3 强化学习闭环。ClassMate 追平听脑类产品的基础输入能力后，应继续把壁垒放在证据链、微测、自适应复习、LearningStore 和 Course Library 上。
