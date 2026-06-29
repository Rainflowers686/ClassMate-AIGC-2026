> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Official Docs Strict Alignment Report

Date: 2026-06-17

Scope: align the current ClassMate AI foundation with the locally captured vivo AIGC official docs. This report summarizes endpoint and schema facts for engineering decisions only. It does not copy official docs in full and does not contain credentials.

Local source:

- Mirror root: `.codex_work/official_docs/vivo_aigc_docs/`
- Index: `.codex_work/official_docs/vivo_aigc_docs/index.md`
- Quality report: `.codex_work/official_docs/vivo_aigc_docs/quality_report.md`

## 2026-06-18 Current Provider Smoke Update

This report originally captured strict endpoint/schema alignment before live provider smoke had broad coverage. Current product-facing smoke status is:

| Provider | Config status | Live smoke status | Sanitized request shape |
|---|---|---|---|
| OCR | `READY` | `PASS` | `POST`, `application/x-www-form-urlencoded`, `FORM`, path last segment `general_recognition`, query key `requestId` |
| QUERY_REWRITE | `READY` | `PASS` | `POST`, `application/json`, `GENERIC_JSON`, path last segment `query_rewrite_base`; PASS after docId 2061 `prompts` request-body schema fix |
| TEXT_SIMILARITY | `READY` | `PASS` | `POST`, `application/json`, `GENERIC_JSON`, path last segment `rerank`, query key `requestId` |
| EMBEDDING | `READY` | `PASS` | `POST`, `application/json`, `GENERIC_JSON`, path last segment `batch`, query key `requestId` |

Interpretation:

- OCR, Query Rewrite, Text Similarity, and Embedding are the first four official product-facing providers with real network smoke `PASS`.
- Query Rewrite was previously blocked by a smoke request body schema mismatch: old payload `{ "query": "..." }`; official docId 2061 requires `{ "prompts": [[q3,a3,q2,a2,q1,a1],[current_query]] }`. Claude fixed the harness schema and verified `PASS`.
- Query Rewrite product fallback remains qwen3.5-plus rewrite when available, then local safe rewrite or direct retrieval.
- Translation, TTS, Function Calling, and ASR Long remain seam-only/deferred or separate validation items.
- Next mainline is App-level L3 cloud-device end-to-end validation, not more feature expansion.

ClassMate product matrix note:

- This report records official documentation facts for engineering alignment.
- The current ClassMate product capability matrix is maintained separately and contains 18 learning-loop capabilities only.
- Do not use this raw alignment report as a product capability list.


## Capability Alignment Table

