# TTS, Translation, and Function Calling Productization Report

Date: 2026-06-20

## TTS

Status: `LOCAL_FALLBACK`.

- Listen-review now has a `TtsPlaybackState` and a `LocalTtsPlayer` port.
- Android production wiring uses `AndroidLocalTtsPlayer` backed by `android.speech.tts.TextToSpeech`.
- Source types include summary, wrong-question explanation, and review card.
- Provider status distinguishes official TTS, Android local TTS, and none.
- Android local TTS fallback is the product path for this sprint; device validation still owns engine availability and voice behavior.
- Official TTS network execution is not claimed.
- Voice cloning is explicitly out of scope.

## Translation

Status: `NOT_CONFIGURED / SEAM_ONLY`.

- `TranslationRequestRecord` and `TranslationResultRecord` are installed.
- Target language enum covers English, Chinese, Japanese, and Korean.
- Translation is stored as a derived artifact and does not overwrite original evidence.
- If official Translation is not configured, the app shows not-configured status and does not fake translated text.

## Function Calling

Status: `LOCAL_ORCHESTRATOR`.

- Local `ToolOrchestrator` now produces structured `ToolStepRecord` entries.
- Tool steps show tool name, provider mode, input summary, output summary, and status.
- Planned chains can include OCR, ASR, PDF page OCR, Query Rewrite, Embedding, Text Similarity, LLM Summary, Question Generation, Review Update, TTS, and Translation.
- Official Function Calling remains unclaimed until schema/config and live validation exist.
