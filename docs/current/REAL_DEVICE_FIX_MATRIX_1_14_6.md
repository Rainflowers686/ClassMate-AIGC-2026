# Real Device Fix Matrix - 1.14.6 / 119

| 问题 | 本轮修复 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| 蓝心大模型调用失败 | 设置保存值与运行配置同源；BlueLM diagnostic 继续使用同一 `ProviderAskChatClient` 配置；普通用户保留本地结果和重试路径 | `AppViewModelProviderConfigTest` | 真实 AppID/AppKey、网络和接口权限 |
| ASR 不能依赖系统 SpeechRecognizer | 主路线改为官方实时/官方长语音/手动转写；系统识别只作为可选 fallback | `OfficialAsrRoutePlannerTest`、`RecordingTranscriptionFlowTest` | 官方 ASR 真实联网和录音权限 |
| 官方 ASR 未配置时录音不可用 | 主录音只保存音频；未配置时提示粘贴转写文本，不生成假 transcript | `AsrUnavailableFallbackTest`、`RecordingTranscriptionFlowTest` | 低端 ROM 文件写入与权限 |
| OCR/文本知识点仍有噪声 | 继续使用 `SubjectKnowledgeExtractor`，题目侧新增 `QuizRelevanceGate` 拦截噪声题 | `SubjectKnowledgeExtractorTest`、`QuizRelevanceGateTest` | 复杂课件 OCR 后人工确认 |
| 微测题不够贴合知识点 | 题目必须绑定 accepted knowledge point + evidence；无证据或无关占位题过滤 | `QuizRelevanceGateTest`、`LocalFallbackQuizTest` | 蓝心返回异常题目的真机样本 |
| 反馈后替换题质量 | 替换题仍走同一知识点/evidence 过滤，避免强调词进入替换题 | `LearningLoopRefinementEnginesTest` | 复杂反馈场景复测 |

## 禁止夸大

- 系统 ASR 不是 vivo 官方 ASR。
- 本地 fallback 不是蓝心成功。
- 官方 ASR/TTS/BlueLM 网络成功仍需真实凭据和设备验证。
