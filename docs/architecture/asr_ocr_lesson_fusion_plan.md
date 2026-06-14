# ASR / OCR / PPT / 板书 / 手动笔记融合方案

目标：把多种课堂材料融合为 `LessonMaterialBundle`，再进入现有 ClassMate 学习闭环。本文是下一阶段实施方案，不表示当前已经接入真实 ASR、OCR、音视频解析或字音同步。

## 1. 总目标

ClassMate 下一阶段不是只追求“录音转写”，而是把课堂里的声音、课件、板书、讲义、手动笔记和粘贴文本融合成可验证、可测验、可复习的学习材料。

最终目标：

- 多入口采集课堂材料。
- 融合材料时保留来源、时间、页码、片段和可信度。
- 输出仍然进入现有知识时间线、证据链、微测、ReviewTask、Weakness Hub、Course Library 和 Export。
- 对用户诚实：没有词级时间戳就不伪造词级同步；没有声纹能力就不声称已识别多人身份。

## 2. 输入类型

下一阶段支持或预留以下输入：

- 实时录音 ASR：课堂中实时或准实时生成 transcript segment。
- 本地音频上传：用户上传本地录音文件，转成 transcript session。
- 本地视频上传：只处理用户本地授权视频，不做平台抓取；优先抽取音轨和用户提供字幕。
- PPT / 课件图片：识别幻灯片标题、正文、公式和图示说明。
- 板书照片：识别手写或拍照板书中的关键文字、公式和结构。
- PDF / 讲义截图：识别讲义页或截图中的段落和标题。
- 手动笔记：用户手动输入课堂补充内容。
- 粘贴文本：沿用当前 Import Hub 主能力。
- `.txt` / `.md`：沿用当前文件导入能力。

## 3. 明确不做

当前阶段和下一阶段早期明确不做：

- 第三方平台网络视频爬取或解析。
- 未授权内容抓取。
- 绕过平台规则下载字幕、音频或视频。
- 云同步和团队协作，除非后续有服务器、账号、存储、隐私和合规方案。
- 把未完成能力包装成已完成能力。

## 4. 核心数据模型设计

### LessonMaterialBundle

一节课的融合输入总包。

字段建议：

- `id`
- `courseTitle`
- `subject`
- `createdAt`
- `materials: List<MaterialSource>`
- `transcriptSessions: List<TranscriptSession>`
- `ocrDocuments: List<OcrDocument>`
- `manualNotes: List<ManualNote>`
- `glossary: TermGlossary`
- `fusionWarnings: List<String>`

作用：作为 `CourseAnalyzer` 之前的材料融合层，把多个来源整理成可分析文本和证据索引。

### TranscriptSession

一次转写会话。

- `id`
- `sourceType`
- `startedAt`
- `endedAt`
- `segments: List<TranscriptSegment>`
- `provider`
- `language`
- `confidence`

### TranscriptSegment

一段转写文本。

- `id`
- `text`
- `timeRange: AudioTimeRange?`
- `speaker: SpeakerLabel`
- `source`
- `confidence`
- `syncTokens: List<SyncToken>`

### AudioTimeRange

音频或视频中的时间范围。

- `startMs`
- `endMs`
- `sourceMediaId`

### SyncToken

字音同步单元。

- `token`
- `startMs`
- `endMs`
- `confidence`
- `sourceSegmentId`

说明：只有 ASR 服务提供真实词级或字级时间戳时才生成；没有时不伪造。

### SpeakerLabel

说话人标签。

- P1：`teacher` / `student` / `unknown`
- P2：`speaker_1` / `speaker_2` 等多人 id
- `confidence`

说明：不能把简单规则误称为声纹识别。

### OcrDocument

一个 OCR 输入材料，例如一组 PPT 图片、板书照片或讲义截图。

- `id`
- `sourceType`
- `title`
- `pages: List<OcrPage>`
- `provider`
- `createdAt`

### OcrPage

OCR 文档中的一页。

- `id`
- `pageIndex`
- `imageRef`
- `blocks: List<OcrBlock>`
- `slideFrame: SlideFrame?`

### OcrBlock

OCR 文本块。

- `id`
- `text`
- `boundingBox`
- `role`
- `confidence`

`role` 可为 title、body、formula、caption、table、unknown。

### SlideFrame

幻灯片结构化帧。

- `slideNumber`
- `title`
- `bullets`
- `figureCaptions`
- `speakerNote`

### MaterialEvidenceRef

融合证据引用。

- `sourceType: MaterialSourceType`
- `sourceId`
- `segmentId`
- `pageId`
- `blockId`
- `timeRange`
- `quote`
- `confidence`

作用：让知识点证据不仅能回到文本段，也能回到录音时间、PPT 页、板书块或笔记片段。

