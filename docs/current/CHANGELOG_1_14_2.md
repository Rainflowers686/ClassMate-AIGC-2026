# Changelog 1.14.2

本文件是 `CHANGELOG.md` 的当前候选版展开说明。

| 版本 | 提交主题 | 变更 | 风险/验证 |
| --- | --- | --- | --- |
| 1.14.2 / 115 | final real-device import and quiz blockers | OCR 失败段手动输入；无微测题兜底；图片题回证据；反馈进入复习闭环 | 官方 OCR/ASR/TTS 仍需 AppKey 真机验证 |
| 1.14.1 / 114 | polished study pack export upgrade | AI 精修学习包，普通导出不被覆盖，SafeExportText 继续清理 | 蓝心失败时显示 fallback 来源 |
| 1.14.0 / 113 | real-device learning loop stabilization | 主链路收敛，删除聊天式入口，收敛小测入口 | 系统 ASR/TTS 设备依赖 |
| 1.13.9 / 112 | learning loop convergence | reviewed/weak、反馈、复习队列闭合 | 真机回归必测 |
| 1.13.8 / 111 | official WebSocket TTS provider | 官方 TTS WebSocket 代码接入，系统 TTS fallback | 需 AppKey 网络验证 |
| 1.13.7 / 110 | official WebSocket ASR base | 官方实时 ASR WebSocket 协议底座 | 需真机流式验证 |
| 1.13.6 / 109 | i18n learning flow copy and guards | 主学习路径文案本地化与 guard | 部分历史文档仍中文 |
| 1.13.5 / 108 | real TTS/subtitles/generated variant drills | 系统 TTS、字幕、变式题和 answerable gate | 官方 TTS 已由 1.13.8 接入 |

## 1.14.2 修复细节

- OCR 不再出现“一张失败整批失败”；失败项可手动补文字。
- 官方 ASR 未配置时保留录音、系统 ASR 和手动转写路径。
- 没有模型生成题时，Practice 使用本地规则生成可答题小测。
- 图片题答题、错题和复习能回到原图证据。
- 学习反馈能推动复习计划和状态，而不是只收集意见。
- AI 精修导出是主动增强，不覆盖普通导出。

## 1.14.2 仍需验证

- 使用真实 AppKey 验证蓝心大模型、官方 OCR、长语音转写、实时 ASR WebSocket、官方 TTS WebSocket。
- 在目标 vivo / 云真机上验证系统 ASR/TTS 是否可用。
- 录屏确认导出文件不含密钥、内部状态或 raw id。
