# ClassMate 1.14.10 / versionCode 123

版本主题：修复旧课程学习 artifact 与微测入口一致性。

## 修复内容

- 旧课程打开时自动修复已保存的 L3 snapshot、课程总结、相关知识点、复习队列和微测题。
- 最终 UI 渲染前增加防线，课堂强调词和 raw 技术词不能作为知识点、相关知识点或微测主题展示。
- 课程详情、知识点时间线、复习计划、证据页和反馈后的微测入口统一使用同一条 PracticeSession 构建路径。
- 旧垃圾题进入练习前会被过滤或由 evidence-backed L3 questions 修复；确实材料不足时显示可靠空态。
- 完成练习后停留在练习页，展示完成摘要，由用户主动返回。

## 保留能力

- 继续保留 1.14.9 的 BlueLM/qwen3.5-plus 三档模式和长超时策略。
- BlueLM 可用时仍优先进入主链路；失败后才使用本地整理版。
- 本地 fallback 不冒充 BlueLM。

## 风险

- 旧数据自动修复基于本课已有 evidence/OCR/transcript/file/manual text；如果材料本身不足，仍需要用户补充资料或手动修正 OCR 文本。
- 官方网络能力仍需真实 AppKey、网络、权限和接口状态真机确认。
