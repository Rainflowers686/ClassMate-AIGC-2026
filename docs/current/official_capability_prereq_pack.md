# ClassMate 官方能力前置准备包

本文是后续 Claude 逐工单真实落地 vivo 官方能力的前置准备包。它基于本地已抓取的 vivo AIGC 官方接口文档和当前 ClassMate 工程状态，只做工程摘要、优先级、风险与任务拆分，不复制官方文档全文，不包含真实凭据，不实现功能。

当前工程定位：

- ClassMate 主链：资料输入 -> 蓝心理解 -> 证据校验 -> 知识地图 -> Ask / Practice / Review / Export。
- AI 路径：云端蓝心 -> 端侧蓝心 -> 安全占位。
- 已有基础：云端大模型链路、端侧 BlueLM 3B 文本/多模态基础、图片/拍照草稿、validators-gated persistence、Stage 10 product UI baseline。
- 本准备包目标：把官方能力拆成可验证、可逐个落地的 Claude 工程任务。

## 2026-06-18 Current Provider Smoke Status

本文件保留前期任务拆分和能力准备信息；以下为当前事实状态：

| Provider | Config status | Live smoke status | Notes |
|---|---|---|---|
| OCR | `READY` | `PASS` | 官方 product-facing OCR provider smoke 已真实通过。 |
| TEXT_SIMILARITY | `READY` | `PASS` | 官方 rerank / retrieval enhancement smoke 已真实通过。 |
| EMBEDDING | `READY` | `PASS` | 官方 vector retrieval foundation smoke 已真实通过。 |
| QUERY_REWRITE | `READY` | `BLOCKED` | live smoke/runtime path blocked；不是官方 provider 不可用证据，也不是 L3 blocker。 |
| TRANSLATION | seam-only | not run | 后置。 |
| TTS | seam-only | not run | 后置。 |
| FUNCTION_CALLING | seam-only | not run | 后置。 |
| ASR_LONG | deferred | not run | 单独用非敏感音频验证。 |

下一主线：App-level L3 云真机/真机学习闭环验证。Query Rewrite 可由 Claude/provider diagnostics 专项继续尝试，但不阻塞 L3 主线；产品 fallback 为 qwen3.5-plus rewrite、local safe rewrite 或 direct retrieval。

## Official Docs Coverage Check

| Item | Result |
|---|---|
| manifest 总页数 | 22 |
| sidebar_links 目录项数 | 22 |
| pages 实际数量 | 22 |
| 成功抓取页数 | 22 |
| 失败页数 | 0 |
| 是否包含使用指引 | Yes，docId `1746` |
| 是否包含鉴权方式 | Yes，docId `1677` |
| 是否包含官方 20 个接口能力项 | Yes |
| 缺失项列表 | None |
| 重复项列表 | None |
| 当前工程准备包纳入能力项数量 | 18 |
| 当前工程准备包排除能力项 | 声音复刻、LBS / 地理编码 / POI 搜索 |
| 结论 | 文档抓取完整；当前工程准备包主动排除 2 个与主线不匹配或合规风险高的能力。 |

### Official Capability Inventory

| # | Official capability | docId | Official title | Local captured path | Pack status |
|---:|---|---:|---|---|---|
| 1 | 大模型 | 1745 | 大模型 | `.codex_work/official_docs/vivo_aigc_docs/pages/011-1745-大模型/` | Included |
| 2 | Function calling | 1805 | Function calling | `.codex_work/official_docs/vivo_aigc_docs/pages/016-1805-Function-calling/` | Included |
| 3 | 图片生成 | 1732 | 图片生成 | `.codex_work/official_docs/vivo_aigc_docs/pages/002-1732-图片生成/` | Included |
| 4 | 视频生成 | 2201 | 视频生成 | `.codex_work/official_docs/vivo_aigc_docs/pages/022-2201-视频生成/` | Included |
| 5 | 通用 OCR | 1737 | 通用 OCR | `.codex_work/official_docs/vivo_aigc_docs/pages/007-1737-通用OCR/` | Included |
| 6 | 文本翻译 | 1733 | 文本翻译 | `.codex_work/official_docs/vivo_aigc_docs/pages/003-1733-文本翻译/` | Included |
| 7 | 文本向量 | 1734 | 文本向量 | `.codex_work/official_docs/vivo_aigc_docs/pages/004-1734-文本向量/` | Included |
| 8 | 文本相似度 | 2060 | 文本相似度 | `.codex_work/official_docs/vivo_aigc_docs/pages/017-2060-文本相似度/` | Included |
| 9 | 查询改写 | 2061 | 查询改写 | `.codex_work/official_docs/vivo_aigc_docs/pages/018-2061-查询改写/` | Included |
| 10 | 实时短语音识别 | 1738 | 实时短语音识别 | `.codex_work/official_docs/vivo_aigc_docs/pages/008-1738-实时短语音识别/` | Included |
| 11 | 长语音听写 | 1740 | 长语音听写 | `.codex_work/official_docs/vivo_aigc_docs/pages/010-1740-长语音听写/` | Included |
| 12 | 长语音转写 | 1739 | 长语音转写 | `.codex_work/official_docs/vivo_aigc_docs/pages/009-1739-长语音转写/` | Included |
| 13 | 方言自由说 | 2065 | 方言自由说 | `.codex_work/official_docs/vivo_aigc_docs/pages/020-2065-方言自由说/` | Included |
| 14 | 同声传译 | 2068 | 同声音传译 | `.codex_work/official_docs/vivo_aigc_docs/pages/021-2068-同声音传译/` | Included |
| 15 | 音频生成 | 1735 | 音频生成 | `.codex_work/official_docs/vivo_aigc_docs/pages/005-1735-音频生成/` | Included |
| 16 | 声音复刻 | 2062 | 声音复刻 | `.codex_work/official_docs/vivo_aigc_docs/pages/019-2062-声音复刻/` | Excluded |
| 17 | 地理编码 / POI 搜索 | 1736 | 地理编码(POI搜索) | `.codex_work/official_docs/vivo_aigc_docs/pages/006-1736-地理编码(POI搜索)/` | Excluded |
| 18 | 端侧 3B 大模型 | 1802 | 端侧 3B 大模型 | `.codex_work/official_docs/vivo_aigc_docs/pages/013-1802-端侧3B大模型/` | Included |
| 19 | 端侧文本审核 | 1804 | 端侧文本审核 | `.codex_work/official_docs/vivo_aigc_docs/pages/015-1804-端侧文本审核/` | Included |
| 20 | 端侧能力相关文件 | 1803 | 端侧能力相关文件 | `.codex_work/official_docs/vivo_aigc_docs/pages/014-1803-端侧能力相关文件/` | Included |

Non-capability reference docs:

