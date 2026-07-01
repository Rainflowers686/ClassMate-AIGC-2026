# ClassMate 1.14.7 / versionCode 120

最新提交目标：`fix(product): make official ASR and BlueLM configuration verifiable`

## 本轮范围

本轮聚焦真实官方服务配置与联网诊断链路，不新增学习主功能，不把 fallback 写成官方成功。

## 关键变化

- BlueLM dry-run 继续使用正式 provider 的同一配置、header 和最小安全 prompt。
- 新增官方服务 dry-run 聚合结果：BlueLM、官方实时 ASR、官方长语音转写、官方 TTS、官方 OCR。
- 缺配置输出 `SKIP`，配置存在但未做真实媒体请求输出“待真机验证”或“缺少测试音频”。
- 开发者设置页新增“运行官方服务 dry-run”入口，结果仅显示分类和脱敏说明。
- 新增 `scripts/qa/provider_live_smoke.ps1`：默认不读本地凭据、不联网；显式授权后才委托现有官方 smoke。

## 风险

- 官方服务真实成功仍依赖 AppID/AppKey、接口权限、网络、设备权限和服务端状态。
- 系统 SpeechRecognizer 仍只是可选设备 fallback，不作为 ClassMate ASR 主路线。
- 本地规则、手动转写和系统 TTS 只能作为 fallback，不能冒充 vivo 官方能力。

## 验证

必须运行：

```powershell
git diff --check
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\provider_live_smoke.ps1
```
