# ClassMate Official Protocol Quick Reference

Version: `1.14.2 / versionCode 115`
Latest documentation baseline: `711e35d docs(final): rewrite release docs and submission evidence for v1.14.2`

This is the short protocol lookup sheet for future ClassMate development. It records only local repository evidence and code facts. It does not contain real keys, tokens, or private config values.

## TTS WebSocket

Current conclusion: official TTS WebSocket has enough protocol information for ClassMate development. ClassMate has a code path, but live network success still requires a valid AppKey, service permission, device audio checks, and real-device validation.

| Field | Current local reference |
| --- | --- |
| Endpoint | `wss://api-ai.vivo.com.cn/tts` |
| HTTP method during upgrade | `GET /tts` |
| Auth header | Header `Authorization` with value pattern `Bearer <AppKey>` |
| Signature header | Header `X-AI-GATEWAY-SIGNATURE` with value `developers-aigc` |
| URL params | `engineid`, `system_time`, `user_id`, `model`, `product`, `package`, `client_version`, `system_version`, `sdk_version`, `android_version`, `requestId` |
| Common engines | `short_audio_synthesis_jovi`, `long_audio_synthesis_screen`, `tts_humanoid_lam` |
| Audio | 24 kHz, 16-bit, mono PCM; ClassMate writes WAV through `PcmWavWriter` |
| Request JSON | `aue`, `auf = audio/L16;rate=24000`, `vcn`, `speed`, `volume`, base64 UTF-8 `text`, `encoding = utf8`, `reqId` |
| Response JSON | `error_code`, `error_msg`, `sid`, `data.status`, `data.progress`, `data.audio`, `data.slice` |
| End status | `data.status = 2` means synthesis finished |
| Fallback | Official TTS -> Android system TTS -> listen-review script only |
| Code | `core/src/main/kotlin/com/classmate/core/official/ws/OfficialTtsWsProtocol.kt`, `OfficialTtsWsSession.kt`, `PcmWavWriter.kt`, `app/src/main/java/com/classmate/app/asr/OfficialTtsProvider.kt` |
| Tests | `core/src/test/kotlin/com/classmate/core/official/ws/OfficialTtsWsProtocolTest.kt`, `OfficialTtsWsSessionTest.kt`, `app/src/test/java/com/classmate/app/asr/OfficialTtsProviderTest.kt` |

Do not write that official TTS is blocked by missing protocol. Do not write that official TTS is fully real-device validated. The correct product statement is: code path and fallback are present; AppKey and real-device validation are still required.

## Realtime ASR WebSocket

Current conclusion: official realtime ASR WebSocket base is present, but complete streaming UI and real-device success still require valid AppKey, permission, device audio routing, and service validation.

| Field | Current local reference |
| --- | --- |
| Endpoint family | `wss://api-ai.vivo.com.cn/asr/v2` |
| Auth header | Header `Authorization` with value pattern `Bearer <AppKey>` |
| Connection params | `client_version`, `product`, `package`, `sdk_version`, `user_id`, `android_version`, `system_time`, `net_type`, `engineid`, `requestId` |
| Engines in code | `shortasrinput` for realtime short ASR, `longasrlisten` for dictation, experimental dialect/interpreting entries |
| Frame flow | WebSocket open -> JSON `started` frame -> raw PCM binary frames -> `--end--` -> optional `--close--` |
| Start JSON | `type = started`, `request_id`, `asr_info.front_vad_time`, `end_vad_time`, `audio_type = pcm`, `chinese2digital`, `punctuation` |
| Result parsing | `action = result`, `type = asr`, `data.text`, `data.is_last`; error and VAD events are also parsed |
| Fallback | Official realtime ASR -> Android `SpeechRecognizer` -> recording saved -> manual transcript |
| Code | `core/src/main/kotlin/com/classmate/core/official/ws/OfficialAsrWsProtocol.kt`, `OfficialRealtimeAsrSession.kt`, `OfficialWsTransport.kt`, `app/src/main/java/com/classmate/app/asr/OkHttpOfficialWsTransport.kt`, `PcmAudioCapture.kt`, `AndroidSpeechRecognizerClient.kt`, `SpeechRecognitionDiagnostics.kt` |
| Tests | `core/src/test/kotlin/com/classmate/core/official/ws/OfficialAsrWsProtocolTest.kt`, `OfficialRealtimeAsrSessionTest.kt`, `app/src/test/java/com/classmate/app/asr/SpeechRecognitionDiagnosticsTest.kt`, `AsrLiveFlowTest.kt`, `AsrUnavailableFallbackTest.kt` |

System ASR is a fallback, not a vivo official runtime. If official ASR is not configured, users should still be able to record, use system recognition when available, or paste transcript text.

## Long ASR HTTP

Current conclusion: the long audio ASR HTTP task flow exists in code through `VivoAsrProvider`. It is not safe to claim live upload/poll/result success until AppKey, endpoint access, and real-device audio files are validated.