| Capability | docId | Official title | Local path | Official endpoint / protocol summary | Auth | Request schema | Response schema | Important params | Error handling | Current implementation | Current status |
|---|---:|---|---|---|---|---|---|---|---|---|---|
| Large model | 1745 | 澶фā鍨?| `pages/011-1745-澶фā鍨?` | `POST https://api-ai.vivo.com.cn/v1/chat/completions` | `Bearer AppKey` plus request id; app id is used by current provider headers | OpenAI-compatible `model`, `messages`, `stream`, `max_tokens`, `temperature`, `top_p` | `choices[0].message.content`; `reasoning_content` may be present and must not be logged | models include `qwen3.5-plus`; `max_tokens` default 4096; `temperature` range 0-2; `top_p` default 0.7 | HTTP/vendor codes; parse failure; empty response; timeout | `core/provider/BlueLMProvider.kt`, `VendorIo.kt`, `ProviderAskChatClient.kt` | `live_ready` |
| Function calling | 1805 | Function calling | `pages/016-1805-Function-calling/` | Chat messages with tool/function call pattern | Same cloud model auth | `messages` plus tool schema/function result messages | Assistant tool-call style response | tool name, parameters, function result role | invalid tool, invalid args, state-change confirmation | `core/tools/InternalFunctionRouter.kt`; official seam only | `seam_only` |
| Image generation | 1732 | 鍥剧墖鐢熸垚 | `pages/002-1732-鍥剧墖鐢熸垚/` | `POST https://api-ai.vivo.com.cn/api/v1/image_generation` | `Bearer AppKey` | prompt/model/image inputs | image URLs/list/task result fields | Doubao image models, image URL/base64, sequential options | non-zero code and HTTP errors | Registry/dev-lab only | `smoke_only` |
| Video generation | 2201 | 瑙嗛鐢熸垚 | `pages/022-2201-瑙嗛鐢熸垚/` | task-flow endpoints such as submit task | `Bearer AppKey` | text/image-to-video task body | task id/result/status | Doubao video models | task failure and polling errors | Registry/dev-lab only | `smoke_only` |
| OCR | 1737 | 閫氱敤 OCR | `pages/007-1737-閫氱敤OCR/` | `POST http://api-ai.vivo.com.cn/ocr/general_recognition` with form body | `Bearer AppKey` | `requestId`, base64 `image`, `businessid`, optional position/session fields | text recognition result with error code/message | `businessid = aigc + appId`; jpg/png/bmp | `error_code`: success, OCR fail, image error; HTTP errors | `core/capture/VivoCaptureProviders.kt`; `app/capture/CaptureGateway.kt` | `live_smoke_pass` |
| Translation | 1733 | 鏂囨湰缈昏瘧 | `pages/003-1733-鏂囨湰缈昏瘧/` | `POST https://api-ai.vivo.com.cn/translation/query/self` | `Bearer AppKey` | `requestId`, `from`, `to`, text/query | translated text/code/request id | language codes, `zh-CHS` target | code table in official page | `core/translation/TranslationAssistedLearning.kt` seam | `seam_only` |
| Embedding | 1734 | 鏂囨湰鍚戦噺 | `pages/004-1734-鏂囨湰鍚戦噺/` | `POST https://api-ai.vivo.com.cn/embedding-model-api/predict/batch` | `Bearer AppKey` | `requestId`, `model_name`, `sentences` | embedding vectors | `m3e-base`, `bge-base-zh-v1.5` | code table / parser errors | `core/retrieval/RetrievalProviders.kt`; parser seam | `live_smoke_pass` |
| Text similarity | 2060 | 鏂囨湰鐩镐技搴?| `pages/017-2060-鏂囨湰鐩镐技搴?` | `POST https://api-ai.vivo.com.cn/rerank` | `Bearer AppKey` | `requestId`, `model_name`, `query`, `sentences` | scores array matching candidate order | `bge-reranker-large` | code table / empty scores | `core/capture/VivoCaptureProviders.kt`, `core/retrieval` | `live_smoke_pass` |
| Query rewrite | 2061 | 鏌ヨ鏀瑰啓 | `pages/018-2061-鏌ヨ鏀瑰啓/` | `POST`, path last segment `query_rewrite_base` | `Bearer AppKey` | official docs require `prompts` array body; smoke harness now sends the documented schema | `result` array plus `code` | query length <= official limit; history q/a fields | negative code table | `VivoQueryRewriteProvider` / smoke harness | `live_smoke_pass` |
| Short ASR | 1738 | 瀹炴椂鐭闊宠瘑鍒?| `pages/008-1738-瀹炴椂鐭闊宠瘑鍒?` | WebSocket short speech | `Bearer AppKey` | URL params + start frame + audio frames | `started`, `result`, `error` events | `engineid=shortasrinput`, `asr_info.audio_type` | websocket code table | Registry/dev-lab only | `smoke_only` |
| Long ASR dictation | 1740 | 闀胯闊冲惉鍐?| `pages/010-1740-闀胯闊冲惉鍐?` | WebSocket long dictation | `Bearer AppKey` | URL params + audio frames | streaming result events | `engineid=longasrlisten` | websocket code table | Secondary ASR reference | `smoke_only` |
| Long ASR transcription | 1739 | 闀胯闊宠浆鍐?| `pages/009-1739-闀胯闊宠浆鍐?` | HTTP task flow: `/lasr/create`, `/lasr/upload`, `/lasr/run`, `/lasr/progress`, `/lasr/result` | `Bearer AppKey` | create audio, multipart upload, run/progress/result | audio id, progress, transcript result | `engineid=fileasrrecorder`, `x-sessionId`, slices | task code table and polling failures | `VivoAsrProvider` in capture providers | `provider_ready`, needs non-sensitive audio smoke |
| Dialect ASR | 2065 | 鏂硅█鑷敱璇?| `pages/020-2065-鏂硅█鑷敱璇?` | WebSocket speech | `Bearer AppKey` | engine/model/audio params | speech result events | dialect engine ids | websocket code table | Registry/dev-lab only | `smoke_only` |
| Simultaneous interpretation | 2068 | 鍚屽０闊充紶璇?| `pages/021-2068-鍚屽０闊充紶璇?` | WebSocket ASR/translation/TTS-style stream | `Bearer AppKey` | engine/audio/target params | result/error events | `longasrsubtitle`, TTS params | websocket code table | Registry/dev-lab only | `smoke_only` |
| TTS / audio generation | 1735 | 闊抽鐢熸垚 | `pages/005-1735-闊抽鐢熸垚/` | `wss://api-ai.vivo.com.cn/tts?...` | `Bearer AppKey` | URL params then text payload | audio chunks/status/error | short/long synthesis engine ids | websocket error codes | `core/audio/CourseEssenceAudio.kt` script/TTS seam | `seam_only` |
| On-device 3B | 1802 | 绔晶3B妯″瀷 | `pages/013-1802-绔晶3B澶фā鍨?` | Android SDK/AAR local model | local SDK and model files; AAR not tracked | `modelPath`, text prompt, multimodal prompt after VIT | callback text/errors | `temperature`, `topP`, tokenizer/model path | SDK error codes | on-device bridge/app integration | `live_ready` on supported device, not CI-complete |
| On-device text safety | 1804 | 绔晶鏂囨湰瀹℃牳 | `pages/015-1804-绔晶鏂囨湰瀹℃牳/` | Android on-device safety SDK | local SDK | init + text safety call | code/result/risk response | SDK init/result code | SDK error tables | `core/safety/TextSafety.kt` seam | `seam_only` |
| On-device capability files | 1803 | 绔晶鑳藉姏鐩稿叧鏂囦欢 | `pages/014-1803-绔晶鑳藉姏鐩稿叧鏂囦欢/` | file/package reference | local artifacts only | model/AAR readiness | checklist/status | model path, device condition, artifact policy | missing file/version mismatch | docs/scripts/checklists | `provider_ready` reference |

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