- 鉴权方式：docId `1677`，本地路径 `.codex_work/official_docs/vivo_aigc_docs/pages/001-1677-鉴权方式/`。
- 使用指引：docId `1746`，本地路径 `.codex_work/official_docs/vivo_aigc_docs/pages/012-1746-使用指引/`。

## Capability Cards

### 1. 大模型 / 文本生成

- Capability ID: `official-large-model-1745`
- Official category: 文本生成
- Official title: 大模型
- docId: `1745`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/011-1745-大模型/`
- Current ClassMate status: 云端大模型链路已经有初步实现；后续不是从零接入，而是统一进入 AI Capability Router。
- Priority: P0
- Decision: Already partially implemented; route and harden.
- Why it matters for ClassMate: 支撑 CourseAnalysis / Ask / Practice / Review / Export 的主分析能力。
- Why it matters for beating 听脑 or competition story: ClassMate 不只做总结，而是用大模型生成可校验学习草稿，再由本地证据与 validators 决定能否入库。
- Official API style: HTTP / OpenAI-compatible chat completions style。
- Auth notes: Bearer AppKey / official auth reference；真实凭据只能走安全配置注入。
- Input type: 课堂资料、证据摘要、用户问题、练习上下文、导出摘要。
- Output type: JSON 学习草稿、grounded answer、练习解释、报告片段。
- Key params summary: model、messages、temperature、top_p、max_tokens、max_completion_tokens、reasoning_effort、request id；qwen3.5-plus 按 quality profile 在兼容支持时启用 `enable_thinking=true`。
- Error handling summary: 映射 HTTP / vendor code / parse / validation；失败后进入端侧 fallback 或安全占位。
- Existing code likely involved: `core/provider`、`CourseAnalyzer`、`AppViewModel`、Ask / Practice / Export flows。
- New provider/interface needed: `AiCapabilityRouter`、cloud source marker、retry / timeout profile consolidation。
- Cloud path: Cloud large model first。
- On-device fallback path: 端侧 3B CourseAnalysis / Ask fallback。
- Manual fallback path: 用户手动编辑资料与安全占位说明。
- User confirmation requirement: AI 输出入库前必须通过 parse + validators；图片/拍照草稿需要用户确认。
- UI surface later involved: Settings provider status、Course Detail、Ask、Practice、Export proof。
- Tests needed: cloud success、cloud fail -> on-device、invalid JSON not persisted、qwen guard、redaction、source label。
- Acceptance criteria: 云端成功结果标记 `CLOUD`；失败时不污染知识库；fallback 来源清楚；日志不含 prompt/messages/vendor body/reasoning_content。
- Risk: 超时、非 JSON、模型幻觉、错误兜底文案、source attribution 混乱。
- Rollback plan: 回退到当前云端路径与安全占位，不移除 validators gate。
- Suggested Claude engineering task title: Cloud Large Model Routing v1
- Suggested commit message: `feat(ai): route cloud large model through capability router`

### 2. Function calling

- Capability ID: `official-function-calling-1805`
- Official category: 文本生成
- Official title: Function calling
- docId: `1805`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/016-1805-Function-calling/`
- Current ClassMate status: 未作为主链能力接入；应先评估工具边界。
- Priority: P1
- Decision: Build later.
- Why it matters for ClassMate: 可把 searchEvidence / createQuiz / updateMastery / exportReport 变成显式工具调用。
- Why it matters for beating 听脑 or competition story: 从“回答问题”升级为“受控执行学习动作”。
- Official API style: Chat messages + tool schema pattern。
- Auth notes: Bearer AppKey / official auth reference；复用大模型配置。
- Input type: 用户意图、课堂上下文摘要、工具 schema。
- Output type: 工具调用计划或结构化动作。
- Key params summary: messages、tool definition、assistant tool-call format。
- Error handling summary: 工具名不在白名单、参数无法验证、证据不足时拒绝执行。
- Existing code likely involved: Ask engine、Practice engine、Export Center、LearningStore boundaries。
- New provider/interface needed: `FunctionCallingRouter`、`ToolPermissionPolicy`、`ToolCallValidator`。
- Cloud path: Cloud large model produces tool proposal。
- On-device fallback path: 端侧模型只做建议，不直接执行工具。
- Manual fallback path: UI 提供用户确认按钮。
- User confirmation requirement: 更新学习状态、导出、创建练习等动作必须用户确认。
- UI surface later involved: Ask result actions、Practice actions、Export actions。
- Tests needed: unknown tool rejected、missing evidence rejected、tool args validation、no direct state mutation before confirmation。
- Acceptance criteria: 工具调用不会绕过 EvidenceResolver / validators / user confirmation。
- Risk: 模型越权、参数注入、错误工具执行。
- Rollback plan: 关闭 Function calling，保留普通 Ask / Practice。
- Suggested Claude engineering task title: Function Calling Router v1
- Suggested commit message: `feat(ai): add guarded function calling router`

### 3. 图片生成

- Capability ID: `official-image-generation-1732`
- Official category: 图片生成
- Official title: 图片生成
- docId: `1732`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/002-1732-图片生成/`
- Current ClassMate status: 未接入；不是近期主线。
- Priority: P2
- Decision: Defer.
- Why it matters for ClassMate: 可用于复习海报、知识点图示、答辩展示素材。
- Why it matters for beating 听脑 or competition story: 有助于学习资料表达，但不构成核心学习闭环。
- Official API style: HTTP / task style image generation。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 知识点摘要、复习卡描述、可视化提示。
- Output type: 图片 URL / base64 / task result，具体以官方文档为准。
- Key params summary: model、prompt、image、generation parameters。
- Error handling summary: 参数错误、内容审核、任务失败；不得自动混入学习证据。
- Existing code likely involved: Export Center、StudyReport、proof assets。
- New provider/interface needed: `VivoImageGenerationProvider`。
- Cloud path: Official image generation service。
- On-device fallback path: 无同等端侧生成能力；可使用本地静态模板。
- Manual fallback path: 用户上传或选择授权图片。
- User confirmation requirement: 生成图仅作为展示增强，必须用户确认后用于导出。
- UI surface later involved: StudyReport cover、复习卡海报、presentation proof。
- Tests needed: no evidence contamination、safety redaction、task failure UI。
- Acceptance criteria: 不把生成图片当作课堂证据，不进入知识校验链。
- Risk: 版权、审核、成本、偏离主线。
- Rollback plan: 关闭生成入口，保留纯文本/HTML/PDF 报告。
- Suggested Claude engineering task title: Image Generation Study Assets v1
- Suggested commit message: `feat(export): prepare optional study image assets`

### 4. 视频生成

- Capability ID: `official-video-generation-2201`
- Official category: 视频生成
- Official title: 视频生成
- docId: `2201`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/022-2201-视频生成/`
- Current ClassMate status: 未接入；只保留前置，近期不做真实工程。
- Priority: P3
- Decision: Defer; prepare only.
- Why it matters for ClassMate: 未来可生成知识点短视频或复赛展示素材。
- Why it matters for beating 听脑 or competition story: 是展示增强，不是超过听脑的核心能力。
- Official API style: HTTP task flow。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 知识点摘要、脚本、画面描述。
- Output type: 视频任务状态、视频结果。
- Key params summary: model、content、task submit/query parameters。
- Error handling summary: task pending / failed / timeout / content risk。
- Existing code likely involved: docs/demo_assets、Export proof pipeline。
- New provider/interface needed: `VivoVideoGenerationProvider` only if future decision changes。
- Cloud path: Official video generation service。
- On-device fallback path: 无。
- Manual fallback path: 人工录屏和剪辑。
- User confirmation requirement: 生成内容必须人工审核后才用于 proof。
- UI surface later involved: 不进入 App 主 UI；最多 proof tooling。
- Tests needed: none for app until implementation approved。
- Acceptance criteria: 当前仅文档准备，不生成用户可见入口。
- Risk: 成本高、等待时间长、内容不可控。
- Rollback plan: 不接入，继续使用人工演示视频。
- Suggested Claude engineering task title: Video Generation Evaluation v1
- Suggested commit message: `docs(proof): evaluate video generation for demo assets`

