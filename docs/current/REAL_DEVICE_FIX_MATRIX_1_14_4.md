# Real Device Fix Matrix - 1.14.4 / 117

| 问题 | 上轮已有修复 | 真机仍复现原因 | 1.14.4 修复 | 验证 |
| --- | --- | --- | --- | --- |
| ASR 无法使用 | 系统 ASR seam、官方 ASR seam、手动转写 fallback | UI 仍把“实时转写”当主按钮，系统设置入口没有 resolve/fallback 链 | 录音、系统实时转写、官方 ASR、手动转写分层；无系统服务时显示“打开语音设置 / 仅录音 / 粘贴转写文本” | `SpeechRecognitionDiagnosticsTest`、`AsrTranscriptMapperTest`、真机无识别服务路径 |
| 提示没有语音识别服务 | 有 unsupported 状态 | 入口只提示错误，用户不知道还能录音和手动转写 | 设置 Intent 逐级尝试语音输入、输入法、应用详情、系统设置；录音仍可继续 | 真机点击“打开语音设置”和“仅录音” |
| 微测答案无详解 | QuizQuality 有基础解析 | 本地/替换题解析过泛，未强调知识点和证据 | 本地题和反馈替换题写入答案详解、知识点、证据摘录和错误项辨析 | `L3LearningPipelineTest`、`LearningLoopRefinementEnginesTest` |
| 复习计划显示 L3 技术统计 | 已有复习计划和诊断 | CourseDetail / Review 仍有技术词或 raw fallback 风险 | 普通页面改为“学习闭环 / 本课核心知识点 / 相关知识点 / 薄弱 / 需复核” | `ReviewPlanKnowledgeSummaryTextTest`、真机复习页 |
| 课程总结知识点不准确 | 有 related knowledge engine | 课内相关知识点入口不够明显 | 相关知识点只从本课 evidence / OCR / transcript / material text 聚合，不调用外部 API | `LearningLoopRefinementEnginesTest` |
| 反馈后无明显优化 | 反馈事件与 optimizer 已有 | 当前题变化不够可见，替换题质量泛化 | 题目反馈退休原题并生成同知识点替换题；知识点/证据反馈进入需复核和重写 | `PracticeFlowTest`、`LearningLoopRefinementEnginesTest` |
| 题目和知识点无关 | 模型题和本地题都存在 | 本地兜底曾使用“与本节课无关”等占位干扰项；模型坏题缺 gate | 缺知识点/证据绑定的模型题过滤；本地题干扰项来自同课知识点/证据范围 | `LocalFallbackQuizTest`、`L3LearningPipelineTest` |
| 蓝心大模型调用失败 | 有 fallback 和开发者诊断 | 普通页面 raw code 不够可理解，重试说明不清 | 云端失败映射为未配置、鉴权、网络、超时、返回为空等友好原因；本地结果保留可重试 | `AnalysisSourceReportTest`、真机断网/未配置路径 |

## 仍需真机确认

- 官方 ASR/TTS/OCR 的真实网络返回依赖 AppKey、接口权限和设备环境。
- 不同 ROM 对语音设置 Intent 的支持不同，需确认最少能落到系统设置或应用详情。
- 蓝心真实超时/鉴权失败分类需用真实配置与网络复测。
