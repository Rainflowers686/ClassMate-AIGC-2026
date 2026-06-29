> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Demo Device Provisioning

Date: 2026-06-20

This checklist prepares a cloud device or demo phone before unified validation. It must not print secrets, endpoint URLs, Authorization values, AppKey values, or `config.local.json` contents.

## Script

Run:

```powershell
scripts\qa\demo_device_provision.ps1
```

The script performs presence/status checks only:

- `ADB_DEVICE_CONNECTED`
- `APP_INSTALLED`
- `CLOUD_CONFIG_PRESENT`
- `ON_DEVICE_MODEL_PRESENT`
- `STORAGE_PERMISSION_READY`
- `RECORD_AUDIO_READY`
- `CAMERA_READY`
- `NETWORK_READY`
- `L3_DEMO_DATA_READY`

## Required Demo State

- `config.local.json` may exist locally for demo configuration, but it is never read or printed by the provisioning script.
- The on-device model directory is checked by path presence only: `/sdcard/1225`.
- All-files access is needed only for the official on-device model directory and user-selected local learning materials.
- `RECORD_AUDIO` is needed for recording artifacts.
- `CAMERA` is needed for photo/image learning material capture.

## GO / NO-GO

GO means every required status is ready. NO-GO means the demo should switch to the fallback route:

- no cloud config: use local safe learning pipeline and avoid cloud model claims
- no model directory or storage permission: do not demo on-device model behavior
- no recording permission: do not demo classroom recording
- no camera permission: use text or existing image/manual OCR text

## Do Not Claim

- Do not claim recording automatically transcribes.
- Do not claim official Embedding, Text Similarity, Translation, TTS, Function Calling, or ASR Long runtime success unless later validation proves the injected app adapter path.
- Do not run provider network smoke from the provisioning script.
