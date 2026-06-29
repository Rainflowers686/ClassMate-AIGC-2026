# Official Capability Matrix - ClassMate 1.14.2

版本：`1.14.2 / versionCode 115`
最新提交：`7473fb1`

## 状态口径

- **已接入**：代码路径存在，并进入 App 学习闭环或真实 fallback。
- **需配置/真机验证**：代码路径存在，但真实官方网络成功依赖 AppKey、权限、设备或接口状态。
- **系统 fallback**：使用 Android 系统能力，如 SpeechRecognizer 或 TextToSpeech。
- **本地 fallback**：使用本地规则、lexical index、手动输入或文稿。
- **deferred**：不在 1.14.2 主演示范围，不展示假入口。

## 官方与系统能力矩阵

| 能力 | 协议/形态 | 当前状态 | fallback | 需 AppKey | 真机验证 | 用户侧文案 |
| --- | --- | --- | --- | --- | --- | --- |
| 蓝心大模型 HTTP | OpenAI-compatible HTTP | 已接入分析、反馈、AI 精修导出；需配置验证真实网络 | 端侧 3B / 本地规则 | 是 | 待真实配置复测 | 蓝心/端侧/本地来源诚实显示 |
| 官方长语音转写 1739 HTTP | create/upload/run/progress/result 任务流 | 已代码接入；录音与转写链路可走 fallback | 系统 ASR / 手动转写 / 录音保存 | 是 | 待配置验证 | 录音保存、可重试、可手动转写 |
| 官方实时 ASR WebSocket | WebSocket ASR protocol | 协议底座已接入；流式 UI 待真机验证 | 系统 SpeechRecognizer / 录音保存 | 是 | 待真机验证 | 系统实时转写 / 录音保存 |
| 官方 TTS WebSocket | WebSocket TTS protocol | 已按官方协议代码接入；不是缺协议 | 系统 TTS / 听背文稿 | 是 | 待真机验证 | 官方 TTS / 系统 TTS / 仅文稿 |
| 系统 ASR | Android SpeechRecognizer | 已接入 | 录音保存 / 手动转写 | 否 | 设备依赖 | 系统实时转写，不可用时给中文提示 |
| 系统 TTS | Android TextToSpeech | 已接入 | 文稿导出 | 否 | 设备依赖 | 系统 TTS 生成，失败保留脚本 |
| 端侧 3B 大模型 | optional reflection / local SDK | optional fallback；非所有设备必备 | 云端或本地规则 | 设备依赖 | 待目标设备验证 | 端侧草稿 / 不可用说明 |
| 通用 OCR | 官方 OCR / capture gateway | 图片导入接入；失败段可手动补充 | 手动输入 / 端侧草稿 | 是 | 待配置验证 | OCR 未配置不阻断资料输入 |
| 文本向量 | official embedding runtime + local index | 官方优先结构存在；本地 lexical index 可用 | 本地 lexical vector | 是 | 待配置验证 | 关联证据/相似知识点，来源不夸大 |
| 文本相似度 | official similarity/rerank + local similarity | 官方优先结构存在；本地相似度可用 | token overlap / local similarity | 是 | 待配置验证 | 关联相似错题/证据 |
| 查询改写 | official query rewrite + local rewrite | 官方优先结构存在；本地改写可用 | local query planning | 是 | 待配置验证 | 用于检索/复习问题整理 |
| Function calling | local tool orchestrator / official seam | 本地编排参与流程；官方 FC 未作为真机主路径 | local orchestrator | 视配置 | 未产品化 | 不写官方 FC 已完成 |
| 文本翻译 | translation seam | 可做双语资料辅助；官方网络未作为主链承诺 | 原文学习 / 双语草稿 | 视配置 | 待验证 | 翻译待配置，不覆盖原文 |
| 音频生成 | TTS/audio script | 听背文稿和系统 TTS 可用；官方 TTS 见上 | 系统 TTS / 文稿 | 是 | 待验证 | 不做声音复刻 |
| 图片生成 | experimental/deferred | 仅可生成学习图解 prompt，默认不展示 | prompt 草稿 | 需配置 | 未产品化 | 不伪装真图生成 |
| 视频生成 | experimental/deferred | 仅可生成复习短视频分镜，默认不展示 | storyboard 草稿 | 需配置 | 未产品化 | 不伪装真视频生成 |
| 方言自由说 | ASR capability direction | 音频模式保留，当前依赖 ASR/后处理 | 系统 ASR / 手动转写 | 视配置 | 待验证 | 口音/低置信提示 |
| 同声传译 | experimental/deferred | 不作为默认入口 | 转写 + 翻译草稿 | 需配置 | 未产品化 | 不伪装实时同传 |
| 端侧文本审核 | AAR/deferred | 不作为 1.14.2 主链路展示 | SafeExportText / 本地安全规则 | 需要 AAR | 未接入 | 不展示假入口 |
| 端侧能力相关文件 | model/resource readiness | 诊断和 readiness 检查 | 云端/本地规则 | 设备依赖 | 待设备验证 | 端侧资源状态 |
| 外部搜索 | Browser Intent | 已接入浏览器搜索入口 | 无 API | 否 | 可测 | 打开浏览器搜索，不是推荐算法 |

## 关键修正

1. 官方 TTS 不再写“缺协议不能做”；1.13.8 后已有 WebSocket 协议代码接入。
2. 官方 ASR/TTS 仍不能写“已 100% 真机跑通”；需要 AppKey、网络、权限和目标设备验证。
3. 官方 ASR 未配置不是用户错误；录音保存、系统 ASR 和手动转写仍可完成学习闭环。
4. 端侧 3B 是 optional fallback，不是所有设备必备主链路。
5. 外部搜索是浏览器 Intent，不是站内 API 或推荐算法。

## 建议真机验证顺序

1. 蓝心大模型配置 presence 与普通学习分析。
2. 官方 OCR 图片导入。
3. 官方长语音转写任务流。
4. 官方实时 ASR WebSocket。
5. 官方 TTS WebSocket。
6. 系统 ASR/TTS fallback。
7. 端侧模型目录与权限。
