# Real Device Fix Matrix - 1.14.7 / versionCode 120

| 问题 | 本轮修复 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| 设置页显示已配置但 provider 可能读不到 | BlueLM dry-run 使用正式 `configBundle`；capture readiness 来自生产 `CaptureGateway` | `AppViewModelProviderConfigTest.officialProviderDryRunUsesSavedConfigAndRedactsCredentials` | 真实 AppID/AppKey 下最小 prompt 成功 |
| 蓝心失败原因不明确 | `OfficialProviderDiagnostics` 将错误归类为 SKIP、鉴权、网络、超时、参数、服务端、空返回、解析失败 | `OfficialProviderDiagnosticsTest` | 真实网络错误分类是否符合服务端返回 |
| ASR 不能继续依赖系统 SpeechRecognizer | 文档和诊断固定官方实时 ASR / 官方长语音转写 / 手动转写主路线；系统 ASR 仅可选 fallback | `OfficialAsrRoutePlannerTest`、设置页 dry-run | 官方 ASR 凭据与录音上传成功 |
| 官方长语音转写缺少可验证入口 | dry-run 在无音频时返回 `SKIPPED_NO_AUDIO`，不报假失败；脚本允许显式传入本地音频后委托 smoke | `provider_live_smoke.ps1` 默认 SKIP | 录 10 秒音频后发起真实长 ASR |
| OCR/TTS 是否配置不清楚 | 开发者页显示 OCR/TTS 配置分类；缺配置时手动 OCR 和系统 TTS/文稿 fallback 保持可用 | `OfficialProviderDiagnosticsTest.captureDiagnosticsSkipWhenCredentialsAreMissing` | 真机图片识别和 TTS WebSocket 合成 |

## 禁止夸大

- 不写“官方 ASR/TTS/OCR 已 100% 真机跑通”。
- 不把系统 ASR/TTS 写成 vivo 官方能力。
- 不把本地 fallback 写成 BlueLM 结果。
- 不在 UI、日志、文档或导出中显示真实 AppKey、Authorization 值。