### 5. 通用 OCR

- Capability ID: `official-ocr-1737`
- Official category: 视觉技术
- Official title: 通用 OCR
- docId: `1737`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/007-1737-通用OCR/`
- Current ClassMate status: 已有手动 OCR 资料流和端侧图片语义草稿；真实 vivo OCR provider network smoke 已 PASS，App-level 图片/拍照闭环仍需 L3 真机验证。
- Priority: P0
- Decision: Build now.
- Why it matters for ClassMate: 课件截图、板书、教材页、题目图片需要可引用文字证据。
- Why it matters for beating 听脑 or competition story: 听脑偏录音转写，ClassMate 需要补齐图片/课件/板书输入。
- Official API style: HTTP POST form / image base64。
- Auth notes: Bearer AppKey / official auth reference；图片与凭据不得写入日志。
- Input type: jpg/png/bmp 图片。
- Output type: 文字、坐标、识别状态。
- Key params summary: requestId、image、pos、business id、session id。
- Error handling summary: OCR fail、image error、missing image、non-2xx；安全映射短错误。
- Existing code likely involved: import hub、MaterialBundle、OcrImport、EvidenceResolver、CourseAnalyzer。
- New provider/interface needed: `VivoOcrProvider`、`OcrProvider` contract、OCR result normalizer。
- Cloud path: Official OCR first。
- On-device fallback path: 端侧多模态生成图片语义草稿，但不能当作 OCR。
- Manual fallback path: 用户粘贴 OCR 文本或手动录入。
- User confirmation requirement: OCR 结果进入 MaterialBundle 前必须可编辑确认。
- UI surface later involved: 图片/拍照导入、Material Tray、Evidence source label、Export。
- Tests needed: image -> OCR draft、bad image safe fail、manual fallback、source marker、no file path leak。
- Acceptance criteria: OCR source 可进入 CourseAnalysis；Evidence 能引用 OCR 文字；失败不落库。
- Risk: 公式识别、图片质量、隐私、坐标与证据定位。
- Rollback plan: 关闭真实 OCR provider，保留手动 OCR 与端侧草稿。
- Suggested Claude engineering task title: Real OCR Smoke v1
- Suggested commit message: `feat(import): add vivo OCR smoke provider`

### 6. 文本翻译

- Capability ID: `official-translation-1733`
- Official category: 自然语言处理
- Official title: 文本翻译
- docId: `1733`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/003-1733-文本翻译/`
- Current ClassMate status: 未接入。
- Priority: P1
- Decision: Build later.
- Why it matters for ClassMate: 双语学习报告、英文 proof 摘要、外语课堂材料辅助。
- Why it matters for beating 听脑 or competition story: 增强报告国际化和跨语种学习，但不改变主链。
- Official API style: HTTP JSON。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 文本与语言方向。
- Output type: 翻译文本、状态码。
- Key params summary: requestId、from、to、text。
- Error handling summary: 参数错误、语言不支持、服务失败。
- Existing code likely involved: Export Center、StudyReport、Settings capability roadmap。
- New provider/interface needed: `VivoTranslationProvider`。
- Cloud path: Official translation service。
- On-device fallback path: 端侧大模型可提供草稿，但需标记 ON_DEVICE。
- Manual fallback path: 保留原文或用户手动翻译。
- User confirmation requirement: 翻译进入正式报告前需用户确认。
- UI surface later involved: Export bilingual report、proof materials。
- Tests needed: bilingual report redaction、translation fail fallback、source label。
- Acceptance criteria: 翻译不改变原始证据；报告清楚标记译文。
- Risk: 术语不一致、证据引用翻译后定位失败。
- Rollback plan: 关闭双语导出，保留中文报告。
- Suggested Claude engineering task title: Translation Assisted Learning v1
- Suggested commit message: `feat(export): add translation assisted report flow`

### 7. 文本向量

