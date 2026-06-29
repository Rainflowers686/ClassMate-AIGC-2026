# Claude Official Interface Lookup Guide

Version: `1.14.2 / versionCode 115`

Read this first before changing any official vivo/BlueLM capability in ClassMate. It is deliberately direct: find the capability, open the source files, keep fallback behavior, and do not overclaim real-device status.

## If You Need to Change TTS

1. Start with `docs/current/OFFICIAL_PROTOCOL_QUICK_REFERENCE.md#tts-websocket`.
2. Open:
   - `core/src/main/kotlin/com/classmate/core/official/ws/OfficialTtsWsProtocol.kt`
   - `core/src/main/kotlin/com/classmate/core/official/ws/OfficialTtsWsSession.kt`
   - `core/src/main/kotlin/com/classmate/core/official/ws/PcmWavWriter.kt`
   - `app/src/main/java/com/classmate/app/asr/OfficialTtsProvider.kt`
   - `app/src/main/java/com/classmate/app/AppViewModel.kt`
3. Run or inspect:
   - `core/src/test/kotlin/com/classmate/core/official/ws/OfficialTtsWsProtocolTest.kt`
   - `core/src/test/kotlin/com/classmate/core/official/ws/OfficialTtsWsSessionTest.kt`
   - `app/src/test/java/com/classmate/app/asr/OfficialTtsProviderTest.kt`

Correct statement: official TTS WebSocket is code-integrated, has system TTS/script fallback, and still needs AppKey plus real-device validation.

Do not say official TTS is blocked by missing protocol. Do not say it is fully validated on real devices.

## If You Need to Change ASR

Separate three routes:

1. Official realtime ASR WebSocket.
2. Official long ASR HTTP task flow.
3. Android system ASR plus recording/manual transcript fallback.

Open:

- `core/src/main/kotlin/com/classmate/core/official/ws/OfficialAsrWsProtocol.kt`
- `core/src/main/kotlin/com/classmate/core/official/ws/OfficialRealtimeAsrSession.kt`
- `core/src/main/kotlin/com/classmate/core/official/ws/OfficialWsTransport.kt`
- `app/src/main/java/com/classmate/app/asr/OkHttpOfficialWsTransport.kt`
- `app/src/main/java/com/classmate/app/asr/PcmAudioCapture.kt`
- `app/src/main/java/com/classmate/app/asr/AndroidSpeechRecognizerClient.kt`
- `app/src/main/java/com/classmate/app/asr/SpeechRecognitionDiagnostics.kt`
- `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt`
- `app/src/main/java/com/classmate/app/AppViewModel.kt`

Correct statement: official realtime ASR base and long ASR provider task flow exist in code. Live success still needs AppKey, permissions, device audio, endpoint access, and real-device validation. System ASR is fallback, not vivo official ASR.

## If You Need to Change OCR

Open:

- `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt`
- `app/src/main/java/com/classmate/app/capture/CaptureGateway.kt`
- `app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt`
- `app/src/main/java/com/classmate/app/importing/OcrImport.kt`
- `core/src/main/kotlin/com/classmate/core/ocr/OcrTextPostProcessor.kt`
- `app/src/main/java/com/classmate/app/AppViewModel.kt`

Check tests:

- `app/src/test/java/com/classmate/app/state/OcrFallbackFlowTest.kt`
- `app/src/test/java/com/classmate/app/importing/OcrImportAssemblerTest.kt`
- `core/src/test/kotlin/com/classmate/core/ocr/OcrTextPostProcessorTest.kt`

Correct statement: OCR is config-gated. When OCR fails or is unavailable, image material remains in the basket, the failed segment can be manually edited, empty OCR does not create fake knowledge, and one failed image does not block other images.

Do not invent an OCR endpoint. If a full official OCR protocol is not found locally, write that it is not confirmed in local official docs.

## If You Need to Change BlueLM

Open:

