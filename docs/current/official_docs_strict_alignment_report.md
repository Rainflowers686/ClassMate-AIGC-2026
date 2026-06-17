# Official Docs Strict Alignment Report

Date: 2026-06-17

Scope: align the current ClassMate AI foundation with the locally captured vivo AIGC official docs. This report summarizes endpoint and schema facts for engineering decisions only. It does not copy official docs in full and does not contain credentials.

Local source:

- Mirror root: `.codex_work/official_docs/vivo_aigc_docs/`
- Index: `.codex_work/official_docs/vivo_aigc_docs/index.md`
- Quality report: `.codex_work/official_docs/vivo_aigc_docs/quality_report.md`

Excluded from product and smoke:

- Voice clone / 声音复刻
- LBS / POI / 地理编码

## Capability Alignment Table

| Capability | docId | Official title | Local path | Official endpoint / protocol summary | Auth | Request schema | Response schema | Important params | Error handling | Current implementation | Current status |
|---|---:|---|---|---|---|---|---|---|---|---|---|
| Large model | 1745 | 大模型 | `pages/011-1745-大模型/` | `POST https://api-ai.vivo.com.cn/v1/chat/completions` | `Bearer AppKey` plus request id; app id is used by current provider headers | OpenAI-compatible `model`, `messages`, `stream`, `max_tokens`, `temperature`, `top_p` | `choices[0].message.content`; `reasoning_content` may be present and must not be logged | models include `qwen3.5-plus`; `max_tokens` default 4096; `temperature` range 0-2; `top_p` default 0.7 | HTTP/vendor codes; parse failure; empty response; timeout | `core/provider/BlueLMProvider.kt`, `VendorIo.kt`, `ProviderAskChatClient.kt` | `live_ready` |
| Function calling | 1805 | Function calling | `pages/016-1805-Function-calling/` | Chat messages with tool/function call pattern | Same cloud model auth | `messages` plus tool schema/function result messages | Assistant tool-call style response | tool name, parameters, function result role | invalid tool, invalid args, state-change confirmation | `core/tools/InternalFunctionRouter.kt`; official seam only | `seam_only` |
| Image generation | 1732 | 图片生成 | `pages/002-1732-图片生成/` | `POST https://api-ai.vivo.com.cn/api/v1/image_generation` | `Bearer AppKey` | prompt/model/image inputs | image URLs/list/task result fields | Doubao image models, image URL/base64, sequential options | non-zero code and HTTP errors | Registry/dev-lab only | `smoke_only` |
| Video generation | 2201 | 视频生成 | `pages/022-2201-视频生成/` | task-flow endpoints such as submit task | `Bearer AppKey` | text/image-to-video task body | task id/result/status | Doubao video models | task failure and polling errors | Registry/dev-lab only | `smoke_only` |
| OCR | 1737 | 通用 OCR | `pages/007-1737-通用OCR/` | `POST http://api-ai.vivo.com.cn/ocr/general_recognition` with form body | `Bearer AppKey` | `requestId`, base64 `image`, `businessid`, optional position/session fields | text recognition result with error code/message | `businessid = aigc + appId`; jpg/png/bmp | `error_code`: success, OCR fail, image error; HTTP errors | `core/capture/VivoCaptureProviders.kt`; `app/capture/CaptureGateway.kt` | `provider_ready`, smoke mapping now conservative |
| Translation | 1733 | 文本翻译 | `pages/003-1733-文本翻译/` | `POST https://api-ai.vivo.com.cn/translation/query/self` | `Bearer AppKey` | `requestId`, `from`, `to`, text/query | translated text/code/request id | language codes, `zh-CHS` target | code table in official page | `core/translation/TranslationAssistedLearning.kt` seam | `seam_only` |
| Embedding | 1734 | 文本向量 | `pages/004-1734-文本向量/` | `POST https://api-ai.vivo.com.cn/embedding-model-api/predict/batch` | `Bearer AppKey` | `requestId`, `model_name`, `sentences` | embedding vectors | `m3e-base`, `bge-base-zh-v1.5` | code table / parser errors | `core/retrieval/RetrievalProviders.kt`; parser seam | `provider_ready`, live mapping missing without explicit endpoint |
| Text similarity | 2060 | 文本相似度 | `pages/017-2060-文本相似度/` | `POST https://api-ai.vivo.com.cn/rerank` | `Bearer AppKey` | `requestId`, `model_name`, `query`, `sentences` | scores array matching candidate order | `bge-reranker-large` | code table / empty scores | `core/capture/VivoCaptureProviders.kt`, `core/retrieval` | `provider_ready`, endpoint path mismatch to review |
| Query rewrite | 2061 | 查询改写 | `pages/018-2061-查询改写/` | `POST https://api-ai.vivo.com.cn/query_rewrite_base` | `Bearer AppKey` | official docs describe history/query prompt body; current provider sends generic `query` | `result` array plus `code` | query length <= official limit; history q/a fields | negative code table | `VivoQueryRewriteProvider` | `provider_ready`, request schema needs strict review |
| Short ASR | 1738 | 实时短语音识别 | `pages/008-1738-实时短语音识别/` | WebSocket short speech | `Bearer AppKey` | URL params + start frame + audio frames | `started`, `result`, `error` events | `engineid=shortasrinput`, `asr_info.audio_type` | websocket code table | Registry/dev-lab only | `smoke_only` |
| Long ASR dictation | 1740 | 长语音听写 | `pages/010-1740-长语音听写/` | WebSocket long dictation | `Bearer AppKey` | URL params + audio frames | streaming result events | `engineid=longasrlisten` | websocket code table | Secondary ASR reference | `smoke_only` |
| Long ASR transcription | 1739 | 长语音转写 | `pages/009-1739-长语音转写/` | HTTP task flow: `/lasr/create`, `/lasr/upload`, `/lasr/run`, `/lasr/progress`, `/lasr/result` | `Bearer AppKey` | create audio, multipart upload, run/progress/result | audio id, progress, transcript result | `engineid=fileasrrecorder`, `x-sessionId`, slices | task code table and polling failures | `VivoAsrProvider` in capture providers | `provider_ready`, needs non-sensitive audio smoke |
| Dialect ASR | 2065 | 方言自由说 | `pages/020-2065-方言自由说/` | WebSocket speech | `Bearer AppKey` | engine/model/audio params | speech result events | dialect engine ids | websocket code table | Registry/dev-lab only | `smoke_only` |
| Simultaneous interpretation | 2068 | 同声音传译 | `pages/021-2068-同声音传译/` | WebSocket ASR/translation/TTS-style stream | `Bearer AppKey` | engine/audio/target params | result/error events | `longasrsubtitle`, TTS params | websocket code table | Registry/dev-lab only | `smoke_only` |
| TTS / audio generation | 1735 | 音频生成 | `pages/005-1735-音频生成/` | `wss://api-ai.vivo.com.cn/tts?...` | `Bearer AppKey` | URL params then text payload | audio chunks/status/error | short/long synthesis engine ids | websocket error codes | `core/audio/CourseEssenceAudio.kt` script/TTS seam | `seam_only` |
| On-device 3B | 1802 | 端侧3B模型 | `pages/013-1802-端侧3B大模型/` | Android SDK/AAR local model | local SDK and model files; AAR not tracked | `modelPath`, text prompt, multimodal prompt after VIT | callback text/errors | `temperature`, `topP`, tokenizer/model path | SDK error codes | on-device bridge/app integration | `live_ready` on supported device, not CI-complete |
| On-device text safety | 1804 | 端侧文本审核 | `pages/015-1804-端侧文本审核/` | Android on-device safety SDK | local SDK | init + text safety call | code/result/risk response | SDK init/result code | SDK error tables | `core/safety/TextSafety.kt` seam | `seam_only` |
| On-device capability files | 1803 | 端侧能力相关文件 | `pages/014-1803-端侧能力相关文件/` | file/package reference | local artifacts only | model/AAR readiness | checklist/status | model path, device condition, artifact policy | missing file/version mismatch | docs/scripts/checklists | `provider_ready` reference |

