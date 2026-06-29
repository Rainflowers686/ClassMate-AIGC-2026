> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# ASR Long Productization Report

Date: 2026-06-20

## ClassMate 当前实现映射（1.14.2 / versionCode 115）

- 官方长语音转写 1739 HTTP：任务流代码路径存在，真实 upload/poll/result 仍需 AppKey 和真机验证。
- 官方实时 ASR WebSocket：协议底座已接入；流式体验需目标设备验证。
- 系统 ASR：Android SpeechRecognizer fallback 已接入，设备无语音服务时会提示手动转写。
- 录音 fallback：即使官方/系统 ASR 不可用，录音文件仍保存，用户可粘贴或编辑转写进入学习闭环。
- 本文件下方保留 2026-06-20 阶段状态。若与当前状态冲突，以本节和 [OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](OFFICIAL_CAPABILITY_MATRIX_1_14_2.md) 为准。

## Current Status

Core contract: PRESENT.
App-level wiring: PARTIAL.
Network smoke in v1.8: NOT RUN.
Demo status: recording artifact + ASR job seam + Manual transcript fallback.

## Implemented

- `AsrLongJob` now records provider status, upload status, polling status, transcript text, transcript segments, error code, and timestamps.
- Recording/audio artifact import creates an ASR Long job.
- Missing official app config maps to `OFFICIAL_ASR_CONFIG_MISSING`.
- Present official app config maps to `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING` until non-sensitive audio upload/poll/result validation is completed.
- Transcript fill-in maps to `TRANSCRIPT_READY` and enters the same L3 pipeline with transcript timeline, summary, evidence, knowledge points, questions, review queue, and mastery.
- Runtime diagnostics report `OFFICIAL_APP_WIRING_PENDING` for core-present/app-validation-pending status instead of schema-missing language.

## Core Evidence

- Core provider contract exists in `VivoAsrProvider`.
- Official doc 1739 task flow is represented as create/upload/run/progress/result.
- App demo path has not validated real upload, polling, result parsing, cancellation, timeout, or classroom-audio privacy behavior.

## Exact Remaining Gap

The blocker is not missing schema. The blocker is app-level wiring and validation:

- app adapter not validated with a non-sensitive audio file
- local config may not be provisioned on demo/cloud device
- upload/polling/result lifecycle is not exercised in the current demo app
- recording is not presented as automatic transcription
- no live upload/poll/result call was run in the v1.8 status-freeze task

## Fallback

Manual transcript fallback is complete for L3 entry. Segments are generated from paragraphs/sentences and marked fallback-generated.