- `core/src/main/kotlin/com/classmate/core/provider/BlueLMProvider.kt`
- `core/src/main/kotlin/com/classmate/core/provider/BlueLmSigner.kt`
- `core/src/main/kotlin/com/classmate/core/provider/BlueLMDiagnostic.kt`
- `core/src/main/kotlin/com/classmate/core/provider/BlueLmConfigDoctor.kt`
- `app/src/main/java/com/classmate/app/data/BlueLMHttpTransport.kt`
- `app/src/main/java/com/classmate/app/data/ModelConfigRepository.kt`
- `app/src/main/java/com/classmate/app/data/ConfigRepository.kt`
- `app/src/main/java/com/classmate/app/AppViewModel.kt`

Also read:

- `docs/current/bluelm_cloud_realdevice_troubleshooting.md`
- `docs/decisions/0001-bluelm-first.md`
- `docs/current/official_docs_strict_alignment_report.md`

Correct statement: BlueLM is the cloud-first route when configured. Optional on-device and local rule fallback must remain honest. Deep/Max analysis and polished export have longer timeout behavior than quick learning fallback.

## If You Need to Change Retrieval

Open:

- `core/src/main/kotlin/com/classmate/core/retrieval/VivoRetrievalProviderAdapters.kt`
- `core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt`
- `app/src/main/java/com/classmate/app/l3/OfficialRuntimeGateway.kt`
- `app/src/main/java/com/classmate/app/l3/OfficialRuntimeGatewayFactory.kt`
- `app/src/main/java/com/classmate/app/l3/OfficialRuntimeIntegrator.kt`

Correct statement: query rewrite, embedding, and text similarity have official-first runtime adapters and local fallback. True official use requires config and validation.

## If You Need to Change On-Device Model Behavior

Open:

- `app/src/main/java/com/classmate/app/ondevice/VivoSdkReflection.kt`
- `app/src/main/java/com/classmate/app/ondevice/RealVivoOnDeviceLlmBridge.kt`
- `core/src/main/kotlin/com/classmate/core/ondevice/MissingOnDeviceBlueLmBridge.kt`
- `core/src/main/kotlin/com/classmate/core/ai/AiCapabilityRouter.kt`
- `docs/architecture/stage8_ondevice_bluelm_architecture.md`
- `docs/architecture/stage8a2_multimodal_bridge_design.md`

Correct statement: on-device 3B is optional and reflection/device-resource dependent. It is not available on every phone and must not be required for the stable learning loop.

## If You Need to Change Text Moderation or Safety

Open:

- `docs/architecture/stage8b_local_text_moderation_plan.md`
- `docs/current/SAFE_EXPORT_POLICY.md`
- `docs/current/PRIVACY_SECURITY_AND_SECRETS.md`

Correct statement: text moderation AAR is not productized in 1.14.2. Export and UI safety still rely on safe copy, evidence checks, and export redaction.

## If You Need to Change Export or Study Pack

Open:

- `docs/current/EXPORT_AND_POLISHED_STUDY_PACK.md`
- `docs/current/AI_STUDY_PACK_PRODUCT_SPEC.md`
- `app/src/main/java/com/classmate/app/l3/LearningExportEngine.kt`
- `core/src/main/kotlin/com/classmate/core/exporting/ContentExporter.kt`
- `core/src/main/kotlin/com/classmate/core/exporting/StudyReportDocxRenderer.kt`
- `app/src/main/java/com/classmate/app/exporting/ExportCenter.kt`

Export must not include private config, provider traces, raw internal ids, or fake official labels.

## Do Not Repeat These Mistakes

1. Do not say official TTS lacks protocol just because a demo file is not nearby.
2. Do not call browser search an API search or recommendation engine.
3. Do not treat missing official config as a user error.
4. Do not label local fallback output as BlueLM output.
5. Do not call Android system ASR/TTS vivo official capability.
6. Do not say on-device 3B is available on every device.
7. Do not print or document real AppKey values, full auth headers, private config, or local config content.
8. Do not say smoke pass means app runtime live success.
9. Do not create transcript/evidence assets from empty ASR/OCR output.
10. Do not expose developer traces in ordinary user pages or demo scripts.

## Recommended Development Sequence

1. Read `OFFICIAL_INTERFACE_REFERENCE_INDEX.md`.
2. Open this guide's specific source files.
3. Check tests listed in `OFFICIAL_SOURCE_FILE_MAP.md`.
4. Preserve fallback behavior before adding official runtime behavior.
5. Update capability/status docs only after tests prove the route.
6. Keep all secrets redacted.