Historical: the previous smoke path did not expose a request timeout parameter. Smoke v4 added `-TimeoutSeconds` with default 20 seconds and classified timeouts as `FAIL_TIMEOUT`. Later live Query Rewrite blocking was traced by Claude to a request-body schema mismatch, not endpoint/method/content-type. The fixed harness now uses the official docId 2061 `prompts` payload and Query Rewrite real network smoke is `PASS`.

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

Current sanitized status after official provider schema configuration and real smoke validation:

| Capability | Endpoint mapping | Auth mapping | Request schema | Mapping source | Live smoke | Request sent in explain mode |
|---|---|---|---|---|---|---:|
| OCR | READY | READY | READY | LOCAL_CONFIG_OFFICIAL_PROVIDER | PASS | false |
| QUERY_REWRITE | READY | READY | READY | LOCAL_CONFIG_OFFICIAL_PROVIDER | PASS | false |
| TEXT_SIMILARITY | READY | READY | READY | LOCAL_CONFIG_OFFICIAL_PROVIDER | PASS | false |
| EMBEDDING | READY | READY | READY | LOCAL_CONFIG_OFFICIAL_PROVIDER | PASS | false |
| TRANSLATION | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | not run | false |
| TTS | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | not run | false |
| FUNCTION_CALLING | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | not run | false |

No key, auth value, full endpoint, or `config.local.json` value is recorded in this report.

## Follow-up Work

- Move next to App-level L3 cloud-device end-to-end validation.
- Do not expand new feature scope before device validation; product fallback remains qwen3.5-plus rewrite, local safe rewrite, or direct retrieval.
- Keep qwen on `qwen3.5-plus`; do not switch to doubao.
- Keep Translation, TTS, Function Calling, and ASR Long as seam-only/deferred or separate validation items until L3 device findings require them.
- Do not expand new feature scope before device validation. Optimization should be driven by App-level L3 acceptance blockers, warnings, and polish findings.
- If a deployed endpoint rejects thinking fields, use the compatibility feature flags to omit unsupported fields for that endpoint while keeping `DEEP_STUDY` as the default supported profile.
