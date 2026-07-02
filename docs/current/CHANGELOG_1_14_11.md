# ClassMate 1.14.11 / versionCode 124

版本主题：保护 OCR 原文，并修复 fresh-install 新材料学习闭环。

## 修复内容

- OCR 三层分离：
  - `rawOcrText` 保留官方/端侧 provider 原始返回，供预览、重新核对和 evidence 使用。
  - `normalizedOcrText` 只做轻量排版清洗，保留题号、公式、单位、项目符号和同句学科内容。
  - `subjectKnowledgeCandidates` 只在知识点提取阶段过滤课堂提示语，不影响 OCR 草稿和用户手动编辑文本。
- 修复 fresh-install 新课程主流程：新装、空 store、新图片 OCR 材料也会从 accepted subject knowledge points 生成课程总结、相关知识点、复习计划和微测。
- 增强学科候选清理：处理“下面看牛顿第二定律”“这个地方可能考”“作业截图上传”等真机 OCR 常见提示语，不把它们作为知识点标题或微测主题。
- 课程总结和复习计划的用户可见句子走展示清理；evidence 原文仍可保留 OCR 原句，便于追溯。
- 新增 fresh-install 端到端回归测试，覆盖 OCR 草稿、课程知识点、相关知识点、复习计划、知识点时间线微测和完成练习停留。

## 保留能力

- 保持 1.14.9 的 BlueLM/qwen3.5-plus 三档模式、长超时和 `reasoning_content` 隐藏策略。
- 保持 1.14.10 的旧课程 artifact repair 和统一 PracticeSession 构建路径。
- 本地 fallback 继续可用，但不冒充 BlueLM。

## 风险

- OCR 真实识别准确率仍依赖图片清晰度、官方 OCR 配置、网络和接口权限。
- 本轮保证 App 内不会因学科过滤二次破坏 OCR 原文；低质量图片仍建议人工修正后再生成课程。
