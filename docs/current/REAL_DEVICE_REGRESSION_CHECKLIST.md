# Real Device Regression Checklist - 1.14.2

当前最终清单，旧版 `realdevice_regression_checklist_v1.md` 保留作历史参考。

## 必测主链路

1. 文本/Markdown 导入 -> 生成知识结构 -> 微测 -> 错题 -> 复习 -> 证据详情。
2. 图片导入 -> OCR 成功/失败段 -> 手动补充 -> 图片 evidence -> 图片题。
3. 多文件/资料篮 -> 资料来源摘要 -> 生成课程。
4. PDF 页文本 -> DOCUMENT evidence -> 复习计划回源。
5. 录音 -> 系统 ASR 可用/不可用两种路径 -> AUDIO evidence / 手动转写。
6. 普通导出 -> AI 精修导出 -> 安全检查。

## 官方能力状态检查

- 蓝心大模型：配置 presence，真实调用结果，失败 fallback。
- 官方 OCR：配置可用时验证；不可用时不阻断。
- 官方 ASR Long：配置可用时验证；不可用时录音/系统/手动 fallback。
- 官方实时 ASR WebSocket：仅在凭据和设备准备好时验证。
- 官方 TTS WebSocket：仅在凭据准备好时验证；否则系统 TTS/文稿。
- 端侧 3B：仅目标机模型目录和权限满足时演示。

## 失败截图要求

每个失败点截图或录屏应包含：

1. 当前页面。
2. 用户可见错误文案。
3. 是否有下一步 fallback。
4. Diagnostics 中不含真实密钥。
