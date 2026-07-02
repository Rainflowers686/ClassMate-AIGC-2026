# Real Device Fix Matrix - 1.14.10 / 123

| 真机问题 | 修复策略 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| 完成练习后自动退出 | ViewModel 完成练习只写入 `practiceResult`，不自动导航；页面保留完成摘要 | `LearningArtifactRepairIntegrationTest.completingPracticeKeepsSessionUntilExplicitExit` | 真机点击“完成练习”后是否停留在练习页 |
| 复习计划显示课堂强调词 | 旧 snapshot 打开时由 `LearningArtifactRepairer` 修复；UI state 进入练习前再过滤 | `legacyDirtySnapshotIsRepairedBeforeUserSurfaces` | 旧课程历史记录是否自动净化 |
| 课程总结/相关知识点不准 | 从 accepted subject knowledge points + evidence 重建 summary / related knowledge | `oldHistoryOpenAndCoursePracticeUseRepairedArtifacts` | 真实 OCR 旧课程是否能提取出学科点 |
| 微测题不是具体知识点习题 | 旧题进 PracticeSession 前过滤，缺失时用修复后的 L3 evidence-backed questions 构建 | `PracticeFlowTest` + 旧数据集成测试 | 旧题库和反馈替换题是否同样稳定 |
| 证据页有题但时间线开始微测无题 | 统一从修复后的 course result 与 L3 questions 构建 PracticeSession；core/L3 id 不一致时兜回 L3 | `oldHistoryOpenAndCoursePracticeUseRepairedArtifacts` | 两个真机入口题目数量是否一致 |

## 不变边界

- BlueLM/qwen3.5-plus 1.14.9 参数映射保持不变。
- 系统 SpeechRecognizer 不作为 ASR 主路线。
- 真实官方能力成功仍依赖配置、网络和接口权限。
