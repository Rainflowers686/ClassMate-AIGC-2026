# Official Interface Gaps and Boundaries

Current update: `1.14.8 / versionCode 121`

1.14.8 boundary: BlueLM is now the preferred AI provider for the main learning features when the saved official configuration is Ready. This is a product-flow guarantee, not a live-network guarantee. Real success still depends on AppID/AppKey, network, endpoint permission, timeout behavior, and service status. If the provider fails, ClassMate must keep the local/on-device result and label it honestly.

The following features should attempt BlueLM before fallback when configured: course analysis, course summary and related knowledge, practice generation, feedback refinement/replacement, weakness variants, secondary learning enhancements, and polished study-pack export.

1.14.7 adds provider dry-run categories and `scripts/qa/provider_live_smoke.ps1`. These diagnostics prove configuration wiring and safe error classification; they do not by themselves prove full real-device media success. `SKIP` means missing or intentionally uninspected local credentials, not a user failure. `READY_CONFIG_PRESENT` means the provider can see configuration and still needs a real AppKey/network/device validation. `SKIPPED_NO_AUDIO` means long ASR was not live-tested because no test recording was supplied.

Version: `1.14.2 / versionCode 115`

This file is the boundary contract for official vivo/BlueLM capability claims in ClassMate. It prevents future work from overstating code paths, smoke evidence, or fallback behavior.

## Implemented in Code, Still Needs AppKey and Real-Device Validation

| Capability | Current code status | Why validation is still required | Safe claim |
| --- | --- | --- | --- |
| BlueLM cloud chat | HTTP provider, diagnostics, prompt builders, and fallback routing exist | Needs real AppID/AppKey, network access, timeout behavior, and service permission validation | BlueLM cloud route is implemented and config-gated; fallback keeps learning flow usable |
| Official TTS WebSocket | Protocol, session, PCM/WAV writer, app provider, and tests exist | Needs AppKey, service permission, live WebSocket synthesis, and audio playback validation | Official TTS code path exists; system TTS and script fallback remain available |
| Official realtime ASR WebSocket | Protocol, realtime session, transport, PCM capture, and tests exist | Needs AppKey, microphone permission, device audio routing, WebSocket service validation | Official realtime ASR base exists; system ASR, recording, and manual transcript fallback remain available |
| Official long ASR HTTP | `VivoAsrProvider` implements create/upload/run/progress/result task flow | Needs valid endpoint access, real audio upload, polling, result schema validation | Long ASR task flow exists in code; live task success remains validation pending |
| OCR provider | Provider code and product fallback flow exist | Needs configured OCR endpoint/access and real image validation | OCR is config-gated; manual image text entry prevents workflow dead end |
| Retrieval providers | Query rewrite, embedding, and similarity adapters are wired through official runtime gateway | Needs valid retrieval config and live official network validation | Official-first retrieval path exists; local retrieval fallback remains active |

## Fallback Routes That Must Stay Honest

| Fallback | Used when | User-facing meaning |
| --- | --- | --- |
| Android system ASR | Official ASR unavailable or unconfigured, and system recognizer exists | System realtime transcript, not vivo official ASR |
| Recording saved | ASR unavailable, fails, or user chooses manual flow | Audio evidence is preserved for later transcript |
| Manual transcript | ASR unavailable or low quality | User-provided text enters the same learning loop |
| Android system TTS | Official TTS unavailable or fails | Device TTS reads review/script content |
| Listen-review script | No usable TTS engine | Text script remains available |
| Local rule analysis | BlueLM/cloud/on-device unavailable | Minimal knowledge/quiz/review loop continues |
| OCR manual text | OCR unavailable or low quality | Image remains evidence; user enters readable text |
| Browser search intent | User asks for external video/search | Opens browser search, not API recommendation |

## Deferred or Not Productized in 1.14.2

| Capability | Current boundary |
| --- | --- |
| Text moderation AAR | Local official notes exist, but it is not a productized runtime in this version. Safety still relies on local/export guards and safe copy. |
| Dialect free-speech product path | Protocol direction exists in ASR code comments and capability docs, but it is not a completed product route in 1.14.2. |
| Simultaneous interpretation | Treated as experimental/deferred; no fake realtime interpretation UI should be exposed. |
| Image/video generation | Not part of the stable real-device demo route. |
| External search API | Not integrated. Current product path is browser intent only. |
| On-device 3B model on all phones | Not guaranteed. It is optional/reflection-based and device/resource dependent. |

## Statements That Are Allowed

- Official TTS WebSocket is code-integrated and has system TTS/script fallback.
- Official realtime ASR WebSocket base is code-integrated and has system ASR/recording/manual fallback.
- Long ASR HTTP task flow is implemented in provider code and still needs live configuration validation.
- BlueLM cloud is config-gated and local/on-device fallback keeps the learning loop alive.
- OCR failures no longer block image learning; manual correction is available.
- Retrieval official-first adapters exist; official use depends on runtime config and validation.

## Statements That Are Not Allowed

- All official interfaces are fully real-device verified.
- Official ASR or TTS is guaranteed to work on every demo phone.
- Android system ASR/TTS is a vivo official capability.
- Local fallback output is BlueLM output.
- Browser search is a video recommendation API.
- On-device 3B model is available on all Android devices.
- OCR failure is the user’s fault.
- Official TTS is blocked because the protocol is missing.

## Claude Development Boundary

Before modifying any official capability:

1. Read `docs/current/CLAUDE_OFFICIAL_INTERFACE_LOOKUP_GUIDE.md`.
2. Check the relevant protocol section in `docs/current/OFFICIAL_PROTOCOL_QUICK_REFERENCE.md`.
3. Open the mapped source files in `docs/current/OFFICIAL_SOURCE_FILE_MAP.md`.
4. Keep fallback behavior intact.
5. Add or update tests before changing user-facing claims.
6. Never print AppKey, private config, or full auth headers.