- Capability ID: `official-embedding-1734`
- Official category: 自然语言处理
- Official title: 文本向量
- docId: `1734`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/004-1734-文本向量/`
- Current ClassMate status: officialProviders.embedding 已配置并通过真实 network smoke；作为检索增强基础进入 L3 readiness baseline。
- Priority: P0
- Decision: Build now as part of Retrieval Providers v1.
- Why it matters for ClassMate: 支持课程库搜索、知识点聚类、跨课程相似课、长期 evidence retrieval。
- Why it matters for beating 听脑 or competition story: 将课堂资料变成可检索学习资产，而不是一次性总结。
- Official API style: HTTP JSON batch embedding。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 知识点、证据片段、课程摘要。
- Output type: vector embeddings。
- Key params summary: requestId、model name、sentences。
- Error handling summary: 参数错误、批量大小、模型不可用、网络失败。
- Existing code likely involved: Evidence index、Course Library、Ask retrieval。
- New provider/interface needed: `VivoEmbeddingProvider`、local vector cache policy。
- Cloud path: Official embedding service。
- On-device fallback path: 端侧大模型可生成关键词，但不是向量等价物。
- Manual fallback path: 本地 keyword/substring search。
- User confirmation requirement: 向量用于检索，不直接修改学习结果。
- UI surface later involved: Course Library search、Ask evidence references。
- Tests needed: vector request batching、cache redaction、fallback search、no raw prompt logging。
- Acceptance criteria: 提升 evidence retrieval，同时不保存敏感全文到不透明缓存。
- Risk: 隐私、缓存迁移、成本、阈值调优。
- Rollback plan: 回退到文本相似度或本地 keyword search。
- Suggested Claude engineering task title: Retrieval Providers v1
- Suggested commit message: `feat(retrieval): add official embedding provider contract`

### 8. 文本相似度

- Capability ID: `official-similarity-2060`
- Official category: 自然语言处理
- Official title: 文本相似度
- docId: `2060`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/017-2060-文本相似度/`
- Current ClassMate status: officialProviders.textSimilarity 已配置并通过真实 network smoke；可作为 evidence matching / rerank 增强进入 L3 readiness baseline。
- Priority: P0
- Decision: Build now as part of Retrieval Providers v1.
- Why it matters for ClassMate: 快速判断问题与证据片段、错题与知识点、相似知识点之间的关联。
- Why it matters for beating 听脑 or competition story: Ask 与 Review 可以基于证据相似度而不是自由发挥。
- Official API style: HTTP JSON rerank / similarity。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: query 与候选句子列表。
- Output type: 每个候选句子的相似度分数。
- Key params summary: requestId、model name、query、sentences。
- Error handling summary: 空候选、过长候选、服务失败、分数异常。
- Existing code likely involved: Ask engine、EvidenceResolver、Practice mapping、Weakness Hub。
- New provider/interface needed: `VivoTextSimilarityProvider`、`EvidenceReranker`。
- Cloud path: Official similarity service。
- On-device fallback path: 端侧大模型可解释候选但不作为分数源。
- Manual fallback path: 本地 BM25/keyword similarity。
- User confirmation requirement: 只影响排序；最终答案仍需 evidence gate。
- UI surface later involved: Ask evidence refs、Course Detail related points。
- Tests needed: rerank order、threshold fail、fallback order、redaction。
- Acceptance criteria: Ask 能返回更准确 evidenceRefs，低分时不胡编。
- Risk: 分数阈值不稳、候选召回不足。
- Rollback plan: 关闭 rerank，保留本地证据定位。
- Suggested Claude engineering task title: Retrieval Providers v1
- Suggested commit message: `feat(retrieval): add text similarity reranker`

### 9. 查询改写

- Capability ID: `official-query-rewrite-2061`
- Official category: 自然语言处理
- Official title: 查询改写
- docId: `2061`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/018-2061-查询改写/`
- Current ClassMate status: officialProviders.queryRewrite 已配置到 READY，但 live smoke/runtime path blocked；作为增强能力后置专项诊断，产品保留 qwen3.5-plus rewrite / local safe rewrite / direct retrieval fallback。
- Priority: P0
- Decision: Build now as part of Retrieval Providers v1.
- Why it matters for ClassMate: 把学生自然问题改写成更适合 evidence retrieval 的查询。
- Why it matters for beating 听脑 or competition story: Ask This Lesson 可以更稳地回到课堂证据，而不是变成开放问答。
- Official API style: HTTP JSON。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 当前问题与有限历史。
- Output type: 改写后的检索查询。
- Key params summary: requestId、prompts、q/a history fields。
- Error handling summary: 改写空结果、扩大范围、服务失败。
- Existing code likely involved: Ask prompt builder、Ask result parser、Evidence retrieval。
- New provider/interface needed: `VivoQueryRewriteProvider`、`AskRetrievalPlanner`。
- Cloud path: Official query rewrite service。
- On-device fallback path: 端侧模型生成候选查询，但必须标记 ON_DEVICE。
- Manual fallback path: 使用原问题检索。
- User confirmation requirement: 不直接入库；答案仍需证据定位。
- UI surface later involved: Ask debug summary only，不显示内部 prompt。
- Tests needed: rewrite success、rewrite fail original query fallback、not_found behavior。
- Acceptance criteria: Ask grounded/partial/not_found 更稳定；无证据仍拒绝胡编。
- Risk: 改写过度、泄漏上下文、证据召回偏移。
- Rollback plan: 关闭改写，使用原始问题。
- Suggested Claude engineering task title: Retrieval Providers v1
- Suggested commit message: `feat(ask): add query rewrite retrieval stage`

### 10. 实时短语音识别

- Capability ID: `official-short-asr-1738`
- Official category: ASR
- Official title: 实时短语音识别
- docId: `1738`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/008-1738-实时短语音识别/`
- Current ClassMate status: Live ASR 实验模式存在，但官方实时短语音 provider 未作为主链落地。
- Priority: P2
- Decision: Evaluate only after long ASR.
- Why it matters for ClassMate: 可支持短片段语音输入、实时课堂入口评估。
- Why it matters for beating 听脑 or competition story: 有助于追平实时转写入口，但长课堂主链优先级更高。
- Official API style: WebSocket。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 短音频帧，通常适合短时识别。
- Output type: started/result/error 事件与识别文本。
- Key params summary: requestId、engine id、audio type、VAD、binary audio frames。
- Error handling summary: handshake fail、result error code、socket timeout、audio format mismatch。
- Existing code likely involved: Live ASR experimental UI、TranscriptSession、audio recorder boundary。
- New provider/interface needed: `VivoShortAsrProvider`。
- Cloud path: Official short ASR service。
- On-device fallback path: 系统 SpeechRecognizer 或端侧语义草稿，不等价于官方短 ASR。
- Manual fallback path: 手动追加课堂片段。
- User confirmation requirement: transcript 进入 CourseAnalysis 前必须可编辑确认。
- UI surface later involved: Live Companion、Transcript Editor。
- Tests needed: socket state machine fake tests、permission denied、manual fallback。
- Acceptance criteria: 只作为短片段能力评估，不替代长语音转写。
- Risk: 实时连接、权限、噪声、短时限制。
- Rollback plan: 保留 Live manual / system experimental mode。
- Suggested Claude engineering task title: Real-time Short ASR Evaluation
- Suggested commit message: `feat(asr): evaluate official short speech provider`

### 11. 长语音听写

