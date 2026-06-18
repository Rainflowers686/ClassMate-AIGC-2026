# vivo AIGC 官方能力工程矩阵

本文基于本地已抓取的 vivo AIGC 官方接口文档镜像整理，作为 ClassMate 后续 ASR / OCR / Retrieval / Function calling / 端侧能力接入的工程决策矩阵。本文只记录接口级摘要和接入判断，不复制官方文档全文，不保存任何 cookie、token 或真实凭据。

本地镜像来源：

- 抓取目录：`.codex_work/official_docs/vivo_aigc_docs/`
- 入口：`https://aigc.vivo.com.cn/#/document/index?id=1802`
- 抓取结果：22 个文档页、175 个 code blocks、133 个 tables、真实文档 JSON API 响应
- 关键索引：`.codex_work/official_docs/vivo_aigc_docs/index.md`
- 质量报告：`.codex_work/official_docs/vivo_aigc_docs/quality_report.md`

## 2026-06-18 Current Provider Smoke Matrix

| Provider | Config status | Live smoke status | Current decision |
|---|---|---|---|
| OCR | `READY` | `PASS` | Product-facing official OCR provider smoke is verified. |
| QUERY_REWRITE | `READY` | `PASS` | Product-facing official query rewrite smoke is verified after the docId 2061 `prompts` payload schema fix. |
| TEXT_SIMILARITY | `READY` | `PASS` | Product-facing official rerank/evidence matching smoke is verified. |
| EMBEDDING | `READY` | `PASS` | Product-facing official vector retrieval foundation smoke is verified. |
| TRANSLATION | seam-only | not run | Post-L3 or when device findings require it. |
| TTS | seam-only | not run | Post-L3; keep course essence script-only fallback. |
| FUNCTION_CALLING | seam-only | not run | Internal tool router remains source of truth. |
| ASR_LONG | deferred | not run | Separate validation with non-sensitive audio. |

Current mainline: App-level L3 cloud-device end-to-end validation. Do not keep expanding features before device acceptance; optimize based on L3 blockers, warnings, and polish findings.

注意：`docId=1802` 是“端侧 3B 大模型”，不是 ASR 文档。ASR 重点文档是 `1738`、`1739`、`1740`。

## Priority Legend

- `P0`：立即接入或立即做工程对照
- `P1`：近期接入
- `P2`：评估后接入
- `P3`：展示增强 / 后置
- `HOLD`：暂不做

## P0 / P1 Core Capability Matrix