## Large Model Parameter Alignment

| Parameter | Official docs | Current code before this pass | Current code after this pass | Conflict with guard | Decision |
|---|---|---|---|---|---|
| Model | official docs include several cloud models including `qwen3.5-plus` | `qwen3.5-plus` as cloud BlueLM-compatible default | unchanged | none | Keep qwen / cloud BlueLM-compatible path; do not switch to doubao. |
| `enable_thinking` / thinking | official docs state `qwen3.5-plus` defaults to thinking enabled and supports top-level `enable_thinking` | `qwen3.5-plus` request used a top-level `enable_thinking=false` compatibility guard | profile-aware: `DEEP_STUDY` and `BALANCED` send `enable_thinking=true` when the endpoint declares support; unsupported compatibility mode omits thinking fields | yes, old global false guard conflicted with quality goals | Replace global false guard with feature flags: `supportsEnableThinking`, `supportsReasoningEffort`, `supportsMaxCompletionTokens`. Response readers still detect reasoning metadata only and never surface or log reasoning text. |
| `reasoning_effort` | official values include `minimal`, `low`, `medium`, `high` | not sent | `DEEP_STUDY=high`, `BALANCED=medium`, `FAST=medium` when supported | no | Use high for persistent learning outputs and medium for default/fast tasks. |
| `max_tokens` | default 4096; does not include thinking content | default provider used lower cap in some paths | `DEEP_STUDY` and `BALANCED` use 4096 unless a repair call-site cap is explicit | no | Keep visible answer length high for CourseAnalysis / Ask / Report. |
| `max_completion_tokens` | official range `[0, 65,536]`; includes answer plus thinking chain | not sent | `DEEP_STUDY=65,536`, `BALANCED=32,768`, `FAST=8,192` when supported | no | Use the official high ceiling for deep study while preserving compatibility flags. |
| `temperature` | range 0-2, default 1 | configured per provider, often low | profile-based: FAST 0.20, BALANCED 0.35, DEEP_STUDY 0.30 | no | Keep conservative values for evidence-grounded learning. |
| `top_p` | default 0.7 | not sent | sent by quality profile; `DEEP_STUDY=0.90`, `BALANCED=0.90`, `FAST=0.85` | no | Increase diversity slightly while keeping learning output stable. |
| `frequency_penalty` | range `[-2.0, 2.0]` | not sent | `DEEP_STUDY=0.20`, `BALANCED=0.15`, `FAST=0.10` | no | Reduce repetitive explanations in study reports and feedback. |
| `presence_penalty` | range `[-2.0, 2.0]` | not sent | `DEEP_STUDY=0.08`, `BALANCED=0.08`, `FAST=0.05` | no | Encourage useful next-step coverage without making factual tasks too divergent. |
| `stream` | supported | false in current core tasks | unchanged | no | Keep non-streaming for parse/validator-gated tasks. |
| timeout/retry | official docs expose normal HTTP behavior; project uses profiled transport | BlueLM analysis has long read timeout and requestId retry | unchanged for provider; smoke harness gets explicit timeout | no | Keep production timeout profile; add smoke timeout. |

