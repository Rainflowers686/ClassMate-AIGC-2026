# Real Device Fix Matrix - 1.14.11 / 124

| 真机问题 | 修复策略 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| fresh-install 新材料仍失败 | 新增 fresh-install 空 store 端到端回归，覆盖 OCR -> 课程 -> 复习 -> 时间线微测 -> 完成练习 | `FreshInstallLearningFlowRegressionTest.freshOcrCourseKeepsRawTextButBuildsCleanSubjectLearningFlow` | 真机重新安装后导入新材料是否同样通过 |
| OCR 正确率下降 | OCR raw/normalized/candidate 三层分离，SubjectKnowledgeExtractor 不再作用于 OCR 草稿文本 | `OcrTextPostProcessorTest.normalizationDoesNotDropSubjectTextOnPromptLine`、`ImageDraftFlowTest.ocrDraftSeparatesRawNormalizedAndSubjectCandidates` | 官方 OCR provider 输入图像质量和真实返回仍需真机看图验证 |
| 复习计划仍显示课堂强调词 | fresh 生成路径的学科标题和 summary 展示句清理“下面看/重点来了/作业截图上传”等提示语 | `SubjectKnowledgeExtractorTest.mixedFreshOcrKeepsSubjectCandidateButCleansPromptFragments` + fresh E2E | 真机真实 OCR 噪声样式是否还有未覆盖提示语 |
| 课程总结/相关知识点不准 | 只从 cleaned subject candidates 和 evidence-bound knowledge points 进入用户可见总结；evidence 可保留原句 | fresh E2E 检查 summary / related / reviewQueue | 真实多学科材料仍需人工抽样复核 |
| 微测题仍不是知识点习题 | fresh E2E 从知识点时间线启动 Practice，断言题目绑定知识点和 evidence，不显示提示语主题 | fresh E2E + 1.14.10 PracticeSession repair tests | 真机 UI 两个入口题目数量是否一致 |
| 完成练习仍自动退出 | fresh E2E 完成全部题后断言仍在 `Screen.PRACTICE` 且有完成结果 | fresh E2E + 旧数据完成练习测试 | 真机点击底部按钮/系统返回的具体行为 |

## 不变边界

- BlueLM/qwen3.5-plus 1.14.9 参数映射保持不变。
- 本轮没有新增 provider 或读取本地密钥。
- evidence 原文可以包含课堂原句；禁止的是把提示语作为知识点标题、相关知识点 anchor、复习任务或微测主题。