| Priority | Capability | docId | Official title | Local captured path | API style / protocol | Auth / credential notes | Input | Output | Key params / callbacks | Error handling location | ClassMate usage | Implementation target | Risk | Decision |
|---|---:|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| P0 | ASR：长语音转写 | 1739 | 长语音转写 | `.codex_work/official_docs/vivo_aigc_docs/pages/009-1739-长语音转写/content.md` | HTTP task flow：创建音频、分片上传、创建任务、查询进度、查询结果 | 使用官方网关凭据；Provider 必须走本地配置注入，不能硬编码 | 本地课堂录音文件 | 转写文本、任务状态、分片/会话信息 | `requestId`、`engineid`、`x-sessionId`、`slice_num`、音频格式与分片大小 | `tables.md` 与 `code_blocks.txt` | 课堂录音转 `TranscriptDraft`，是 ClassMate ASR 第一优先级 | `VivoAsrProvider`、`TranscriptDraftImporter` | 上传耗时、文件大小、任务轮询、权限与隐私提示 | Build first |
| P0 | OCR：通用 OCR | 1737 | 通用OCR | `.codex_work/official_docs/vivo_aigc_docs/pages/007-1737-通用OCR/content.md` | HTTP POST，表单参数，图片 base64 | 使用官方网关凭据；不要把图片或凭据写入日志 | 课件截图、板书照片、教材页、题目图片 | 识别文字、坐标、方向等结构化结果 | `requestId`、`image`、`pos`、`businessid`、`sessid` | `tables.md` | 图片/拍照资料转文字，与端侧多模态草稿形成双轨 | `VivoOcrProvider`、`OcrMaterialImporter` | 图片质量、公式识别、坐标映射、隐私合规 | Build after ASR |
| P0 | 端侧 3B 大模型对照 | 1802 | 端侧3B大模型 | `.codex_work/official_docs/vivo_aigc_docs/pages/013-1802-端侧3B大模型/content.md` | Android 端侧 SDK / AAR，文本与多模态调用 | AAR 本地存在但 gitignored；不 direct import SDK 到 core | 文本 prompt、图片输入、端侧模型文件 | 端侧生成文本、多模态草稿、诊断状态 | `init`、`callVit`、`generate`、`TokenCallback`、模型路径、`multimodal`、`nCtx` | `tables.md` 与 `code_blocks.txt` | 校验当前端侧 BlueLM 3B 接入与官方文档一致性 | `OnDeviceDocParityCheck`、现有端侧 bridge 审计 | 设备依赖强，CI 无法完全验证，SDK bridge 需隔离 | Keep and audit |
| P1 | ASR：长语音听写 | 1740 | 长语音听写 | `.codex_work/official_docs/vivo_aigc_docs/pages/010-1740-长语音听写/content.md` | WebSocket 长连接，流式发送音频 | 使用官方网关凭据；不要保存原始音频或完整转写日志 | 课堂实时或长时音频流 | 流式听写文本、状态事件 | `requestId`、`engineid`、`asr_info`、二进制音频帧、start/finish 事件 | `tables.md` 与 `code_blocks.txt` | 与 1739 对照，评估 Live ASR 与长课堂听写 | `VivoAsrProvider` streaming profile | 长连接稳定性、断线恢复、权限提示、功耗 | Build after 1739 smoke |
| P1 | ASR：实时短语音识别 | 1738 | 实时短语音识别 | `.codex_work/official_docs/vivo_aigc_docs/pages/008-1738-实时短语音识别/content.md` | WebSocket，短音频单轮识别 | 使用官方网关凭据；适合短片段，不做长课堂主链 | 60 秒内短语音片段 | 短句识别文本、状态事件 | `requestId`、`engineid`、`asr_info.end_vad_time`、音频帧、回调事件 | `tables.md` 与 `code_blocks.txt` | 片段转写、短语音输入、实时课堂入口评估 | `VivoShortAsrProvider` 或 `VivoAsrProvider.ShortMode` | 不适合长课堂完整录音，误用会造成体验不稳定 | Build after long ASR |
| P1 | 文本相似度 | 2060 | 文本相似度 | `.codex_work/official_docs/vivo_aigc_docs/pages/017-2060-文本相似度/content.md` | HTTP JSON rerank | 使用官方网关凭据；只发送必要片段，不上传完整隐私材料 | query 与候选句子列表 | 每个候选句子的相似度分数 | `requestId`、`model_name`、`query`、`sentences` | `tables.md` | 轻量 evidence matching、错题关联知识点、相似知识点去重 | `VivoTextSimilarityProvider` | 排序质量依赖候选集，需阈值和本地兜底 | Build before embeddings |
| P1 | 查询改写 | 2061 | 查询改写 | `.codex_work/official_docs/vivo_aigc_docs/pages/018-2061-查询改写/content.md` | HTTP JSON | 使用官方网关凭据；Ask 问题需脱敏和最小化上下文 | 多轮问答历史与当前问题 | 改写后的检索查询 | `requestId`、`prompts`、`q/a` 历史字段 | `tables.md` | Ask This Lesson 检索增强，提高 evidence recall | `VivoQueryRewriteProvider` | 改写可能扩大问题范围，必须继续 evidence-gated | Build with Ask retrieval |
| P1 | 文本向量 | 1734 | 文本向量 | `.codex_work/official_docs/vivo_aigc_docs/pages/004-1734-文本向量/content.md` | HTTP JSON batch embedding | 使用官方网关凭据；向量缓存不能包含原始敏感文本 | 短文本数组、知识点、证据片段 | 向量数组 | `requestId`、`model_name`、`sentences` | `tables.md` | 课程库搜索、知识点聚类、跨课程相似课、长期 evidence retrieval | `VivoEmbeddingProvider` | 成本、缓存策略、隐私、向量版本迁移 | Build after similarity provider |

## P2 Evaluation Matrix

