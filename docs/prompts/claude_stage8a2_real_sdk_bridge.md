# Claude Stage 8A-2 Real SDK Bridge Prompt

请在 ClassMate 中实现真实端侧 SDK bridge。先读当前代码和本文档，再动手。不要改坏 Official BlueLM 云端主路径，不要改 ProviderResolver 顺序，不要削弱 validators。

## 当前 SDK 事实

官方 demo 源码已复制到英文路径：

```text
D:\AIGC_SDK\ondevice_llm_demo_src
```

已成功构建 AAR：

```text
D:\AIGC_SDK\ondevice_llm_demo_src\llm-sdk\build\outputs\aar\llm-sdk-release.aar
```

已复制到 ClassMate：

```text
D:\Edge Download\AIGC\ClassMate\app\libs\llm-sdk-release.aar
```

AAR 被 `.gitignore` 忽略：

```text
app/libs/*.aar
```

不要提交 AAR。

## javap 已确认签名

### LlmConfig

- `modelPath`
- `nPredict`
- `nCtx`
- `nThreads`
- `topK`
- `topP`
- `temperature`
- `npuPower`
- `multimodal`

### LlmManager

- `init(LlmConfig): int`
- `callVit(byte[], int, int): int`
- `generate(String, TokenCallback): void`
- `interrupt(): void`
- `release(): void`

### TokenCallback

- `onToken(String)`
- `onComplete()`
- `onError(int,String)`

重要：`onComplete()` 无参数。不要写旧版 stats 参数。

## 实现要求

P0：

- 用 reflection 实现 `RealVivoOnDeviceLlmBridge`。
- 缺 AAR、缺类、缺模型目录时返回 unavailable。
- 不直接导入 SDK 包名。
- optional AAR include：本地有 AAR 时可用，本地没有时仍可编译。
- 纯文本 init/generate 诊断。
- interrupt/release 生命周期。
- LocalProviderChain fallback 保持 OnDevice -> LocalRule。

P1：

- 多模态 diagnostic。
- `LlmConfig.multimodal = true`。
- Bitmap ARGB_8888 转 RGB byte array。
- `callVit(rgbData, width, height)`。
- ret == 0 后再 generate。
- VIT 错误码显示短标签。

P2：

- Settings SDK card。
- Settings multimodal card。
- model path 可配置或至少可显示。
- 真机 smoke 记录。

## 多模态输入模板

```text
[|Human|]:<im_start><image><im_end>用户问题
[|AI|]:
```

## 不允许

- 不读取 `config.local.json` 内容。
- 不写真实密钥。
- 不新增危险存储权限。
- 不提交 AAR。
- 不改 Official BlueLM 云端主路径。
- 不改 ProviderResolver 顺序。
- 不削弱 ResultValidator / EvidenceValidator / EvidenceResolver。
- 不把端侧 unavailable 显示成成功。
- 不声称多模态已经完整进入学习链路，除非有真实链路证据。

## 测试命令

完成后运行：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
git diff --check
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
.\scripts\qa\stage8a2_sdk_preflight.ps1
```

## 最终报告

请报告：

- 是否真实接入 AAR bridge。
- 是否用 reflection，是否避免直接导入 SDK 包。
- 纯文本 init/generate 结果。
- 多模态 callVit 诊断结果。
- TokenCallback 签名是否为无参完成回调。
- 是否新增权限。
- 是否改 BlueLM 云端主路径、ProviderResolver 或 validators。
- 测试和 secrets scan 结果。
- 真机 smoke 下一步。