## Quality Profile Policy

New core model quality profiles:

- `FAST`: low-latency UI responses, not for persistent learning outputs.
- `BALANCED`: default learning work.
- `DEEP_STUDY`: high-quality learning outputs.

The following learning tasks must use `DEEP_STUDY` or an equivalent high-quality profile:

- CourseAnalysis
- Ask with evidence
- Practice generation
- Practice feedback
- Review plan
- StudyReport
- CourseEssenceScript

The profile now controls public generation parameters plus qwen thinking fields. The old global `enable_thinking=false` guard is replaced by feature flags:

- supported qwen path: `enable_thinking=true`
- supported qwen path: `reasoning_effort=high` for `DEEP_STUDY`, `medium` for `BALANCED` and `FAST`
- unsupported compatibility path: omit thinking / reasoning / max-completion / penalty fields instead of forcing a failing request
- response readers keep reasoning text private and expose only `reasoningContentPresent` and length metadata

## Official Provider Config Schema v1

Specialized official capabilities must use dedicated config groups. Generic top-level BlueLM/qwen config is only for cloud large-model text generation and must not make OCR, ASR, retrieval, translation, TTS, or function calling `READY`.

Example schema shape with placeholders only:

```json
{
  "officialProviders": {
    "ocr": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "queryRewrite": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "textSimilarity": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "embedding": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "translation": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "tts": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "functionCalling": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "asrLong": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" }
  }
}
```