| Priority | Capability | docId | Official title | Local captured path | API style / protocol | Auth / credential notes | Input | Output | Key params / callbacks | Error handling location | ClassMate usage | Implementation target | Risk | Decision |
|---|---:|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| P2 | Function calling | 1805 | Function calling | `.codex_work/official_docs/vivo_aigc_docs/pages/016-1805-Function-calling/content.md` | Chat message + tool schema pattern | 继续沿用 BlueLM 凭据配置，不单独硬编码 | 用户问题、工具描述、课堂上下文摘要 | 工具调用描述或结构化响应 | `messages`、tool schema、assistant tool-call 格式 | `tables.md` 与 `content.md` | 后续 `searchEvidence`、`createQuiz`、`updateMastery`、`exportReport` | `FunctionCallingRouter` | 工具调用必须严控权限，避免模型越权改学习状态 | Evaluate only |
| P2 | TTS：音频生成 | 1735 | 音频生成 | `.codex_work/official_docs/vivo_aigc_docs/pages/005-1735-音频生成/content.md` | WebSocket TTS，流式音频 | 使用官方网关凭据；音频输出不应含敏感文本 | 复习卡文本、提醒文案 | PCM 音频流 | `requestId`、`engineid`、音色、采样率、回调事件 | `tables.md` | 复习卡朗读、Flow Companion 声音播报 | `TtsProvider` | 声音体验、播放权限、音频缓存、用户隐私 | Defer until ASR/OCR stable |
| P2 | 端侧文本审核 | 1804 | 端侧文本审核 | `.codex_work/official_docs/vivo_aigc_docs/pages/015-1804-端侧文本审核/content.md` | Android 端侧审核 SDK | 本地 SDK 能力，不写入凭据；权限需 release/privacy audit | 待审核文本 | 审核结果、风险类型、响应 id | SDK init、审核调用、超时、结果码 | `tables.md` | 未来本地安全审核、导出/分享前安全检查 | `OnDeviceTextModerationProvider` | 端侧依赖、误判、权限声明、用户提示 | Evaluate only |
| P2 | 端侧能力相关文件 | 1803 | 端侧能力相关文件 | `.codex_work/official_docs/vivo_aigc_docs/pages/014-1803-端侧能力相关文件/content.md` | 文件清单 / demo / SDK artifact reference | AAR 与模型文件只本地使用，不入库 | SDK、demo、模型文件说明 | 文件与能力说明 | 下载项、版本、设备条件 | `content.md` | 端侧模型文件路径、设备条件、SDK 对照清单 | `OnDeviceReleaseChecklist` | 文件版本漂移，不能把官方二进制提交到仓库 | Keep as reference |

## P3 / HOLD Matrix

| Priority | Capability | docId | Official title | Local captured path | API style / protocol | Auth / credential notes | Input | Output | Key params / callbacks | Error handling location | ClassMate usage | Implementation target | Risk | Decision |
|---|---:|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| P3 | 图片生成 | 1732 | 图片生成 | `.codex_work/official_docs/vivo_aigc_docs/pages/002-1732-图片生成/content.md` | HTTP task / image generation | 使用官方网关凭据；不作为学习主链 | 文本或图片提示 | 图片结果或任务状态 | `requestId`、prompt、model、参数 | `tables.md` | 展示增强、海报/复习卡配图可评估 | `ImageGenerationProvider` | 容易偏离课堂学习闭环，版权和审核成本 | Defer |
| P3 | 视频生成 | 2201 | 视频生成 | `.codex_work/official_docs/vivo_aigc_docs/pages/022-2201-视频生成/content.md` | HTTP task flow | 使用官方网关凭据；不处理第三方平台爬取 | 文本/内容参数 | 视频任务与结果 | 任务提交、查询、模型参数 | `tables.md` | 复赛展示增强，不进入主链 | `VideoGenerationProvider` | 成本高、时间长、非核心 | Defer |
| P3 | 文本翻译 | 1733 | 文本翻译 | `.codex_work/official_docs/vivo_aigc_docs/pages/003-1733-文本翻译/content.md` | HTTP JSON | 使用官方网关凭据 | 文本、语言对 | 翻译结果 | `requestId`、源语言、目标语言 | `tables.md` | 双语报告、英文 proof 摘要 | `VivoTranslationProvider` | 非当前学习闭环核心 | Evaluate later |
| P3 | LBS / POI | 1736 | 地理编码(POI搜索) | `.codex_work/official_docs/vivo_aigc_docs/pages/006-1736-地理编码(POI搜索)/content.md` | HTTP API | 使用官方网关凭据 | 地址、POI 查询 | 地理编码或 POI 结果 | 查询、坐标、城市等参数 | `tables.md` | 当前 ClassMate 无强需求 | None | 与课堂学习弱相关 | Do not build now |
| P3 | 方言自由说 | 2065 | 方言自由说 | `.codex_work/official_docs/vivo_aigc_docs/pages/020-2065-方言自由说/content.md` | ASR 类能力，具体协议见抓取页 | 使用官方网关凭据 | 方言/自由说音频 | 识别文本 | 语种/音频/回调参数 | `tables.md` | 特定课堂方言场景可评估 | `DialectAsrProvider` | 场景窄，测试成本高 | Defer |
| P3 | 同声音传译 | 2068 | 同声音传译 | `.codex_work/official_docs/vivo_aigc_docs/pages/021-2068-同声音传译/content.md` | ASR + 翻译/音频类能力 | 使用官方网关凭据 | 语音流 | 翻译文本或音频 | 语音流、目标语言、回调事件 | `tables.md` | 国际课程展示增强 | `InterpretationProvider` | 复杂度高，非复赛主线 | Defer |
| HOLD | 声音复刻 | 2062 | 声音复刻 | `.codex_work/official_docs/vivo_aigc_docs/pages/019-2062-声音复刻/content.md` | HTTP task / voice clone | 高隐私风险，暂不接入 | 声音样本与文本 | 声音复刻任务/voice id | 上传音频、文本、任务状态 | `tables.md` | 暂无必要，不进入课堂学习主链 | None | 声纹/隐私/授权风险高 | Do not build |