### MaterialSourceType

建议枚举：

- `PASTED_TEXT`
- `TEXT_FILE`
- `MARKDOWN_FILE`
- `LIVE_MANUAL_TRANSCRIPT`
- `ASR_REALTIME`
- `ASR_AUDIO_FILE`
- `VIDEO_LOCAL`
- `PPT_IMAGE`
- `BOARD_PHOTO`
- `PDF_SCREENSHOT`
- `MANUAL_NOTE`

### CourseTerm

课程术语。

- `term`
- `aliases`
- `subject`
- `definition`
- `examples`
- `priority`
- `source`

### TermGlossary

一节课或一个科目的术语表。

- `subject`
- `terms: List<CourseTerm>`
- `version`
- `selectedByUser`
- `generatedFromCourse`

## 5. Provider 设计

### TranscriptionProvider

转写能力抽象。

- 输入：音频流、本地音频、本地视频音轨或手动片段。
- 输出：`TranscriptSession`。
- 要求：不在日志中保存音频内容或敏感路径。

### VivoAsrProvider

vivo ASR 接入实现，下一阶段调研和接入。

- 支持长语音转写或实时短语音识别。
- 输出句段级时间戳；若服务提供词级时间戳，再生成 `SyncToken`。
- 不在未接入前展示为已完成。

### ManualTranscriptProvider

当前 Live manual 的抽象化版本。

- 用户手动输入片段。
- source 标为 manual 或 simulated。
- 适合作为 ASR 接入前的稳定测试入口。

### OcrProvider

OCR 能力抽象。

- 输入：图片、PPT 截图、板书照片、PDF 截图。
- 输出：`OcrDocument`。

### VivoOcrProvider

vivo OCR 接入实现，下一阶段调研和接入。

- 输出文本块和置信度。
- 可结合课程术语表做纠错提示。

### ManualOcrFallback

OCR 未可用时的兜底。

- 用户手动粘贴图片识别结果。
- 仍标注为 manual fallback。

### LessonFusionEngine

融合引擎。

- 输入：`LessonMaterialBundle`。
- 输出：可进入 `CourseAnalyzer` 的融合文本、证据索引和材料引用。
- 不负责模型生成，不绕开现有分析主链路。

## 6. 分析流程

1. 用户导入材料：ASR、OCR、手动笔记、粘贴文本、txt/md。
2. 各 provider 生成结构化中间结果。
3. 清洗资料：去重、去口头套话、修正常见 OCR 错字、保留来源。
4. 术语表增强：注入科目术语、别名、公式写法和易混词。
5. 融合排序：按课堂时间、PPT 页序、板书上下文和手动笔记时间合并。
6. 证据来源标记：每段文本都保留 `MaterialEvidenceRef`。
7. 生成面向 `CourseAnalyzer` 的课堂文本和材料索引。
8. 调用现有分析链路，生成 Knowledge Timeline / Quiz / ReviewTask。
9. UI 展示时，证据可以回到原文段、音频时间、PPT 页、OCR 块或笔记来源。

## 7. 字音同步策略

### P1：句段级同步

- transcript segment 绑定 `AudioTimeRange`。
- UI 可以在证据卡显示“来自 03:20-03:45”。
- 适合先上线，复杂度低。

### P2：词级同步

- 只有 ASR 返回真实词级或字级时间戳时生成 `SyncToken`。
- 可用于点击文字定位音频片段。
- 没有词级时间戳时不可伪造，不用平均分配时间冒充真实同步。

## 8. speaker 策略

### P1：teacher / student / unknown

- 通过手动选择、简单规则或 ASR 返回标签标注。
- 默认 unknown。
- 不声称完成声纹识别。

### P2：多人 speaker id

- 若 ASR 服务支持 diarization，再引入 `speaker_1`、`speaker_2`。
- UI 只显示置信度足够的标签。
- 低置信度保持 unknown。

## 9. 安全策略

- 用户本地材料只在用户明确操作后进入分析。
- 不爬取第三方平台。
- 不处理未授权内容。
- 不记录真实密钥。
- 不导出模型请求上下文、完整厂商原始返回或模型内部思考字段。
- 不在 History / LearningStore 中保存认证信息。
- proof 截图不展示本地隐私路径、账号、密钥输入框。

## 10. 下一步建议

1. 先实现 `LessonMaterialBundle` 和 `MaterialEvidenceRef` 的纯数据模型。
2. 保持当前 Live manual 为 `ManualTranscriptProvider`。
3. 添加手动 OCR fallback，让图片识别结果可先粘贴进入 bundle。
4. 调研 vivo ASR/OCR 接入细节。
5. 做句段级同步 proof，再考虑词级同步。
