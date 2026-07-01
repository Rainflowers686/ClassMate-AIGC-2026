# ClassMate Official Interface Reference Index

Current update: `1.14.7 / versionCode 120`

1.14.7 adds a verifiable provider diagnostics layer:

- `OfficialProviderDiagnostics` maps BlueLM and official provider readiness into safe categories: `SUCCESS`, `SKIP_MISSING_CONFIG`, `AUTH_FAILED`, `NETWORK_FAILED`, `TIMEOUT`, `BAD_REQUEST`, `SERVER_ERROR`, `EMPTY_RESPONSE`, `PARSE_ERROR`, `READY_CONFIG_PRESENT`, `SKIPPED_NO_AUDIO`.
- Developer Settings can run an official service dry-run. BlueLM uses the same provider path as the real request with a minimal safe prompt. Official ASR/TTS/OCR report config/readiness without exposing credentials.
- `scripts/qa/provider_live_smoke.ps1` defaults to no local credential inspection and no network request. It prints SKIP when credentials are missing or not inspected; explicit `-UseLocalConfig` delegates to the existing official smoke with redacted output.
- System SpeechRecognizer is only an optional device fallback. The ClassMate ASR main route is official realtime ASR, official long-ASR after recording, then manual transcript.

Version: `1.14.2 / versionCode 115`
Latest documentation baseline: `711e35d docs(final): rewrite release docs and submission evidence for v1.14.2`
Current product baseline includes: `7473fb1 fix(product): repair final real-device import and quiz blockers`

This is the main index for local vivo/BlueLM/official capability references inside the ClassMate repository. It is designed so future Claude/Codex work can find protocol notes, implementation files, tests, gaps, and claim boundaries without repeatedly searching the whole repo.

## 1. How to Use This Index

1. For quick implementation work, start with `docs/current/CLAUDE_OFFICIAL_INTERFACE_LOOKUP_GUIDE.md`.
2. For exact protocol fields, use `docs/current/OFFICIAL_PROTOCOL_QUICK_REFERENCE.md`.
3. For source files and tests, use `docs/current/OFFICIAL_SOURCE_FILE_MAP.md`.
4. For what cannot be claimed, use `docs/current/OFFICIAL_INTERFACE_GAPS_AND_BOUNDARIES.md`.
5. Do not read or quote private local config. Do not copy AppKey, full auth values, or private endpoint overrides into docs.

## 2. Search Result Inventory

The local scan covered markdown/text/config-like documentation and Kotlin source under `docs`, `scripts`, `core`, and `app`, excluding `.git`, build folders, `.gradle`, `.codex_work`, `OfficialDemos`, and `config.local.json`. The scan found 263 documentation/config-like files and the following high-value official-interface locations.

| File path | Capability hit | Content type | Current validity | Claude lookup entry | Handling |
| --- | --- | --- | --- | --- | --- |
| `docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md` | Official capability status | Current matrix | Current | Yes | Link from this index |
| `docs/current/official_docs_strict_alignment_report.md` | Official docs alignment | Protocol/status report | Current/historical | Yes | Keep as evidence, do not treat as live validation |
| `docs/current/official_provider_network_smoke_run.md` | OCR/retrieval smoke | Historical smoke report | Historical evidence | Yes | Smoke pass does not equal app runtime success |
| `docs/current/asr_long_productization_report.md` | Long ASR | Historical/current report | Partially current | Yes | Use with current ASR quick reference |
| `docs/current/tts_translation_function_calling_report.md` | TTS/translation/FC | Historical report | Partially superseded | Yes | TTS protocol superseded by current WS docs |
| `docs/current/bluelm_cloud_realdevice_troubleshooting.md` | BlueLM | Troubleshooting | Current | Yes | Use for readiness and error taxonomy |
| `docs/issues/07_vivo_ocr_integration.md` | OCR | Historical issue | Reference only | Yes | Use as research record |
| `docs/issues/02_vivo_asr_integration.md` | ASR | Historical issue | Reference only | Yes | Use as research record |
| `docs/architecture/stage8_ondevice_bluelm_architecture.md` | On-device 3B | Architecture | Current boundary reference | Yes | Use for optional/reflection model framing |
| `docs/architecture/stage8b_local_text_moderation_plan.md` | Text moderation AAR | Architecture plan | Deferred | Yes | Not productized in 1.14.2 |
| `core/src/main/kotlin/com/classmate/core/official/ws/*.kt` | TTS/ASR WebSocket | Protocol code | Current | Yes | Primary WS protocol source |
| `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt` | OCR, long ASR, retrieval | Provider code | Current | Yes | Multi-provider source |
| `core/src/main/kotlin/com/classmate/core/provider/BlueLMProvider.kt` | BlueLM cloud | Provider code | Current | Yes | Primary BlueLM protocol source |
| `core/src/main/kotlin/com/classmate/core/retrieval/VivoRetrievalProviderAdapters.kt` | Query rewrite/embedding/similarity | Adapter code | Current | Yes | Runtime bridge to L3 |
| `app/src/main/java/com/classmate/app/asr/*.kt` | ASR/TTS/system fallback | App runtime code | Current | Yes | App-level official and system fallback |
| `app/src/main/java/com/classmate/app/ondevice/*.kt` | On-device model | Reflection bridge | Current | Yes | Optional fallback path |
| `app/src/test/**`, `core/src/test/**` | Official guards | Tests | Current | Yes | Validate before claims |