## Recommended Provider Work Order

Previous provider work now has real smoke PASS for OCR, Query Rewrite, Text Similarity, and Embedding. Query Rewrite had previously appeared live-smoke blocked, but Claude traced that to a smoke request body schema mismatch and fixed the `prompts` payload required by official docId 2061.

Current order:

1. Run App-level L3 cloud-device end-to-end validation.
2. Use the current provider matrix as the official-provider readiness baseline.
3. Let device findings drive fixes; do not add broad new features before L3 acceptance.
4. Keep Query Rewrite fallback paths in product behavior: qwen3.5-plus rewrite when available, then local safe rewrite or direct retrieval.
5. Translation, TTS, Function Calling, and ASR Long remain post-L3 or separate validation items.
6. Image generation / video generation / LBS / voice clone remain outside the current product mainline; voice clone and LBS/POI remain excluded.

## ClassMate vs 听脑类产品：能力差异

听脑类录音转写/AI 总结产品通常偏重：

- 录音
- 转写
- 总结
- 随时问答
- 分享导出

ClassMate 的目标不是替代这类产品，而是在追平基础输入能力后，把课堂材料转成可验证、可练习、可复习的学习闭环：

- 声音 / 图片 / 文档 -> 结构化课堂资料
- ASR / OCR / 端侧多模态输入
- 用户确认，避免自动污染知识库
- `CourseAnalysis`
- Evidence 校验
- 知识地图
- Ask evidence-grounded
- Practice
- Weakness Hub
- Review priority
- Export 学习报告

官方接口在这个闭环中的位置：

- ASR：把课堂声音转成可编辑课堂资料。
- OCR：把课件、板书、教材页和题目图片转成文字证据。
- 端侧多模态：生成图片语义草稿和本地诊断，不替代 OCR。
- 查询改写 / 文本相似度 / 文本向量：增强 evidence retrieval、Ask grounding、课程库聚合。
- 蓝心大模型：负责课堂分析、问答、练习解释，但所有入库结果必须 validator-gated。
- TTS / Flow Companion：用于复习陪伴和朗读，后置。

## Security / Secret Handling

1. `.codex_work` 是本地抓取资料，不提交。
2. API key / AppID / AppKEY 不写入代码、测试、文档正文或截图。
3. `config.local.json` 只本地使用；本文不读取、不记录其内容。
4. 本矩阵不包含 cookie、token、完整请求头或真实凭据。
5. AAR 只作为本地 SDK 依赖，不提交。
6. Provider 接入必须通过安全配置读取和运行时注入，不能硬编码凭据。
7. UI 只能显示 masked / present / absent 状态，不显示 key 内容。
8. ASR / OCR / TTS 等能力未接入前，不允许用户可见文案声称已完成。
9. 不把 LocalRule / 安全占位当作用户可见智能能力展示。
10. 不把 DeepSeek / Compatible 作为复赛主 AI 路径。
11. 不声称多模态替代 OCR；图片理解与 OCR 是双轨能力。

## Immediate Engineering Decisions

- `1739` 长语音转写是下一阶段 ASR 第一优先级。
- `1737` 通用 OCR 是图片/课件/板书文字提取第一优先级。
- `2060` 文本相似度应先于 `1734` 文本向量落地，用低成本方式提升 evidence matching。
- `2061` 查询改写应服务 Ask This Lesson，而不是生成开放式闲聊。
- `1802` 只用于端侧 3B 文档一致性审计和真机能力对照，不要误归类为 ASR。
- `1805` Function calling 先评估，不立即接入生产主链。
- `2062` 声音复刻暂不做，避免隐私和授权风险。
