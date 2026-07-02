# ClassMate 1.14.9 真机问题修复矩阵

| 问题 | 当前修复 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| 点击完成练习自动退出 | 完成后停留在 Practice 页，显示摘要和显式返回按钮 | `PracticeFlowTest.practiceCompletionStaysOnPracticeScreenUntilExplicitReturn` | 真机点击最后一题“完成练习”后页面停留 |
| 复习计划总结课堂强调词 | L3 pipeline、ReviewPlan、RelatedKnowledge、Quiz 共用 accepted subject knowledge points | `L3LearningPipelineTest.physicsOcrNoiseDoesNotReachReviewSummaryRelatedKnowledgeOrQuiz` | OCR 真图中“同学们注意/重点来了”不成为知识点 |
| 课程总结/相关知识点不准 | 相关知识从课程已接受知识点出发，回到本课 evidence/OCR/transcript/file/manual text 检索 | `LearningLoopRefinementEnginesTest` 和 1.14.9 物理样例 | 真机课程页与复习页相关知识一致 |
| 微测题是关键词题 | 题目进入 UI 前必须绑定知识点、证据、答案详解和错误项解释 | `QuizRelevanceGateTest`、`PracticeFlowTest` | 真机题目围绕学科点，不围绕提示语 |
| 两个微测入口不一致 | 课程详情统计、知识点时间线、复习入口统一 `startPractice(PracticeMode.QUICK_REVIEW)` | `PracticeFlowTest.courseAndKnowledgeQuizEntriesUseUnifiedPracticeSource` | 证据页有题时，知识时间线开始微测也有题 |
| BlueLM 思考模式错误 | qwen3.5-plus 三档映射 low/medium/high + enable_thinking false/false/true | `CloudModelQualityProfileTest`、`VivoOpenAIChatProtocolTest`、`VivoRequestFactoryTest` | 有 AppKey 时请求不再因 `reasoning_effort=max` 被拒 |
| BlueLM 正式请求超时过短 | 快速约 5 分钟、均衡约 6 分钟、专业约 10 分钟；dry-run 仍短超时 | `AnalysisIntensityTest`、`BlueLMProviderTest`、`PolishedStudyPackTest` | 专业模式长等待有取消，失败保留本地结果 |
| 用户看到实际模型名 | 设置页和用户报告显示“蓝心 / 蓝心大模型”，不展示 qwen 模型名 | `SettingsModelConfigTextTest`、用户页泄漏守卫 | 真机设置/导出无底层模型名 |

## 真机复测重点

1. 导入含课堂强调词和物理公式的图片，确认复习计划只显示学科知识点。
2. 从课程详情和知识点时间线分别进入微测，确认题目来源一致。
3. 完成一次练习，确认页面不自动退出。
4. 选择专业模式触发 BlueLM 课程分析或精修导出，确认页面显示长等待和取消，失败时保留本地结果。
