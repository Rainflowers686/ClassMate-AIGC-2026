# TTS, Translation, and Function Calling Productization Report

Date: 2026-06-20

## TTS

Status: `LOCAL_FALLBACK`; official runtime gateway path wired.

- Listen-review now has a `TtsPlaybackState` and a `LocalTtsPlayer` port.
- Android production wiring uses `AndroidLocalTtsPlayer` backed by `android.speech.tts.TextToSpeech`.
- Source types include summary, wrong-question explanation, and review card.
- Provider status distinguishes official TTS, Android local TTS, and none.
- v1.6 calls the official runtime gateway before local playback. If an injected `TtsProvider` returns audio, the App records official runtime success; otherwise local TTS/script fallback remains.
- Android local TTS fallback is the product path for this sprint; device validation still owns engine availability and voice behavior.
- Official TTS network execution is not claimed.
- Voice cloning is explicitly out of scope.

## Translation

Status: `NOT_CONFIGURED / SEAM_ONLY`; official runtime gateway path wired.

- `TranslationRequestRecord` and `TranslationResultRecord` are installed.
- Target language enum covers English, Chinese, Japanese, and Korean.
- Translation is stored as a derived artifact and does not overwrite original evidence.
- v1.6 calls the official runtime gateway for lesson/evidence translation. If an injected `TranslationProvider` returns translated text, the derived artifact records it; otherwise the original evidence is preserved.
- If official Translation is not configured, the app shows not-configured status and does not fake translated text.

## Function Calling

Status: `LOCAL_ORCHESTRATOR`; official runtime gateway path wired.

- Local `ToolOrchestrator` now produces structured `ToolStepRecord` entries.
- Tool steps show tool name, provider mode, input summary, output summary, and status.
- Planned chains can include OCR, ASR, PDF page OCR, Query Rewrite, Embedding, Text Similarity, LLM Summary, Question Generation, Review Update, TTS, and Translation.
- v1.6 can ask an injected official Function Calling provider for a tool proposal. If it is missing, invalid, or parse-failed, local orchestration remains active and the blocker is visible in diagnostics.
- Official Function Calling remains unclaimed until schema/config and live validation exist.
