# ClassMate 1.14.18

ClassMate is an AIGC learning-loop Android app for the national AIGC competition. It turns classroom material into a traceable study workflow:

```text
material input -> subject knowledge -> evidence binding -> micro quiz -> feedback -> review plan -> AI-polished export
```

Current candidate:

| Item | Value |
| --- | --- |
| Version | `1.14.18 / versionCode 131` |
| Branch | `main` |
| Commit | local 1.14.18 commit after validation |
| Main demo state | Final round-two candidate; Practice -> Review crash fix still needs final real-device confirmation |

## Current Focus

- Practice completion uses deferred Review navigation to avoid Compose screen-switch/key crashes.
- Placeholder/retry quiz items are blocked from student practice.
- Quiz quality gate, fill-in requirement, OCR raw/normalized/candidate separation, and persistent diagnostics remain active.
- BlueLM/qwen strategy remains unchanged: users see BlueLM/蓝心大模型 wording, while technical docs describe the qwen3.5-plus API mapping.

## 当前中文说明

ClassMate 当前候选版本是 `1.14.18 / versionCode 131`。本轮重点不是新增大功能，而是修复真机完成小测后切回复习页时的 Compose 导航崩溃，并把复赛材料整理到仓库内可执行状态。练习完成后不再在按钮点击同一帧同步切换页面，而是先保存结果、清理练习状态，再由根页面延迟消费导航请求进入复习页；如果导航失败，页面会保留安全完成态，用户可以再次点击返回复习计划。复习页和题目列表继续保留稳定 key、不可变列表、诊断事件和崩溃堆栈记录，便于真机复测定位。

能力状态保持诚实口径：蓝心大模型、官方 OCR/ASR/TTS 的真实成功仍依赖有效配置、网络和设备权限；配置缺失或服务失败时，主学习链路会进入本地基础整理，不把本地结果冒充为蓝心输出。实验性能力默认关闭，不影响“资料导入 -> 证据绑定 -> 微测 -> 复习计划 -> 导出”的核心闭环。18 项官方能力的当前 readiness 和兜底边界以 [official_18_capability_l3_readiness.md](docs/current/official_18_capability_l3_readiness.md) 以及 `docs/current` 下 1.14.18 文档为准。

## Build

```powershell
cd "D:\Edge Download\AIGC\ClassMate"
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

Debug APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Validate

```powershell
git diff --check
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\provider_live_smoke.ps1
```

## Current Docs

- [Current document index](docs/current/DOCUMENT_INDEX.md)
- [1.14.18 changelog](docs/current/CHANGELOG_1_14_18.md)
- [1.14.18 real-device test manual](docs/current/REAL_DEVICE_TEST_MANUAL_1_14_18.md)
- [1.14.18 fix matrix](docs/current/REAL_DEVICE_FIX_MATRIX_1_14_18.md)
- [Round-two submission workspace](docs/submission/round2/README.md)
- [Build and release guide](docs/current/BUILD_AND_RELEASE_GUIDE_1_14_18.md)
- [Core LLM code package guide](docs/current/CORE_LLM_CODE_PACKAGE_GUIDE_1_14_18.md)

## Safety Boundaries

- Do not commit `config.local.json`, APK/AAB/AAR, fonts, OfficialDemos, or real credentials.
- Do not expose AppKey, Authorization, Bearer, or token values in docs, UI, logs, screenshots, or exports.
- Do not claim official provider live success without real AppKey/device/network verification.
- Do not describe local fallback as BlueLM output.
