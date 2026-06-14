# Claude Stage 4 主任务提示词：ASR/OCR/PPT/板书/手动笔记地基

你现在在 Windows 本地项目：

`D:\Edge Download\AIGC\ClassMate`

目标：执行 Stage 4，建设 ASR/OCR/PPT/板书/手动笔记融合地基，让 ClassMate 从“课堂文本分析”扩展为“多资料融合后的可验证学习闭环”。本轮优先做架构、数据模型、本地/手动模拟 provider、UI 接入缝和测试，不要求一次接通真实 vivo ASR/OCR API。

## 必须先保留的现状

- 保留 Stage 3 的 P0 修复结果：中文化、Live -> Timeline 闭环、UI 溢出修复、Design HTML 落地。
- 保留官方 BlueLM 主链路，不改协议，不改 ProviderResolver 顺序。
- 保留 qwen3.5-plus 的关闭深度思考配置。
- 保留 Compatible demo 与 LocalFallback。
- 不削弱 EvidenceValidator / ResultValidator / EvidenceResolver。
- 不读取本地真实配置内容，只能检查文件是否存在。
- 不写入任何真实 AppID / AppKEY / API key。
- 不记录模型请求全文、课堂正文、供应商原始返回、认证头、内部思考文本。
- 不做第三方平台视频爬取。
- 如果只是地基，不申请录音权限；只有明确实现真实 ASR 且有权限说明时才可讨论权限。

## Stage 4 总目标

把课堂材料统一成 `LessonMaterialBundle`，让后续的 ASR、OCR、PPT 图片、板书照片、手动笔记、粘贴文本和本地 txt/md 都能进入同一条证据链分析流程。

核心不是“多入口堆叠”，而是：

1. 输入材料可标记来源；
2. 证据可追溯到材料片段；
3. CourseAnalyzer 仍输出 Knowledge Timeline / Quiz / ReviewTask；
4. 学习结果仍通过现有校验器；
5. 后续真机接入 vivo ASR/OCR 时不需要推翻架构。

## P0：数据模型 + Provider Abstraction + Lesson Fusion

优先实现：

- `LessonMaterialBundle`
- `MaterialEvidenceRef`
- `MaterialSourceType`
- `TranscriptSession`
- `TranscriptSegment`
- `AudioTimeRange`
- `SyncToken`
- `SpeakerLabel`
- `OcrDocument`
- `OcrPage`
- `OcrBlock`
- `SlideFrame`
- `CourseTerm`
- `TermGlossary`

Provider 抽象：

- `TranscriptionProvider`
- `ManualTranscriptProvider`
- `OcrProvider`
- `ManualOcrFallback`
- `LessonFusionEngine`

验收：

- 手动课堂片段、粘贴文本、txt/md 导入能构造 `LessonMaterialBundle`。
- `LessonFusionEngine` 能把 bundle 转成 CourseAnalyzer 可消费的课堂文本，同时保留 evidence source mapping。
- 生成结果仍进入现有 Timeline / Quiz / Review / History / Export。
- 不改变官方 BlueLM 主链路。

## P1：OCR/PPT/板书入口和模拟 Provider

实现诚实入口：

- PPT/课件图片导入入口；
- 板书照片导入入口；
- OCR 结果手动粘贴或模拟结果入口；
- 不接真实网络 OCR 时明确显示“当前为模拟/手动导入，后续接入官方 OCR”。

Provider：

- `VivoOcrProvider` 可先只留接口和配置结构，不实现真实调用。
- `ManualOcrFallback` 支持用户粘贴 OCR 文本并标记为 `slide_ocr` 或 `blackboard_ocr`。

验收：

- 不申请不必要权限。
- 不上传图片到未知服务。
- OCR 占位不会误触发网络请求。
- Export 能展示 OCR 来源证据摘要。

## P2：ASR Session / 句段级同步地基

实现：

- Transcript session 状态机：idle / active / paused / ended。
- Segment source：manual / simulated / asr_future。
- 句段级时间范围：startMs / endMs。
- Speaker 第一版：teacher / student / unknown。
- SyncToken 第一版只做句段级，不伪造词级时间戳。

验收：

- Live Companion 追加片段后能生成 bundle。
- 结束课堂后可生成 Timeline，并进入 History / LearningStore。
- 页面必须标注“手动/模拟转写，暂未接入真实 ASR”。
- 不申请录音权限。

## P3：Ask Lesson Provider Grounded QA

实现问答地基：

- `LessonQuestion`
- `LessonAnswer`
- `LessonAnswerEvidenceRef`
- `AskLessonPromptBuilder`
- `AskLessonResultParser`
- `AskLessonEngine`

要求：

- 输入只包含课程标题、知识点摘要和短证据片段。
- 输出必须是结构化 JSON。
- evidence quote 能定位时标记 grounded；不能定位时降级 partial 或 not_found。
- provider 失败时可用本地兜底，但必须标注 fallback。
- 不记录完整模型输入、课堂全文、供应商原始返回或内部思考文本。

## 必跑验证

完成后运行：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
bash scripts/secrets_scan/secrets_scan.sh
git diff --check
```

还要检查：

- `git ls-files` 不追踪本地配置、密钥文件、keystore、apk/aab/build 输出。
- Ask Lesson、Export、History、LearningStore 不保存敏感字段或供应商原始内容。

## 完成报告

报告必须包含：

- 修改文件；
- P0/P1/P2/P3 完成情况；
- 是否改动 BlueLM 主链路；
- 是否削弱 validator；
- 是否申请录音权限；
- 是否实现真实 ASR/OCR；
- 测试结果；
- secrets scan 结果；
- 是否建议 commit；
- 建议 commit message。