- Capability ID: `official-long-dictation-1740`
- Official category: ASR
- Official title: 长语音听写
- docId: `1740`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/010-1740-长语音听写/`
- Current ClassMate status: 官方 provider 未落地；与长语音转写一起做对照。
- Priority: P0
- Decision: Build now as long ASR candidate.
- Why it matters for ClassMate: 支持长课堂实时/长连接听写。
- Why it matters for beating 听脑 or competition story: 追平课堂录音转写类产品的基础能力。
- Official API style: WebSocket streaming。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 长音频流。
- Output type: incremental transcription events。
- Key params summary: requestId、engine id、audio type、language、punctuation、start/end frames。
- Error handling summary: connect fail、stream fail、partial result、finish event missing。
- Existing code likely involved: ASR provider abstraction、TranscriptSession、Transcript Editor。
- New provider/interface needed: `VivoLongDictationProvider` or mode under `VivoAsrProvider`。
- Cloud path: Official long dictation service。
- On-device fallback path: 系统 ASR / manual segmentation fallback。
- Manual fallback path: 粘贴转写稿或手动课堂片段。
- User confirmation requirement: transcript draft 必须用户确认。
- UI surface later involved: Live Companion、Import audio transcript、Transcript Editor。
- Tests needed: streaming fake server, reconnect, partial result merge, redaction。
- Acceptance criteria: 可生成可编辑 TranscriptDraft，不自动污染知识库。
- Risk: 长连接稳定、设备权限、功耗、弱网。
- Rollback plan: 使用 1739 长语音转写或手动 transcript。
- Suggested Claude engineering task title: Real ASR Long Audio Smoke v1
- Suggested commit message: `feat(asr): add long dictation smoke path`

### 12. 长语音转写

- Capability ID: `official-long-transcription-1739`
- Official category: ASR
- Official title: 长语音转写
- docId: `1739`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/009-1739-长语音转写/`
- Current ClassMate status: 官方 provider 未落地；这是 ASR 第一优先级。
- Priority: P0
- Decision: Build now.
- Why it matters for ClassMate: 本地课堂录音文件转 TranscriptDraft，是输入能力追平的核心。
- Why it matters for beating 听脑 or competition story: 先追平录音转写，再在证据链、微测、复习、练习闭环上超过。
- Official API style: HTTP task-flow with multipart upload and polling。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 本地音频文件。
- Output type: task status、transcript text、segments if available。
- Key params summary: requestId、session id、slice number、audio type、task id、query result。
- Error handling summary: upload fail、task fail、poll timeout、file too large、unsupported format。
- Existing code likely involved: Import audio path、TranscriptDraft、MaterialBundle、CourseAnalyzer。
- New provider/interface needed: `VivoLongAudioTranscriptionProvider`。
- Cloud path: Official long transcription service。
- On-device fallback path: 系统 ASR / short chunk fallback only if reliable。
- Manual fallback path: 用户粘贴音频转写稿。
- User confirmation requirement: transcript result 必须可编辑确认后进入 CourseAnalysis。
- UI surface later involved: Import audio、Transcript Editor、Material Tray。
- Tests needed: fake task flow、upload chunking, polling, cancellation, no file path leak。
- Acceptance criteria: 音频文件 -> TranscriptDraft -> user confirm -> CourseAnalysis。
- Risk: 文件大小、上传耗时、隐私、失败恢复。
- Rollback plan: 关闭真实 ASR，保留转写稿粘贴。
- Suggested Claude engineering task title: Real ASR Long Audio Smoke v1
- Suggested commit message: `feat(asr): add long audio transcription smoke flow`

### 13. 方言自由说

- Capability ID: `official-dialect-asr-2065`
- Official category: ASR
- Official title: 方言自由说
- docId: `2065`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/020-2065-方言自由说/`
- Current ClassMate status: 未接入。
- Priority: P2
- Decision: Evaluate only.
- Why it matters for ClassMate: 特定地区课堂或口语练习可作为后续扩展。
- Why it matters for beating 听脑 or competition story: 对标准课堂主链帮助有限，但可作为差异化边界能力。
- Official API style: WebSocket ASR-like。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 方言/自由说音频。
- Output type: 识别文本和状态事件。
- Key params summary: requestId、engine id、audio type、result/error action。
- Error handling summary: dialect unsupported、engine error、socket failure。
- Existing code likely involved: ASR abstraction only。
- New provider/interface needed: `DialectAsrProvider` if future approved。
- Cloud path: Official dialect ASR。
- On-device fallback path: 无等价端侧保证。
- Manual fallback path: 手动转写。
- User confirmation requirement: 转写结果仍需用户确认。
- UI surface later involved: Settings experimental capability only。
- Tests needed: capability hidden by default, no main-chain dependency。
- Acceptance criteria: 仅评估，不进入复赛主流程。
- Risk: 场景窄、测试成本高、准确率不确定。
- Rollback plan: 不展示入口。
- Suggested Claude engineering task title: Dialect / Simultaneous Interpretation Evaluation
- Suggested commit message: `docs(asr): evaluate dialect speech capability`

### 14. 同声传译

- Capability ID: `official-simultaneous-interpretation-2068`
- Official category: ASR
- Official title: 同声音传译
- docId: `2068`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/021-2068-同声音传译/`
- Current ClassMate status: 未接入。
- Priority: P2
- Decision: Evaluate only.
- Why it matters for ClassMate: 国际课程或双语课堂可后续评估。
- Why it matters for beating 听脑 or competition story: 非复赛主线，属于高级展示能力。
- Official API style: ASR + translation / streaming style。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: 语音流。
- Output type: 翻译文本或相关音频输出，具体以官方文档为准。
- Key params summary: requestId、language、stream events、result/error fields。
- Error handling summary: stream fail、translation fail、unsupported language。
- Existing code likely involved: ASR abstraction、translation provider、TranscriptSession。
- New provider/interface needed: `SimultaneousInterpretationProvider` if future approved。
- Cloud path: Official simultaneous interpretation。
- On-device fallback path: 端侧大模型可翻译文本草稿，但不等价。
- Manual fallback path: 用户提供翻译文本。
- User confirmation requirement: 翻译进入学习资料前需确认。
- UI surface later involved: Deferred。
- Tests needed: none until approved。
- Acceptance criteria: 当前只评估文档，不实现。
- Risk: 复杂度高、成本高、与主线距离远。
- Rollback plan: 不展示入口。
- Suggested Claude engineering task title: Dialect / Simultaneous Interpretation Evaluation
- Suggested commit message: `docs(asr): evaluate simultaneous interpretation capability`

### 15. 音频生成 / TTS

