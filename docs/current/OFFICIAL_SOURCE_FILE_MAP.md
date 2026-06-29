# Official Source File Map

Version: `1.14.2 / versionCode 115`

This map lists the local files that future Claude/Codex development should inspect before changing official vivo/BlueLM capability behavior. It excludes private config and raw OfficialDemos content.

## Current Documentation Entry Points

| File | Role | Capability | Current entry | Notes |
| --- | --- | --- | --- | --- |
| `docs/current/OFFICIAL_INTERFACE_REFERENCE_INDEX.md` | Main official interface index | All official capabilities | Yes | Start here for source and status lookup |
| `docs/current/CLAUDE_OFFICIAL_INTERFACE_LOOKUP_GUIDE.md` | Claude-oriented lookup guide | All official capabilities | Yes | Fast route for future implementation work |
| `docs/current/OFFICIAL_PROTOCOL_QUICK_REFERENCE.md` | Protocol field quick reference | TTS, ASR, BlueLM, OCR, retrieval | Yes | Short protocol facts, no secrets |
| `docs/current/OFFICIAL_INTERFACE_GAPS_AND_BOUNDARIES.md` | Claim boundary document | All official capabilities | Yes | Prevents overclaiming |
| `docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md` | Product capability matrix | Official/product status | Yes | User/product-facing status |
| `docs/current/official_capability_evidence_matrix_v2.md` | Evidence matrix | Official evidence mapping | Yes | Current matrix aligned to 1.14.2 |
| `docs/current/official_tool_matrix.md` | Tool matrix | Official tool surfaces | Legacy/current bridge | Keep aligned with current matrix |
| `docs/current/official_tool_productization_matrix.md` | Productization matrix | Official tool productization | Legacy/current bridge | Keep aligned with current matrix |
| `docs/current/official_docs_strict_alignment_report.md` | Strict alignment report | Official docs vs implementation | Historical/current reference | Useful for protocol origins |
| `docs/current/official_provider_network_smoke_run.md` | Smoke run record | OCR/retrieval provider smoke | Historical evidence | Smoke pass is not app runtime validation |
| `docs/current/asr_long_productization_report.md` | ASR long report | Long ASR | Historical/current mapping | Clarifies core contract and app fallback |
| `docs/current/tts_translation_function_calling_report.md` | TTS/translation/FC report | TTS and seams | Historical/current mapping | Superseded for TTS protocol by 1.14.2 docs |
| `docs/current/bluelm_cloud_realdevice_troubleshooting.md` | Troubleshooting | BlueLM cloud | Yes | Real-device readiness/error taxonomy |

## Official WebSocket Code

| File | Role | Capability | Current entry | Notes |
| --- | --- | --- | --- | --- |
| `core/src/main/kotlin/com/classmate/core/official/ws/OfficialTtsWsProtocol.kt` | TTS URL/header/params/request/response parser | Official TTS WebSocket | Yes | Primary TTS protocol source |
| `core/src/main/kotlin/com/classmate/core/official/ws/OfficialTtsWsSession.kt` | TTS session orchestration | Official TTS WebSocket | Yes | Handles synthesis session |
| `core/src/main/kotlin/com/classmate/core/official/ws/PcmWavWriter.kt` | PCM to WAV writing | Official TTS output | Yes | Audio container writer |
| `core/src/main/kotlin/com/classmate/core/official/ws/OfficialAsrWsProtocol.kt` | ASR URL/header/frame/result parser | Official realtime ASR | Yes | Primary ASR WebSocket protocol source |
| `core/src/main/kotlin/com/classmate/core/official/ws/OfficialRealtimeAsrSession.kt` | ASR streaming session | Official realtime ASR | Yes | Open/start/feed/stop/cancel state |
| `core/src/main/kotlin/com/classmate/core/official/ws/OfficialWsTransport.kt` | WebSocket transport boundary | ASR/TTS | Yes | Keep this abstraction clean |
| `app/src/main/java/com/classmate/app/asr/OkHttpOfficialWsTransport.kt` | OkHttp transport implementation | ASR/TTS | Yes | App-level network transport |
| `app/src/main/java/com/classmate/app/asr/PcmAudioCapture.kt` | PCM microphone capture | Realtime ASR | Yes | Device permission/audio risk area |
| `app/src/main/java/com/classmate/app/asr/OfficialTtsProvider.kt` | App TTS provider | Official TTS + fallback | Yes | Uses official session and safe fallback |