## 3. Official Capability Matrix

| Capability | Key local docs | Key protocol/code | Current implementation | Fallback | True-device status | Claude first reads |
| --- | --- | --- | --- | --- | --- | --- |
| BlueLM cloud model | `bluelm_cloud_realdevice_troubleshooting.md`, `official_docs_strict_alignment_report.md` | `BlueLMProvider.kt`, `BlueLmSigner.kt`, `BlueLMHttpTransport.kt` | HTTP provider and learning flow integration | Optional on-device, then local rule analysis | Config and network validation required per device | `BlueLMProvider.kt`, `AppViewModel.kt` |
| On-device 3B | `stage8_ondevice_bluelm_architecture.md`, `stage8a2_multimodal_bridge_design.md` | `VivoSdkReflection.kt`, `RealVivoOnDeviceLlmBridge.kt`, `AiCapabilityRouter.kt` | Optional reflection fallback | Cloud or local rules | Device/resource dependent | `VivoSdkReflection.kt` |
| Long ASR 1739 HTTP | `asr_long_productization_report.md`, `official_docs_strict_alignment_report.md` | `VivoAsrProvider` in `VivoCaptureProviders.kt` | create/upload/run/progress/result task flow in code | System ASR, recording, manual transcript | Needs AppKey/audio upload validation | `VivoCaptureProviders.kt` |
| Realtime ASR WebSocket | `OFFICIAL_PROTOCOL_QUICK_REFERENCE.md` | `OfficialAsrWsProtocol.kt`, `OfficialRealtimeAsrSession.kt`, `OkHttpOfficialWsTransport.kt` | WebSocket base and app transport | System ASR, recording, manual transcript | Needs AppKey/device validation | `OfficialAsrWsProtocol.kt` |
| Official TTS WebSocket | `OFFICIAL_PROTOCOL_QUICK_REFERENCE.md` | `OfficialTtsWsProtocol.kt`, `OfficialTtsWsSession.kt`, `OfficialTtsProvider.kt` | Protocol/session/provider code path exists | System TTS, script only | Needs AppKey/audio validation | `OfficialTtsWsProtocol.kt` |
| System SpeechRecognizer | `REAL_DEVICE_TEST_MANUAL_1_14_2.md`, ASR tests | `AndroidSpeechRecognizerClient.kt`, `SpeechRecognitionDiagnostics.kt` | System fallback integrated | Recording/manual transcript | Device service dependent | `SpeechRecognitionDiagnostics.kt` |
| System TextToSpeech | `OFFICIAL_CAPABILITY_MATRIX_1_14_2.md` | `AndroidLocalTtsPlayer.kt`, `LocalTtsPlayer.kt` | System fallback integrated | Script only | Device engine dependent | `AndroidLocalTtsPlayer.kt` |
| OCR | `official_docs_strict_alignment_report.md`, `07_vivo_ocr_integration.md` | `VivoOcrProvider`, `CaptureGateway.kt`, `OcrImport.kt` | Provider code plus manual fallback product flow | Manual image text entry | Config/image validation required | `OcrImport.kt`, `VivoCaptureProviders.kt` |
| Query rewrite | `official_provider_network_smoke_run.md`, `official_runtime_injection_v1_7.md` | `VivoRetrievalProviderAdapters.kt`, `OfficialRuntimeGatewayFactory.kt` | Official-first adapter exists | Local query planning | Config validation required | `OfficialRuntimeGatewayFactory.kt` |
| Embedding | Same as query rewrite | Same retrieval adapter set | Official vector support plus semantic index | Local lexical vector | Config validation required | `VivoRetrievalProviderAdapters.kt` |
| Text similarity | Same as query rewrite | Same retrieval adapter set | Official score adapter plus ranking fallback | Local similarity | Config validation required | `VivoRetrievalProviderAdapters.kt` |
| Text moderation AAR | `stage8b_local_text_moderation_plan.md` | No stable product runtime in 1.14.2 | Deferred | Local/export safety guards | Not integrated | Plan doc only |
| Dialect/interpreting | ASR protocol comments, capability docs | Experimental ASR engine entries | Not productized in stable flow | Standard ASR/manual transcript | Not validated | `OfficialAsrWsProtocol.kt` |
| External search | Product docs | Browser intent route | Browser search only | None/API absent | Locally testable | UI/search code, not official API |