- Capability ID: `official-tts-audio-generation-1735`
- Official category: TTS
- Official title: 音频生成
- docId: `1735`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/005-1735-音频生成/`
- Current ClassMate status: 未接入。
- Priority: P1
- Decision: Build later after Export Study Report v1.
- Why it matters for ClassMate: 生成 3-8 分钟课程精华音频，用于睡前听、路上听、考前快速复习和导出可听学习资料。
- Why it matters for beating 听脑 or competition story: 把学习报告变成可听复习材料，而不是只导出静态文本。
- Official API style: WebSocket TTS / audio generation。
- Auth notes: Bearer AppKey / official auth reference。
- Input type: CourseAnalysis summary、Knowledge map、Weakness Hub、Review priority、Export Study Report 文本稿。
- Output type: 课程精华音频、可选逐字稿、export artifact。
- Key params summary: requestId、engine id、voice/style params、text payload、audio stream callbacks。
- Error handling summary: synthesis fail、stream timeout、audio file write fail、content length limit。
- Existing code likely involved: Export Center、StudyReport、audio artifact store。
- New provider/interface needed: `CourseEssenceAudioProvider`、`TtsProvider`。
- Cloud path: Official TTS / audio generation。
- On-device fallback path: 端侧文本生成可生成音频脚本，但不生成声音。
- Manual fallback path: 导出文字稿，让用户自行朗读或保存。
- User confirmation requirement: 音频生成前用户确认文本稿；生成后用户确认导出。
- UI surface later involved: Export Center、StudyReport、Review page。
- Tests needed: script redaction, audio artifact metadata, failure fallback, no real-person imitation wording。
- Acceptance criteria: 只用通用 TTS；不模拟老师/同学声音；不克隆任何真实人声。
- Risk: 音频时长、生成延迟、缓存、授权、用户误解。
- Rollback plan: 关闭音频导出，保留文字报告。
- Suggested Claude engineering task title: Course Essence Audio Export v1
- Suggested commit message: `feat(export): add course essence audio export`

### 16. 端侧 3B 大模型

- Capability ID: `official-ondevice-3b-1802`
- Official category: 端侧文本生成
- Official title: 端侧 3B 大模型
- docId: `1802`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/013-1802-端侧3B大模型/`
- Current ClassMate status: 已有端侧 BlueLM 3B / 端侧多模态工程基础；后续不是从零接入，而是 harden。
- Priority: P0
- Decision: Already partially implemented; validate, route, harden.
- Why it matters for ClassMate: 是 Cloud -> On-device -> Manual 路由中的 ON_DEVICE fallback 主引擎。
- Why it matters for beating 听脑 or competition story: 在弱网/无云端配置下仍可生成学习草稿，并支撑端侧多模态图片理解。
- Official API style: Android SDK / file package / on-device inference。
- Auth notes: AAR 和模型文件本地存在但不入库；不 direct import SDK 到 core。
- Input type: 文本 prompt、图片、多模态 prompt。
- Output type: generated text、image semantic draft、diagnostic result。
- Key params summary: model path、init、callVit、generate、callbacks、context length、multimodal flag。
- Error handling summary: model path unavailable、init fail、callVit fail、generate fail、invalid JSON、validation fail。
- Existing code likely involved: ondevice bridge、CourseAnalysis fallback、image draft flow。
- New provider/interface needed: `OnDeviceCapabilityProvider` hardening, doc parity checks。
- Cloud path: Cloud large model first。
- On-device fallback path: Official on-device 3B model。
- Manual fallback path: 安全占位 + user editable manual draft。
- User confirmation requirement: 端侧输出必须 JSON parse + validators；图片语义草稿需用户确认。
- UI surface later involved: Settings diagnostics、Import image/camera、CourseAnalysis source labels。
- Tests needed: unavailable not persisted、invalid JSON not persisted、validation fail not persisted、source ON_DEVICE。
- Acceptance criteria: 离线/弱网 fallback 稳定；不把多模态图片理解宣传成 OCR；不把端侧文本模型宣传成 ASR。
- Risk: 设备依赖、模型路径、AAR bridge、CI 无法完全覆盖。
- Rollback plan: 关闭端侧 fallback，保留云端和安全占位。
- Suggested Claude engineering task title: On-device 3B Fallback Hardening v1
- Suggested commit message: `feat(ondevice): harden 3b fallback routing`

### 17. 端侧文本审核

