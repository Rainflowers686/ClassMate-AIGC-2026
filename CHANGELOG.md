# Changelog

当前候选版本：`1.14.6 / versionCode 119`，本轮聚焦真实服务接入路径：蓝心配置/诊断、官方 ASR 主路线、系统 ASR 降级为可选 fallback、学科知识点过滤贯穿和微测相关性 gate。

## 1.14.6 / 119 - official ASR routing and model diagnostics hardening

- ASR 主路线改为官方优先：官方实时 ASR / 官方长语音转写按配置进入 route plan；录音主按钮不再默认启动系统 SpeechRecognizer。
- 系统 SpeechRecognizer 降级为“可选设备 fallback”，只在用户主动选择时使用；无系统语音识别服务不再阻塞录音保存和手动转写。
- 录音保存后可对 app-private 音频触发“官方 ASR 转写录音”路径，复用现有官方长语音转写/手动 fallback 处理，不生成假 transcript。
- 设置保存的 AppID/AppKey 会同步喂给 capture readiness，OCR/官方 ASR/TTS 摘要不再和真实运行配置脱节；所有显示仍只暴露配置存在性，不输出密钥。
- 新增 QuizRelevanceGate：模型题和本地题必须绑定已接受学科知识点、证据摘录和答案详解，拒绝强调词/无关占位题。
- 继续保留本地整理、手动转写、系统 TTS/ASR 等 fallback，但普通用户文案不把 fallback 冒充蓝心或 vivo 官方能力。
- 风险：官方 BlueLM/ASR/TTS 网络成功仍依赖真实 AppKey、接口权限、网络、录音权限和设备验证；本轮不读取或提交 `config.local.json`。

当前候选版本：`1.14.5 / versionCode 118`，本轮聚焦真机反馈中的 OCR/文本学科知识点过滤、微测相关性、复习计划知识点口径、课程相关知识课内检索、ASR 不可用路径和新装默认外观。

## 1.14.5 / 118 - subject extraction, quiz relevance, and fresh-install appearance

- 新增学科知识点过滤：OCR、转写、文档和手动文本进入 L3 之前会过滤“同学们注意、重点来了、大家记一下、页面/按钮/点击”等课堂提示语和 UI 噪声。
- 知识框架、课程总结、相关知识点、复习计划、微测题和反馈后替换题共用过滤后的 evidence-bound knowledge points，避免围绕强调词生成知识点或题目。
- 低质量/纯噪声 OCR 不再硬造知识点和微测；用户可继续手动修正文本后再生成课程。
- 相关知识点只做课内检索：从已接受的课程知识点出发回到本课 evidence 摘录，不把浏览器搜索或外部内容写成课程总结来源。
- 微测题继续要求 knowledgePointId、evidenceIds、答案详解和干扰项解释；本地 fallback 只从本课材料与知识点生成。
- 心流实时转写页补充系统语音设置入口和“粘贴转写”路径；ASR 仍依赖设备系统服务或官方配置，不把系统 ASR 写成 vivo 官方能力。
- 新装默认外观改为“沉浸学习”，字体默认“端正阅读”；已有用户保存过的主题/字体不会被覆盖。
- 风险：官方 OCR/ASR/TTS 网络能力仍需要真实 AppKey、权限、设备 ROM 和接口状态验证；学科过滤为保守规则，疑难材料仍建议人工复核 OCR/转写文本。

当前候选版本：`1.14.4 / versionCode 117`，本轮聚焦真机 ASR 不可用兜底、蓝心失败分类与重试、微测题知识点/证据绑定、反馈即时优化和复习知识点摘要。

## 1.14.4 / 117 - real-device ASR, model failure, and quiz relevance regressions

- ASR 入口分层：录音仍可用；系统实时转写按设备能力显示；无系统语音识别服务时提供“打开语音设置 / 仅录音 / 粘贴转写文本”路径。
- 系统 ASR 优化：按 App 语言传入 `zh-CN` / `en-US`，final transcript 去重，避免 partial/final 重复污染课程材料。
- 蓝心失败分类：普通页面显示未配置、鉴权失败、网络不可达、超时、返回为空等友好原因；开发者诊断保留技术码；重试不清空本地结果。
- 微测题强化：模型题缺知识点或证据绑定时过滤；本地 fallback 题只围绕本课知识点和证据生成，不再使用“无关背景/只需背诵”占位项。
- 答案详解强化：题目与反馈替换题均包含知识点、证据摘录、正确项原因和错误项辨析。
- 复习与总结：普通用户页面收敛为学科知识点、薄弱点、待复核点、相关知识点和证据，不展示 L3 技术统计。
- 风险：官方 ASR/TTS/OCR 网络能力仍依赖真实 AppKey、权限、设备 ROM 和接口状态；系统语音设置入口是否直达取决于 ROM。

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