## 4. Capability Details

### A. BlueLM Cloud Model

- Official source location: `docs/current/official_docs_strict_alignment_report.md`, `docs/current/bluelm_cloud_realdevice_troubleshooting.md`, `docs/decisions/0001-bluelm-first.md`.
- Key protocol: POST to chat completions endpoint with `Authorization` bearer value pattern and `app_id` request header.
- Request: OpenAI-compatible chat body with model, messages, generation controls, and stream flag.
- Response: first assistant message content parsed from choices.
- Success: content parsed and returned through provider result.
- Failure: classified into config, timeout, network, HTTP, or parse categories; raw exception text is not user copy.
- Current state: learning analysis, enhancement, and polished export routes can use BlueLM when configured.
- Fallback: optional on-device model, then local rule analysis.
- Tests: `BlueLMProviderTest`, `BlueLMDiagnosticRunnerTest`, `BlueLmConfigDoctorTest`, product state tests.
- Boundary: local fallback is not BlueLM output.

### B. On-Device Model / 3B SDK

- Official/local source location: `docs/architecture/stage8_ondevice_bluelm_architecture.md`, `docs/architecture/stage8a2_multimodal_bridge_design.md`, `docs/testing/stage8a2_ondevice_sdk_build_record.md`.
- Protocol shape: local SDK/AAR reflection, not HTTP.
- Current state: optional reflection bridge and missing bridge fallback.
- Auth: no cloud AppKey for local SDK route, but device model files/resources are required.
- Fallback: cloud or local rules.
- Tests: `VivoSdkReflectionTest`, `RealVivoOnDeviceLlmBridgeTest`, `AiCapabilityRouterTest`.
- Boundary: do not claim all phones have on-device 3B capability.

### C. Official Long ASR / 1739 HTTP Task Flow

- Official source location: `docs/current/asr_long_productization_report.md`, `docs/current/official_docs_strict_alignment_report.md`.
- Code: `VivoAsrProvider` in `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt`.
- Flow: create -> upload slices -> run -> progress polling -> result.
- Success: transcript segments parsed from `onebest`, `bg`, `ed`, and `speaker`.
- Failure: config missing, invalid audio, audio too long, service unavailable, timeout, or parse failure.
- Fallback: realtime/system ASR, recording saved, manual transcript.
- Boundary: core task flow exists; live task success requires configured service validation.

### D. Official Realtime ASR WebSocket

- Official source location: `OfficialAsrWsProtocol.kt` and quick reference.
- Endpoint family: `/asr/v2` WebSocket.
- Request: auth header, URL params, JSON start frame, PCM frames.
- Response: result/error/VAD events.
- Success: partial/final text event returned by session.
- Fallback: system SpeechRecognizer, recording saved, manual transcript.
- Tests: `OfficialAsrWsProtocolTest`, `OfficialRealtimeAsrSessionTest`, `SpeechRecognitionDiagnosticsTest`.
- Boundary: WebSocket base is integrated; complete real-device live stream remains validation pending.