- Capability ID: `official-ondevice-text-safety-1804`
- Official category: 端侧文本生成
- Official title: 端侧文本审核
- docId: `1804`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/015-1804-端侧文本审核/`
- Current ClassMate status: 未接入。
- Priority: P1
- Decision: Build later.
- Why it matters for ClassMate: 导出/分享前本地安全检查，降低敏感内容外泄风险。
- Why it matters for beating 听脑 or competition story: 强化隐私和安全 proof。
- Official API style: Android on-device safety SDK。
- Auth notes: 本地 SDK 能力；不写真实凭据。
- Input type: 导出报告文本、分享摘要、用户编辑稿。
- Output type: safety result、risk label、message。
- Key params summary: SDK init、text moderation call、timeout、result code。
- Error handling summary: SDK unavailable、timeout、safe/unsafe/unknown state。
- Existing code likely involved: Export Center、Share flow、StudyReport。
- New provider/interface needed: `OnDeviceTextSafetyProvider`。
- Cloud path: 可选云端文本审核不在当前范围。
- On-device fallback path: Official on-device text moderation。
- Manual fallback path: 用户确认安全提示和本地 keyword scan。
- User confirmation requirement: 风险内容导出前提示用户确认。
- UI surface later involved: Export Center privacy check。
- Tests needed: safe/unsafe/timeout fake tests、export guard、no content logging。
- Acceptance criteria: 不阻断正常导出；风险提示明确；不上传文本。
- Risk: 误判、权限、SDK 版本、用户体验。
- Rollback plan: 关闭审核 provider，保留现有 redaction scan。
- Suggested Claude engineering task title: On-device Text Safety v1
- Suggested commit message: `feat(security): add on-device text safety gate`

### 18. 端侧能力相关文件

- Capability ID: `official-ondevice-files-1803`
- Official category: 端侧文本生成
- Official title: 端侧能力相关文件
- docId: `1803`
- Local captured path: `.codex_work/official_docs/vivo_aigc_docs/pages/014-1803-端侧能力相关文件/`
- Current ClassMate status: 本地 AAR 和模型路径已有使用；仍需 release checklist 对照官方文件要求。
- Priority: P0
- Decision: Already partially implemented; validate, route, harden.
- Why it matters for ClassMate: 校验端侧 SDK、模型文件、demo、设备条件与当前实现一致。
- Why it matters for beating 听脑 or competition story: 端侧能力是官方路径和弱网/隐私叙事的关键 proof。
- Official API style: file package / SDK artifact reference。
- Auth notes: AAR 不入库；模型文件不入库；只记录路径存在性和版本摘要。
- Input type: SDK artifact、model directory、device capability checks。
- Output type: diagnostics、readiness summary、proof checklist。
- Key params summary: model path、AAR policy、device requirement、download artifact references。
- Error handling summary: file missing、version mismatch、device unsupported。
- Existing code likely involved: ondevice diagnostics、QA scripts、README/current docs。
- New provider/interface needed: `OnDeviceReleaseChecklist` and proof automation。
- Cloud path: Cloud fallback if device package unavailable。
- On-device fallback path: Local model package when available。
- Manual fallback path: 记录不可用原因，进入安全占位。
- User confirmation requirement: 不直接产生学习内容；只影响 diagnostics。
- UI surface later involved: Settings diagnostics、proof pack。
- Tests needed: AAR ignored/untracked, model path probe, no direct SDK import。
- Acceptance criteria: 本地 artifact policy 明确；proof 不复制 AAR；CI 不依赖 AAR 内容。
- Risk: 官方文件版本变动、设备差异、误提交二进制。
- Rollback plan: 仅保留文档 checklist 和 cloud path。
- Suggested Claude engineering task title: On-device 3B Fallback Hardening v1
- Suggested commit message: `chore(ondevice): align local sdk artifact checks`

## Unified Priority Matrix

| Priority | Engineering item | Included official capabilities | Notes |
|---|---|---|---|
| P0 | AI Capability Routing v1 | 大模型、端侧 3B、端侧能力相关文件 | 统一 Cloud -> On-device -> Manual source routing。 |
| P0 | Cloud Large Model Routing v1 | 大模型 | 已有云端链路，做 router / source / hardening。 |
| P0 | On-device 3B Fallback Hardening v1 | 端侧 3B、端侧能力相关文件 | 已有基础，做 fallback、diagnostic、validator gate。 |
| P0 | Capture Picker Wiring v1 | 通用 OCR、ASR 输入前置 | 把图片/拍照/音频/转写稿进入统一资料篮。 |
| P0 | Real OCR Smoke v1 | 通用 OCR | 先 smoke，再进入 MaterialBundle。 |
| P0 | Real ASR Long Audio Smoke v1 | 长语音转写、长语音听写 | 长语音转写优先，听写对照。 |
| P0 | Evidence-grounded Ask v1 | 大模型、查询改写、文本相似度 | Ask 必须证据约束。 |
| P0 | Retrieval Providers v1 | 文本相似度、查询改写、文本向量 | 相似度先行，向量后续增强。 |
| P1 | Practice Generation v1 | 大模型、Function calling | 练习生成要受 evidence 与用户反馈约束。 |
| P1 | Weakness Hub / Review Priority v1 | 大模型、文本相似度 | 复习优先级从练习和反馈来。 |
| P1 | Export Study Report v1 | 大模型、翻译、端侧文本审核 | 打印级报告、双语辅助、安全检查。 |
| P1 | Course Essence Audio Export v1 | 音频生成 | 课程精华音频，不模拟具体老师或同学声音。 |
| P1 | Function Calling Router v1 | Function calling | 工具权限必须可控。 |
| P1 | Translation Assisted Learning v1 | 文本翻译 | 报告和外语材料辅助。 |
| P1 | On-device Text Safety v1 | 端侧文本审核 | 导出/分享前本地安全检查。 |
| P2 | Image Generation Study Assets v1 | 图片生成 | 复习卡/海报/展示增强。 |
| P2 | Real-time Short ASR Evaluation | 实时短语音识别 | 低于长语音。 |
| P2 | Dialect ASR / Simultaneous Interpretation Evaluation | 方言自由说、同声传译 | 评估，不进主链。 |
| P2 | Optional TTS Review Companion Integration | 音频生成 | 在课程精华音频后再评估朗读/提醒。 |
| P3 | Video Generation Evaluation v1 | 视频生成 | 后置展示增强。 |
| P3 | Ambient Loop Audio Player v1 | 非官方素材任务 | 授权背景音循环播放，不是官方音频生成。 |
| P3 | Fast App Proof v1 | 非官方工程任务 | proof 后置。 |
| P3 | Final UI Optimization v1 | 非官方工程任务 | Flow / Final UI 后置。 |

Priority counts in this matrix:

- P0: 8
- P1: 7
- P2: 4
- P3: 4
- Excluded: 2

## Claude Engineering Queue

Claude 每次只做一个真实落地工单。以下队列不包含 Excluded Capabilities。

| # | Task title | Depends on | Uses official docs | Must modify files | Must not modify files | Done means | Tests | Suggested commit message | Claude prompt seed |
|---:|---|---|---|---|---|---|---|---|---|
| 1 | AI Capability Routing v1 | Current provider baseline | 1745, 1802, 1803 | `core/ai`, `core/provider`, app state glue | validators weakening, SDK direct import | All AI outputs have source CLOUD / ON_DEVICE / MANUAL / SAFE_PLACEHOLDER | router unit tests, source label tests | `feat(ai): add capability routing core` | Build a source-aware AI router without changing validators. |
| 2 | Cloud Large Model Routing v1 | Task 1 | 1745 | cloud provider adapters, CourseAnalysis, Ask integration | qwen guard, credentials | Cloud path goes through router and keeps qwen guard | cloud success/fail, redaction | `feat(ai): route cloud large model through capability router` | Harden existing cloud model path through the new router. |
| 3 | On-device 3B Fallback Hardening v1 | Task 1 | 1802, 1803 | ondevice bridge glue, diagnostics, fallback logic | direct SDK import in core, AAR tracking | On-device fallback validates JSON before persistence | unavailable, invalid JSON, validation fail | `feat(ondevice): harden 3b fallback routing` | Align on-device 3B fallback with official docs and validator gate. |
| 4 | Capture Picker Wiring v1 | MaterialBundle baseline | 1737, 1739, 1740 | import picker, material tray, draft models | provider core | Image/audio/text inputs enter editable drafts | picker/draft tests | `feat(import): wire capture picker drafts` | Wire capture inputs into editable draft flow only. |
| 5 | Real OCR Smoke v1 | Task 4 | 1737 | `VivoOcrProvider`, OCR draft normalizer, import UI | ASR provider, validators | Image -> OCR draft -> user confirm -> MaterialBundle | fake OCR, bad image, source marker | `feat(import): add vivo OCR smoke provider` | Implement official OCR smoke path with manual fallback. |
| 6 | Real ASR Long Audio Smoke v1 | Task 4 | 1739, 1740 | `VivoAsrProvider`, transcript draft, polling/streaming | Live UI redesign, unrelated providers | Audio -> TranscriptDraft -> confirm -> CourseAnalysis | fake upload/poll, timeout, cancel | `feat(asr): add long audio transcription smoke flow` | Build long audio ASR smoke with editable transcript draft. |
| 7 | Evidence-grounded Ask v1 | Tasks 1, 8 | 1745, 2060, 2061 | Ask retrieval, parser, UI source labels | validators weakening | grounded/partial/not_found work with evidence | Ask parser/retrieval tests | `feat(ask): add evidence-grounded retrieval flow` | Make Ask answer only with locatable evidence. |
| 8 | Retrieval Providers v1 | Task 1 | 1734, 2060, 2061 | retrieval providers, reranker, query rewrite | cloud model prompt changes beyond need | Similarity/query rewrite/embedding contracts exist | fake provider tests, redaction | `feat(retrieval): add official retrieval providers` | Add retrieval providers for Ask and evidence matching. |
| 9 | Practice Generation v1 | Tasks 1, 7 | 1745, 1805 | practice generation, feedback wiring | ReviewStore destructive migration | Practice uses evidence and feedback | generation/fallback tests | `feat(practice): add evidence-based practice generation` | Generate practice from evidence, not freeform guesses. |
| 10 | Weakness Hub / Review Priority v1 | Task 9 | 2060, 1745 | weakness aggregation, review priority | provider resolver | Feedback changes review priority | aggregation tests | `feat(review): prioritize weaknesses from practice feedback` | Connect practice feedback to Review priority. |
| 11 | Export Study Report v1 | Tasks 7, 10 | 1733, 1804 | StudyReport, Export Center, safety gate | AAR, provider resolver | Printable report includes sources and safety note | export redaction tests | `feat(export): build printable study report` | Build print-grade report with source and safety summaries. |
| 12 | Course Essence Audio Export v1 | Task 11 | 1735 | TTS provider, audio export artifact | real-person voice imitation, background audio loop | Course summary can export to generic TTS audio | fake TTS, artifact tests | `feat(export): add course essence audio export` | Generate generic course essence audio from report script. |
| 13 | Function Calling Router v1 | Tasks 7, 9 | 1805 | tool router, permission policy | direct state mutation by model | Tool calls are validated and user-confirmed | unknown tool, arg validation | `feat(ai): add guarded function calling router` | Add constrained function calling for learning tools. |
| 14 | Translation Assisted Learning v1 | Task 11 | 1733 | translation provider, bilingual export | evidence mutation | Report can include confirmed translation | translation fail fallback | `feat(export): add translation assisted report flow` | Add optional bilingual report without changing evidence. |
| 15 | On-device Text Safety v1 | Task 11 | 1804 | safety provider, export/share gate | direct SDK import in core | Export/share can run local safety check | safe/unsafe/timeout tests | `feat(security): add on-device text safety gate` | Add local text safety gate for export and sharing. |
| 16 | Image Generation Study Assets v1 | Task 11 | 1732 | optional export asset provider | evidence chain | Generated images are display-only | no evidence contamination tests | `feat(export): prepare optional study image assets` | Evaluate study asset image generation as optional export feature. |
| 17 | Real-time Short ASR Evaluation | Task 6 | 1738 | short ASR provider experiment | long ASR path | Short ASR evaluated behind experimental flag | socket fake tests | `feat(asr): evaluate official short speech provider` | Evaluate short speech ASR after long audio path. |
| 18 | Dialect / Simultaneous Interpretation Evaluation | Task 17 | 2065, 2068 | docs or experimental provider only | main ASR flow | Evaluation result documented or hidden experiment | capability hidden tests | `docs(asr): evaluate dialect and interpretation capabilities` | Evaluate dialect and simultaneous interpretation without main-chain dependency. |
| 19 | Video Generation Evaluation v1 | Task 11 | 2201 | docs/proof tooling or optional provider | core learning chain | Evaluation only, no main UI claim | no app dependency | `docs(proof): evaluate video generation for demo assets` | Evaluate video generation as proof asset only. |
| 20 | Ambient Loop Audio Player v1 | Final UI baseline | non-official assets | Flow UI/audio asset player | official TTS, real-person voice imitation | Authorized loop audio can play in Flow mode | asset license checks | `feat(flow): add authorized ambient loop audio` | Add licensed local loop audio, not AI generated background audio. |
| 21 | Fast App Proof v1 | Proof readiness | none | docs/proof, maybe separate demo | main app chain | Fast App proof plan or demo exists | docs/proof checks | `docs(proof): add fast app proof plan` | Prepare Fast App proof only after core flow is stable. |
| 22 | Final UI Optimization v1 | Stable function baseline | none | UI screens and design tokens | provider/validators | Final screenshots pass subjective review | screenshot/smoke tests | `refactor(ui): polish final product screens` | Polish UI after function chain is stable. |

## Excluded Capabilities

### Voice Clone / 声音复刻

- Do not prepare.
- Do not implement.
- Do not create Claude task.
- Reason: 合规风险高，涉及老师/同学声音授权，与核心学习闭环弱。课程精华音频导出已经覆盖“听复习内容”的需求。

### LBS / POI / Geocoding

- Do not prepare.
- Do not implement.
- Do not create Claude task.
- Reason: 与 ClassMate 核心学习闭环弱，容易偏生活服务，对“追平输入、超过学习闭环”帮助低。

## Agent Division Policy

Codex 负责：

- 官方文档抓取。
- 官方文档摘要。
- 接口契约整理。
- 能力矩阵。
- 工单拆分。
- 验收清单。
- 风险清单。
- 测试清单。
- 提示词草案。
- repo 审计。
- 低风险脚本。
- 所有“不直接让功能真实运行”的前置任务。

Claude 负责：

- 真实功能实现。
- Provider 接入。
- 端到端链路打通。
- 复杂架构重构。
- 高难度 bug 修复。
- 真实 smoke。
- 能直接使用的功能。

禁止：

- 让 Claude 做纯文档矩阵。
- 让 Claude 做官方文档整理。
- 让 Claude 做不落地的报告。
- 让 Claude 做大范围 UI 换皮。
- 让 Claude 一次性接太多独立能力。
- 让 Claude 处理 Excluded Capabilities。

## Cloud / On-device / Manual Policy

所有生成式 AI 功能：

- Cloud first。
- On-device fallback。
- Manual safe fallback。

所有专用输入识别能力：

- Official specialized service first。
- On-device semantic fallback if modality supported。
- Manual editable fallback。
- User confirmation required。

所有输出必须带来源：

- `CLOUD`
- `ON_DEVICE`
- `MANUAL`
- `SAFE_PLACEHOLDER`

入库与展示边界：

- 所有 AI 输出入库前必须用户确认或通过明确 validator gate。
- 端侧多模态图片理解不能宣传成 OCR。
- 手动粘贴不能宣传成 ASR。
- 未接入实时 ASR 时不能写成已完成能力。
- 本地安全占位不能包装为智能分析结果。
- 背景音不能写成 AI 实时生成，除非未来确实实现并通过合规审查。

## Security / Secret Handling

- 不提交 `.codex_work`。
- 不提交 `config.local.json`。
- 不提交 AAR。
- 不打印 key。
- 不在文档中写真实 credential、token、cookie。
- Provider 通过配置注入。
- UI 不显示 key。
- 网络 smoke 需要用户授权和测试数据。
- 未接入能力不得 UI 宣称已完成。
- 背景音频素材必须授权明确。
- 课程精华音频不得模拟具体老师/同学声音。
- 日志、导出、历史、proof 不得包含 prompt/messages/vendor body/reasoning_content。

## Notes on Audio Positioning

官方“音频生成 / TTS”在 ClassMate 中的产品定位是课程精华音频导出：

- 由学习报告、知识地图、薄弱点、复习优先级生成 3-8 分钟可听复习内容。
- 可用于睡前听、路上听、考前快速复习。
- 输出为 generic TTS 音频，不模仿任何真实老师或同学。

Flow / 沉浸式背景音不使用官方音频生成作为主方案：

- 正确方案是准备授权明确的循环背景音频资源。
- App 内置或下载后缓存。
- 用户选择雨声、树林声、窗边下雨等背景音。
- 支持循环播放、音量控制、专注计时。
- 这是后置非官方素材任务，不属于官方 20 个接口能力准备卡。