## ASR, System Speech, and Audio Evidence

| File | Role | Capability | Current entry | Notes |
| --- | --- | --- | --- | --- |
| `app/src/main/java/com/classmate/app/asr/AndroidSpeechRecognizerClient.kt` | Android system recognizer client | System ASR fallback | Yes | Not vivo official ASR |
| `app/src/main/java/com/classmate/app/asr/SpeechRecognizerEngine.kt` | System recognizer engine wrapper | System ASR fallback | Yes | Fallback engine |
| `app/src/main/java/com/classmate/app/asr/SpeechRecognitionDiagnostics.kt` | Readiness diagnostics | ASR readiness | Yes | Developer/user-safe readiness |
| `app/src/main/java/com/classmate/app/asr/AsrSession.kt` | ASR session state | ASR fallback flow | Yes | Do not fake transcript evidence |
| `app/src/main/java/com/classmate/app/l3/ClassroomAudioRecorder.kt` | Recording file creation | Audio evidence | Yes | Recording saved fallback |
| `app/src/main/java/com/classmate/app/l3/RecordingFileManager.kt` | Recording lifecycle helper | Audio evidence | Yes | Cleanup/delete/exists checks |
| `app/src/main/java/com/classmate/app/asr/AsrTranscriptMapper.kt` | Transcript mapping | ASR/evidence | Yes | Keeps transcript segments structured |

## BlueLM Cloud and Provider Code

| File | Role | Capability | Current entry | Notes |
| --- | --- | --- | --- | --- |
| `core/src/main/kotlin/com/classmate/core/provider/BlueLMProvider.kt` | BlueLM HTTP provider | BlueLM cloud | Yes | Primary cloud large-model protocol source |
| `core/src/main/kotlin/com/classmate/core/provider/BlueLmSigner.kt` | Request signing/auth helper | BlueLM cloud | Yes | Do not log auth values |
| `core/src/main/kotlin/com/classmate/core/provider/BlueLMDiagnostic.kt` | Diagnostic model | BlueLM cloud | Yes | Safe readiness/error classification |
| `core/src/main/kotlin/com/classmate/core/provider/BlueLmConfigDoctor.kt` | Config doctor | BlueLM cloud | Yes | Does not require exposing secrets |
| `app/src/main/java/com/classmate/app/data/BlueLMHttpTransport.kt` | App HTTP transport | BlueLM cloud | Yes | App network boundary |
| `app/src/main/java/com/classmate/app/data/ModelConfigRepository.kt` | Model config storage | BlueLM/official config | Yes | Must preserve AppKey when fields are empty |
| `app/src/main/java/com/classmate/app/data/ConfigRepository.kt` | Config repository | BlueLM/official config | Yes | Do not read private local config in docs tasks |
| `app/src/main/java/com/classmate/app/AppViewModel.kt` | Learning flow orchestration | BlueLM/fallback/product flow | Yes | Main app route, large file |

## OCR, Capture, and Retrieval

