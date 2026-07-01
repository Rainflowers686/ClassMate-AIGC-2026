# Changelog

当前候选版本：`1.14.3 / versionCode 116`，本轮聚焦真机录音/转写入口、反馈即时优化、复习知识摘要和版本收口。

## 1.14.3 / 116 - capture readiness and feedback-driven learning refinement

- 录音实时转写不可用时提供系统语音设置入口，同时保留录音保存、手动转写和字幕导入 fallback。
- 反馈不只记录：对题目、证据、知识点和复习计划反馈会即时生成替换题或重写知识点摘要，并进入复习队列。
- 复习页和知识页展示本课核心知识点、相关知识点和证据摘录，减少技术统计露出。
- 小测替换题补充“答案详解”、选项解释和证据摘录，避免只给正确答案。
- 风险：官方 OCR/ASR/TTS 网络成功仍需真实 AppKey、权限和设备环境验证；系统语音设置入口是否可直达取决于 ROM。

## 1.14.2 / 115 - final real-device import and quiz blockers

- 修复真机导入和小测最终阻塞：OCR 失败段可手动补充，资料篮不因单项失败卡死。
- 小测无题时使用可答题的本地兜底，不再出现“没有微测题”空链路。
- 图片题保留图片证据，答题/错题/复习能回到来源。
- 反馈不只是收集：进入需复核、已复习、薄弱点和复习计划。
- 风险：官方 OCR/ASR/TTS 网络成功仍需真实 AppKey 和真机环境验证。

## 1.14.1 / 114 - polished study pack export upgrade

- 增加 AI 精修学习包导出流程，普通导出不被覆盖。
- 同一份精修 markdown 驱动 PDF、HTML、Word 兼容 HTML、Markdown、Text。
- SafeExportText 继续清理密钥、内部状态和 raw id。
- 风险：蓝心不可用时使用本地精修草稿，来源需诚实显示。

## 1.14.0 / 113 - real-device learning loop stabilization

- 稳定资料输入、知识结构、证据绑定、微测、反馈、复习计划主链路。
- 删除“问这节课”作为主入口，避免把项目叙事拉回聊天壳。
- 收敛小测入口，避免重复入口造成真机演示混乱。
- 风险：不同设备的系统 ASR/TTS 可用性仍需现场确认。

## 1.13.9 / 112 - learning loop convergence

- 收敛学习闭环中的 reviewed/weak 标记、反馈入口和复习队列。
- 增强图片题和证据回溯链路。
- 风险：官方网络能力仍需配置后验证。

## 1.13.8 / 111 - official WebSocket TTS provider

- 按官方 WebSocket TTS 协议完成代码接入。
- 接入官方 TTS -> 系统 TTS -> 文稿的诚实 fallback。
- 风险：官方 TTS 需要 AppKey、网络和真机验证；不能写成已 100% 跑通。

## 1.13.7 / 110 - official WebSocket ASR base

- 建立官方实时 ASR WebSocket 协议底座。
- 保留系统 SpeechRecognizer 和录音保存 fallback。
- 风险：流式 UI 和官方网络效果仍需真机凭据验证。

## 1.13.6 / 109 - i18n learning flow copy and guards

- 将学习流关键文案迁入 `appStrings(language)`。
- 增加中英 parity 和用户页 forbidden token guard。
- 风险：仍有部分历史/开发者文档保留中文或技术词，但普通用户主路径受 guard 保护。

## 1.13.5 / 108 - real TTS/subtitles/generated variant drills

- 接入真实系统 TTS 听背、字幕导入和变式题生成。
- 增强 answerable gate，坏题不进入练习。
- 风险：官方 TTS 网络路径当时仍处于后续接入阶段，已由 1.13.8 更新。

## Earlier milestones

- `1.12.x`：多图 OCR、证据资产、课程删除、导出安全、真机预检。
- `1.11.x`：非前端工程收口、OCR import/export guard。
- `1.10.x`：知识页本地化、CourseDetail 收敛、心流自定义时长。
- `1.0.x - 1.9.x`：L3 学习闭环、Practice/Review/WrongBook、官方能力 runtime wiring、红队修复。
