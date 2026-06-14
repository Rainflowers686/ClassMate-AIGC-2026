# ClassMate AI 功能工程边界

原则：每个核心功能都可以有 AI 增强，但不能让每个按钮直接裸调用模型。AI 只能在明确输入、输出、证据、provider path、日志脱敏和 fallback 策略下工作。

| 功能 | 输入 | AI 角色 | 不能让 AI 做什么 | 防胡编 | provider path 日志 | 端侧适配 |
|---|---|---|---|---|---|---|
| Course analysis | MaterialBundle plain text、来源 marker | 生成 compact draft | 不直接写最终内部对象，不绕过 validator | EvidenceResolver + ResultValidator | profile、model、短错误 | 长文本暂缓 |
| Ask This Lesson | 课程标题、知识点、证据片段、问题 | 生成证据约束答案 | 不回答课堂外无依据问题 | quote 定位失败降级 | groundedness、fallback | 适合 |
| Quiz explanation | 题目、选项、证据、用户答案 | 解释为什么对错 | 不改正确答案，不编证据 | 固定答案 + evidence refs | provider、latency | 适合 |
| Review suggestion | LearningStore、任务状态、反馈 | 给复习建议 | 不直接改调度规则 | 规则先行，AI 只解释 | suggestion_source | 适合 |
| Practice explanation | 知识点、错误类型、搜索词 | 解释练习方向 | 不爬取平台，不下载内容 | 白名单搜索 + 本地说明 | practice_provider | 适合 |
| StudyReport / PDF | 已验证结构化结果 | 摘要和下一步建议 | 不导出完整模型交互 | Export sanitizer | report_profile | 适合 |
| OCR/ASR postprocess | 用户授权材料和识别文本 | 清洗、术语纠错 | 不伪造时间戳、speaker 或 OCR 结果 | 来源 marker | material_source | 后续 |
| Course Library summary | 历史记录聚合 | 生成短摘要 | 不重新分类敏感信息 | 本地聚合优先 | summary_provider | 适合 |
| Learning profile | 答题、反馈、任务状态 | 提供学习画像解释 | 不存敏感原文，不改规则状态 | counters + rules | learning_profile_source | 适合 |
| Export / Share safety | 导出内容 | 可选安全检查 | 不保留密钥、完整模型交互或内部推理字段 | sanitizer + audit | export_safety | 适合接文本审核 |

## 通用要求

- AI 生成结果必须落在明确 DTO 或受控文本区。
- 任何最终学习结果仍经过现有 validator。
- provider path 必须可见，不能把 fallback 包装成主路径成功。
- 日志只记录短标签、数字、布尔和错误类型。
- 导出内容必须通过敏感词与结构化安全检查。
- 端侧模型适合小任务、解释、摘要和隐私兜底，不适合作为 Stage 8A 默认长文本分析主路径。