| File | Role | Capability | Current entry | Notes |
| --- | --- | --- | --- | --- |
| `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt` | OCR, ASR long, retrieval provider implementations | OCR/ASR/retrieval | Yes | Contains several provider contracts |
| `app/src/main/java/com/classmate/app/capture/CaptureGateway.kt` | Capture gateway | OCR/image capture | Yes | App capture boundary |
| `app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt` | Import UI wiring | OCR/manual fallback | Yes | Minimal UI route only |
| `app/src/main/java/com/classmate/app/importing/OcrImport.kt` | OCR draft model and assembler | OCR import | Yes | Multi-image OCR model |
| `core/src/main/kotlin/com/classmate/core/ocr/OcrTextPostProcessor.kt` | OCR cleanup and quality assessment | OCR | Yes | Must not alter Chinese punctuation unexpectedly |
| `core/src/main/kotlin/com/classmate/core/retrieval/VivoRetrievalProviderAdapters.kt` | Query rewrite, embedding, similarity adapters | Retrieval official runtime | Yes | Bridges capture providers to L3 gateway |
| `app/src/main/java/com/classmate/app/l3/OfficialRuntimeGateway.kt` | Official runtime gateway interface | Retrieval/runtime provenance | Yes | Main L3 official gateway contract |
| `app/src/main/java/com/classmate/app/l3/OfficialRuntimeGatewayFactory.kt` | Production gateway factory | Retrieval runtime injection | Yes | Should not default to config-missing only |
| `app/src/main/java/com/classmate/app/l3/OfficialRuntimeIntegrator.kt` | Runtime contribution integration | L3 official provenance | Yes | Learning loop contribution |

## On-Device and Safety

| File | Role | Capability | Current entry | Notes |
| --- | --- | --- | --- | --- |
| `app/src/main/java/com/classmate/app/ondevice/VivoSdkReflection.kt` | Reflection wrapper | On-device 3B optional SDK | Yes | Avoid direct SDK import dependency |
| `app/src/main/java/com/classmate/app/ondevice/RealVivoOnDeviceLlmBridge.kt` | App bridge | On-device fallback | Yes | Device/resource dependent |
| `core/src/main/kotlin/com/classmate/core/ondevice/MissingOnDeviceBlueLmBridge.kt` | Missing bridge fallback | On-device fallback | Yes | Honest unavailable route |
| `core/src/main/kotlin/com/classmate/core/ai/AiCapabilityRouter.kt` | Route selection | Cloud/on-device/local | Yes | Keeps fallback hierarchy |
| `docs/architecture/stage8b_local_text_moderation_plan.md` | Text moderation plan | Content safety AAR | Reference only | Not productized in 1.14.2 |

## Tests to Check Before Changing Claims

| Test file | Covers |
| --- | --- |
| `core/src/test/kotlin/com/classmate/core/official/ws/OfficialTtsWsProtocolTest.kt` | TTS protocol fields and parsing |
| `core/src/test/kotlin/com/classmate/core/official/ws/OfficialTtsWsSessionTest.kt` | TTS session behavior |
| `app/src/test/java/com/classmate/app/asr/OfficialTtsProviderTest.kt` | App TTS fallback safety |
| `core/src/test/kotlin/com/classmate/core/official/ws/OfficialAsrWsProtocolTest.kt` | ASR WebSocket protocol |
| `core/src/test/kotlin/com/classmate/core/official/ws/OfficialRealtimeAsrSessionTest.kt` | ASR WebSocket session |
| `app/src/test/java/com/classmate/app/asr/SpeechRecognitionDiagnosticsTest.kt` | System ASR readiness |
| `app/src/test/java/com/classmate/app/state/AsrUnavailableFallbackTest.kt` | ASR fallback app flow |
| `app/src/test/java/com/classmate/app/state/OcrFallbackFlowTest.kt` | OCR manual fallback |
| `app/src/test/java/com/classmate/app/importing/OcrImportAssemblerTest.kt` | Multi-image OCR merge/status |
| `core/src/test/kotlin/com/classmate/core/ocr/OcrTextPostProcessorTest.kt` | OCR cleanup and quality |
| `app/src/test/java/com/classmate/app/l3/OfficialRuntimeWiringTest.kt` | Official retrieval gateway wiring |
| `app/src/test/java/com/classmate/app/qa/OfficialDocsStrictAlignmentReportTest.kt` | Official docs alignment guard |

## QA Scripts

| File | Role |
| --- | --- |
| `scripts/qa/current_preflight.ps1` | Current full project preflight |
| `scripts/qa/cloud_device_precheck.ps1` | Cloud/real-device precheck |
| `scripts/qa/stage6_text_audit.ps1` | Text/copy audit |
| `scripts/qa/stage7_sensitive_text_audit.ps1` | Sensitive text audit |
