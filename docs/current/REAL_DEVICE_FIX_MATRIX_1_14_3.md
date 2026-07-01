# Real Device Fix Matrix - ClassMate 1.14.3

版本：`1.14.3 / 116`

本页补充 1.14.2 后的真机修复点。完整历史矩阵见 [REAL_DEVICE_FIX_MATRIX_1_14_2.md](REAL_DEVICE_FIX_MATRIX_1_14_2.md)。

## 1.14.3 增量修复矩阵

| 问题 | 当前修复 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| 开始录音并实时转写后提示系统无语音识别服务 | 显示“打开语音设置”，并继续保留录音保存、手动转写、字幕导入 fallback | `SpeechRecognitionDiagnosticsTest` + 真机无服务 ROM | 设置 action 是否直达语音服务页 |
| 题目/证据反馈只记录、不改变学习内容 | 反馈会即时生成替换题、替代证据提示或重写知识点摘要 | `LearningLoopRefinementEnginesTest`、`PracticeFlowTest` | 真机反馈后微测候选刷新 |
| 题目解析只有答案、不够可学 | 替换题包含答案详解、选项解释和证据摘录 | `LearningLoopRefinementEnginesTest` | 长材料真实题质量 |
| 复习计划像技术统计 | 页面主文案改为本课知识点复习，显示核心/相关知识点和 evidence 摘录 | `ReviewPlanKnowledgeSummaryTextTest` | 小屏幕展示 |
| 课程总结没有基于本课知识点继续关联 | 新增课程内 related knowledge summaries，只从当前 snapshot 知识点/evidence 汇总 | `LearningLoopRefinementEnginesTest` | 长课程质量 |

## 复测建议

1. 在系统 SpeechRecognizer 不可用的设备上开始录音，确认按钮和 fallback 文案。
2. 对错题提交“不准确”反馈，确认新的题目进入微测候选且包含详解。
3. 对知识点提交“不清楚”反馈，确认复习页摘要和复习任务更新。
4. 打开知识页和复习页，确认相关知识点摘要只引用当前课程证据。
