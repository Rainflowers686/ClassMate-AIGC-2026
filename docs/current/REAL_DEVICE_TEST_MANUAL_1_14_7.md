# Real Device Test Manual - 1.14.7 / versionCode 120

## A. 开发者 dry-run

1. 打开设置 -> 开发者设置。
2. 点击“运行官方服务 dry-run”。
3. 预期：
   - 未配置时 BlueLM/ASR/TTS 输出 SKIP。
   - 配置存在但未做真实媒体请求时输出“配置存在，待真机验证”。
   - 长语音无音频时输出“SKIP：缺少测试音频”。
   - 不显示 AppKey、Authorization 或完整响应正文。

## B. BlueLM 最小请求

1. 在 AI 模型配置保存 AppID/AppKey。
2. 回到开发者设置运行 dry-run。
3. 预期：
   - 成功时显示“蓝心云端大模型：成功”。
   - 失败时显示鉴权失败、网络失败、超时、参数错误、返回空或解析失败之一。
   - 本地学习结果不会被清空。

## C. 官方 ASR 主路线

1. 录制 10 秒课堂音频。
2. 如果官方 ASR 已配置，停止录音后点击官方 ASR 转写录音。
3. 如果未配置，使用“粘贴转写文本”继续学习闭环。
4. 预期：
   - 系统 SpeechRecognizer 不再是默认主承诺。
   - 未配置官方 ASR 不阻止录音保存。
   - 失败不生成假 transcript。

## D. OCR / TTS

1. 导入图片并触发 OCR。
2. 如果 OCR 未配置或失败，手动输入图片文字后确认。
3. 触发听背或课程精华音频。
4. 预期：
   - OCR/TTS 未配置时不会标为官方成功。
   - 手动 OCR 和系统 TTS/文稿 fallback 继续可用。

## E. 本地 smoke 脚本

默认安全检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\provider_live_smoke.ps1
```

预期：未显式读取本地配置时输出 SKIP 或 READY manual fallback，不打印密钥。

显式本地配置检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\provider_live_smoke.ps1 -UseLocalConfig
```

仅在本机已有合法配置并确认允许读取本地配置时运行；输出仍必须脱敏。
