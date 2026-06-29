> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# TTS, Translation, and Function Calling Productization Report

Date: 2026-06-20

## ClassMate 当前实现映射（1.14.2 / versionCode 115）

- 官方 TTS WebSocket：已按用户补充的官方协议完成代码接入；真实网络成功仍需 AppKey、网络、权限和目标设备验证。
- TTS fallback：官方 TTS 不可用时走 Android 系统 TTS；系统 TTS 不可用时保留听背文稿，不伪装生成音频。
- Translation：仍是资料辅助/derived artifact 方向，不作为 1.14.2 主演示的官方实时网络能力。
- Function Calling：本地 ToolOrchestrator 可解释地参与工具链；官方 Function Calling 不作为 1.14.2 已真机验证能力。
- 本文件下方保留 2026-06-20 阶段状态。若与当前状态冲突，以本节和 [OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](OFFICIAL_CAPABILITY_MATRIX_1_14_2.md) 为准。

## TTS

Status: `LOCAL_FALLBACK`; official runtime gateway path wired, official runtime still not configured/validated.

- Listen-review now has a `TtsPlaybackState` and a `LocalTtsPlayer` port.
- Android production wiring uses `AndroidLocalTtsPlayer` backed by `android.speech.tts.TextToSpeech`.
- Source types include summary, wrong-question explanation, and review card.
- Provider status distinguishes official TTS, Android local TTS, and none.
- The App can call the official runtime gateway before local playback. If a future injected `TtsProvider` returns audio, the App records official runtime success; otherwise local TTS/script fallback remains.
- Android local TTS fallback is the product path for this sprint; device validation still owns engine availability and voice behavior.
- Official TTS network execution is not claimed.
- Voice cloning is explicitly out of scope.

## Translation

Status: `NOT_CONFIGURED / SEAM_ONLY`; official runtime gateway path wired but not validated.

- `TranslationRequestRecord` and `TranslationResultRecord` are installed.
- Target language enum covers English, Chinese, Japanese, and Korean.
- Translation is stored as a derived artifact and does not overwrite original evidence.
- The App can call the official runtime gateway for lesson/evidence translation. If a future injected `TranslationProvider` returns translated text, the derived artifact records it; otherwise the original evidence is preserved.
- If official Translation is not configured, the app shows not-configured status and does not fake translated text.

## Function Calling

Status: `LOCAL_ORCHESTRATOR`; official runtime gateway path wired but official Function Calling is not configured/validated.

- Local `ToolOrchestrator` now produces structured `ToolStepRecord` entries.
- Tool steps show tool name, provider mode, input summary, output summary, and status.
- Planned chains can include OCR, ASR, PDF page OCR, Query Rewrite, Embedding, Text Similarity, LLM Summary, Question Generation, Review Update, TTS, and Translation.
- The App can ask an injected official Function Calling provider for a tool proposal. If it is missing, invalid, or parse-failed, local orchestration remains active and the blocker is visible in diagnostics.
- Official Function Calling remains unclaimed until schema/config and live validation exist.