| Step | Current local reference |
| --- | --- |
| Create | `POST /lasr/create` with `audio_type`, `x-sessionId`, `slice_num`; returns `audio_id` |
| Upload | `POST /lasr/upload` with `audio_id`, `slice_index`, `x-sessionId`; multipart audio slice |
| Run | `POST /lasr/run` with `audio_id`, `x-sessionId`; returns `task_id` |
| Progress | `POST /lasr/progress` with `task_id`, `x-sessionId`; poll until progress reaches done |
| Result | `POST /lasr/result` with `task_id`, `x-sessionId`; parses `onebest`, `bg`, `ed`, `speaker` |
| Limits in code | 5 MB slices, max 100 slices, max 60 polls, 2 second poll interval |
| Fallback | Long ASR -> realtime/system ASR if suitable -> recording saved -> manual transcript |
| Code | `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt` (`VivoAsrProvider`) |
| Docs | `docs/current/asr_long_productization_report.md`, `docs/current/official_docs_strict_alignment_report.md` |

## BlueLM Cloud

Current conclusion: BlueLM cloud is the primary AI analysis route when configured. Local and on-device routes must remain honest fallback routes.

| Field | Current local reference |
| --- | --- |
| Endpoint | `https://api-ai.vivo.com.cn/v1/chat/completions` |
| Auth | Header `Authorization` with value pattern `Bearer <AppKey>` plus `app_id` request header |
| Body style | OpenAI-compatible chat request: `model`, `messages`, `stream`, `max_tokens`, `max_completion_tokens`, `temperature`, `top_p`, `reasoning_effort`, `enable_thinking` |
| Response | `choices[0].message.content` is parsed as assistant content; `reasoning_content` is ignored for normal UI/export |
| Request id behavior | Code supports documented request id naming and retry compatibility |
| qwen thinking | Fast: `reasoning_effort=low`, `enable_thinking=false`; Balanced: `medium`, `false`; Professional/Max UI: API `high`, `true` |
| Timeout | Formal requests use long profiles: Fast about 5 min, Balanced about 6 min, Professional about 10 min; dry-run/smoke stays 15-30s |
| Fallback | BlueLM -> optional on-device model -> local rule analysis |
| Code | `core/src/main/kotlin/com/classmate/core/provider/BlueLMProvider.kt`, `BlueLmSigner.kt`, `BlueLMDiagnostic.kt`, `BlueLmConfigDoctor.kt`, `app/src/main/java/com/classmate/app/data/BlueLMHttpTransport.kt` |
| Related app code | `PromptBuilder`, `PolishedStudyPackPromptBuilder`, `PolishedExportPlan`, `AppViewModel`, `ModelConfigRepository`, `ConfigRepository` |

Do not label local output as BlueLM output. Do not treat configuration absence as a student-facing failure.

## OCR

Current conclusion: OCR provider code and historical smoke evidence exist, but the local official protocol set should still be treated carefully. Do not invent a new OCR endpoint if a document is missing.

| Field | Current local reference |
| --- | --- |
| Known provider code | `VivoOcrProvider` in `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt` |
| Request shape in code | Form request with base64 image, position parameter, business id, request id |
| Known historical docs | `docs/current/official_docs_strict_alignment_report.md`, `docs/issues/07_vivo_ocr_integration.md`, `docs/architecture/asr_ocr_lesson_fusion_plan.md` |
| App behavior | Image enters material basket even when OCR fails; failed segments can be manually edited; empty OCR does not create fake knowledge; one image failure does not block other images |
| App code | `app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt`, `app/src/main/java/com/classmate/app/AppViewModel.kt`, `app/src/main/java/com/classmate/app/importing/OcrImport.kt` |
| Tests | `app/src/test/java/com/classmate/app/state/OcrFallbackFlowTest.kt`, `app/src/test/java/com/classmate/app/importing/OcrImportAssemblerTest.kt`, `core/src/test/kotlin/com/classmate/core/ocr/OcrTextPostProcessorTest.kt` |

## Retrieval Providers

Current conclusion: query rewrite, embedding, and text similarity have app runtime adapters. True official runtime use still requires valid config and real-device/network validation.

| Capability | Code | Product effect | Fallback |
| --- | --- | --- | --- |
| Query rewrite | `core/src/main/kotlin/com/classmate/core/retrieval/VivoRetrievalProviderAdapters.kt`, `app/src/main/java/com/classmate/app/l3/OfficialRuntimeGatewayFactory.kt` | Better evidence search and question planning | Local query planning |
| Embedding | Same adapter set plus `LocalSemanticIndexRepository` | Semantic index and evidence/question retrieval | Local lexical vector |
| Text similarity | Same adapter set | Evidence ranking and similar question matching | Local similarity |

## System Fallback

| Capability | Code | Boundary |
| --- | --- | --- |
| Android SpeechRecognizer | `AndroidSpeechRecognizerClient.kt`, `SpeechRecognizerEngine.kt`, `SpeechRecognitionDiagnostics.kt` | System fallback only; not vivo official ASR |
| Android TextToSpeech | `AndroidLocalTtsPlayer.kt`, `LocalTtsPlayer.kt` | System fallback only; not vivo official TTS |
| Recording saved | `ClassroomAudioRecorder.kt`, `RecordingFileManager.kt` | Evidence and manual transcript route; not automatic ASR success |
| Browser search | App intent route | Browser search only; not a recommendation API |