The app and smoke harness store only configured/missing status for these groups. They must not print or export actual URL or credential values.

## Endpoint Mapping Findings

### OCR 404 Root Cause

The previous smoke v3 mapping treated `topLevel.bluelm`, `providers.bluelm`, or `providers.qwen` as sufficient to construct OCR and retrieval URLs. A user network smoke for OCR then sent a request with `mappingSource=LOCAL_CONFIG_BLUELM` and received HTTP 404.

This is a mapping bug, not proof that the official OCR service is unavailable:

- Generic qwen/BlueLM cloud model config is only valid for text generation.
- OCR has a dedicated official endpoint and form schema.
- OCR smoke must require explicit OCR env config or capture-specific local config (`vivoCapture` / `officialProviders.vivoCapture`), not a generic LLM endpoint.
- The current code also needs strict review because the official OCR doc shows the `http://.../ocr/general_recognition` endpoint while current provider code builds an HTTPS URL from a domain.

### Query Rewrite Hang

The previous smoke path did not expose a request timeout parameter. Smoke v4 adds `-TimeoutSeconds` with default 20 seconds and classifies timeouts as `FAIL_TIMEOUT`. It also keeps query rewrite `MISSING` unless a doc-specific endpoint mapping is explicit, so generic qwen config cannot send retrieval smoke requests.

## Smoke Harness v4 Mapping Rules

- Default mode remains dry-run.
- `config.local.json` is not read unless `-UseLocalConfig` is explicit.
- `topLevel.bluelm`, `providers.bluelm`, and `providers.qwen` may only describe cloud text generation.
- Generic cloud config cannot make OCR, ASR, retrieval, translation, TTS, or function calling `READY`.
- OCR/ASR long only become `READY` from explicit env endpoint/auth or capture-specific local config.
- Retrieval providers require explicit env endpoint or future retrieval-specific local config.
- Translation/TTS/Function calling remain `SEAM_ONLY` unless explicit live endpoint mapping is supplied.
- Network requests are sent only when endpoint, auth, and request schema are all `READY`.

## Current Mapping Snapshot

Observed on 2026-06-17 with `-ExplainConfig -UseLocalConfig`:

| Capability | Endpoint mapping | Auth mapping | Request schema | Mapping source | Request sent |
|---|---|---|---|---|---:|
| OCR | MISSING | MISSING | READY | NONE | false |
| QUERY_REWRITE | MISSING | MISSING | READY | NONE | false |
| TEXT_SIMILARITY | MISSING | MISSING | READY | NONE | false |
| TRANSLATION | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | false |
| TTS | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | false |
| FUNCTION_CALLING | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | false |
| EMBEDDING | MISSING | MISSING | READY | NONE | false |

Detected config group in the local opt-in run: `topLevel.bluelm`. No value was printed.

## Follow-up Work

- Align `VivoTextSimilarityProvider` endpoint to official `/rerank` if current provider path is not accepted by vivo.
- Align `VivoQueryRewriteProvider` body shape to the official `query_rewrite_base` schema before network smoke.
- Decide whether OCR should use official `http://` endpoint exactly or whether HTTPS is accepted by vivo gateway.
- Add retrieval-specific local config groups if real endpoints need to be configured without env variables.
- Run real smoke only after `officialProviders.*` or explicit env mapping exists for each specialized capability.
- Keep qwen on `qwen3.5-plus`; do not switch to doubao.
- If a deployed endpoint rejects thinking fields, use the compatibility feature flags to omit unsupported fields for that endpoint while keeping `DEEP_STUDY` as the default supported profile.
