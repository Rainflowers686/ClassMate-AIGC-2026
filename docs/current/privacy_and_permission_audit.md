# Privacy and Permission Audit

Date: 2026-06-20

No key, auth value, endpoint URL, APK, AAB, AAR, font file, or local config content is recorded here.

## Manifest Permissions

| Permission | Status | Purpose | Risk mitigation |
| --- | --- | --- | --- |
| `INTERNET` | required | Cloud model/API connectivity and non-secret diagnostics. | Do not print credentials or endpoints. |
| `MANAGE_EXTERNAL_STORAGE` | required for demo/on-device model | Read the official on-device model directory `/sdcard/1225` and user-selected local learning materials. | Do not upload user files, do not scan unrelated directories, explain purpose in Settings/docs. |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | legacy only, capped at API 32 | Compatibility for older Android file import/export behavior. | Capped by `maxSdkVersion=32`; modern devices use picker/media APIs. |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` | required for imports | User-selected learning screenshots, videos/subtitle sources, recordings/audio sources. | User-initiated import only. |
| `CAMERA` | required for photo material capture | Capture blackboard, slide, paper, or question images. | Optional camera feature; no background capture. |
| `RECORD_AUDIO` | required for recording artifact | Classroom recording artifact and system ASR experiments. | User-initiated; app does not claim automatic official transcription. |
| `MODIFY_AUDIO_SETTINGS` | retained | Local TTS/ASR audio UX and routing adjustment. | No Bluetooth nearby-device permission is requested. |
| `POST_NOTIFICATIONS` | required on Android 13+ | Review/task reminders. | No reminder is sent before feature/user action enables it. |
| `mediatek.permission.ACCESS_APU_SYS` | vendor SDK support | Optional on-device SDK/APU access on compatible devices. | SDK missing/unavailable falls back safely. |

## Removed / Not Requested

- `BLUETOOTH_CONNECT`
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `BLUETOOTH_SCAN`

Reason: ClassMate currently has no real Bluetooth device feature. Keeping those permissions would overstate product behavior and expand privacy surface without need.

## User-visible Explanation

Settings should explain that all-files access is for the preset model directory and local learning files only. ClassMate does not read or upload unrelated files and does not print local config contents.
