# Official Tool Matrix

> 状态：当前入口已更新到 `1.14.2 / 115`。详细矩阵见 [OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](OFFICIAL_CAPABILITY_MATRIX_1_14_2.md)。

## 1.14.2 工具状态摘要

| 工具 | 状态 | 不可夸大边界 |
| --- | --- | --- |
| 蓝心大模型 | 已接入学习分析/反馈/AI 精修 | 需 AppKey 真机验证 |
| 官方 OCR | app-level 图片输入路径存在 | 未配置时走手动补充 |
| 官方 ASR Long | 任务流代码路径存在 | 未配置时不是用户错误 |
| 官方实时 ASR WS | WebSocket 协议底座存在 | 流式真机体验待验证 |
| 官方 TTS WS | 已代码接入 | 需 AppKey 真机验证，不做声音复刻 |
| Retrieval 三能力 | official-first/fallback 架构存在 | 真正 official used 需配置验证 |
| Function calling | 本地 orchestrator active | 不写官方 FC 已完成 |
| 端侧模型 | optional fallback | 不写所有设备可用 |

## 用户侧表达

- “官方能力已接入代码路径，未配置时使用系统或本地 fallback。”
- “真实网络能力需要 AppKey、网络、权限和目标设备验证。”
- 禁止写“全部官方工具已完整真机跑通”。