### E. Official TTS WebSocket

- Official source location: user-supplied official protocol now captured in `OfficialTtsWsProtocol.kt` and quick reference.
- Endpoint: `wss://api-ai.vivo.com.cn/tts`.
- Request: GET WebSocket upgrade with auth/signature headers and URL params; JSON synthesis request with base64 UTF-8 text.
- Response: error fields, session id, progress, audio chunks, slice index; status 2 ends synthesis.
- Success: PCM chunks are collected and written to WAV.
- Fallback: Android system TTS, then script only.
- Tests: `OfficialTtsWsProtocolTest`, `OfficialTtsWsSessionTest`, `OfficialTtsProviderTest`.
- Boundary: protocol is no longer missing; live success still needs AppKey and real-device validation.

### F. Android System SpeechRecognizer

- Source: `AndroidSpeechRecognizerClient.kt`, `SpeechRecognizerEngine.kt`, `SpeechRecognitionDiagnostics.kt`.
- Role: system fallback when official ASR is unavailable or not configured.
- Success: system transcript enters transcript editing/learning flow.
- Failure: user can continue recording and manually paste transcript.
- Boundary: not vivo official ASR.

### G. Android System TextToSpeech

- Source: `AndroidLocalTtsPlayer.kt`, `LocalTtsPlayer.kt`, `OfficialTtsProvider.kt`.
- Role: fallback for review listening and scripts when official TTS is unavailable.
- Boundary: not vivo official TTS; generated script remains available when no system engine exists.

### H. OCR / Image Text Recognition

- Source: `VivoOcrProvider` in `VivoCaptureProviders.kt`, OCR docs/issues, `OcrImport.kt`.
- Request shape in code: base64 image form request, business id, request id, and position parameter.
- Current product flow: image is retained as material/evidence; OCR text can be edited; failed images do not block a batch.
- Fallback: manual image text entry.
- Boundary: if a full local official OCR protocol is not confirmed, do not invent fields.

### I. Text Moderation AAR / Content Safety

- Source: `docs/architecture/stage8b_local_text_moderation_plan.md`.
- Current state: deferred/not productized in 1.14.2.
- Fallback: safe export text, no raw ids, no provider trace, user copy guards.
- Boundary: do not list as a completed runtime.

### J. Dialect / Simultaneous Interpretation / Other Speech Capabilities

- Source: ASR protocol engine comments and historical capability docs.
- Current state: experimental or deferred; not stable product route.
- Fallback: standard ASR, recording, manual transcript.
- Boundary: do not expose fake realtime interpretation claims.

### K. External Search

- Source: product docs and app search entry behavior.
- Current state: browser intent search.
- Boundary: not official API, not a crawler, not a recommendation algorithm.

## 5. Current Real-Device Validation State

| Capability | Validation state |
| --- | --- |
| OCR material fallback | Product path validated by tests; official provider still config-gated |
| BlueLM cloud | Config-gated; real device depends on AppID/AppKey and network |
| Official TTS WebSocket | Code path exists; live synthesis pending AppKey/device validation |
| Official realtime ASR | WebSocket base exists; live streaming pending AppKey/device validation |
| Long ASR HTTP | Task flow exists; upload/poll/result pending configured validation |
| System ASR/TTS | Device-dependent fallback |
| On-device 3B | Device/resource-dependent optional fallback |
| Text moderation AAR | Deferred |

## 6. Prohibited Overclaims

- Official TTS is missing protocol.
- Official TTS/ASR has been fully real-device verified.
- All vivo official capabilities are productized.
- System ASR/TTS is vivo official ASR/TTS.
- Browser search is an API search.
- On-device 3B works on all phones.
- Local fallback is BlueLM output.
- Smoke pass means app runtime success.

## 7. Future Claude Development Advice

1. Treat this index as the first stop.
2. For TTS/ASR, use the WebSocket protocol tests as the source of truth before UI changes.
3. For long ASR and OCR, inspect `VivoCaptureProviders.kt` and alignment docs together.
4. For BlueLM, preserve config safety and timeout behavior.
5. For retrieval providers, preserve local fallback and runtime provenance.
6. For any real-device official call, add a validation note and fallback route.
7. If local official docs are incomplete, write “not confirmed in local official materials” rather than guessing.
