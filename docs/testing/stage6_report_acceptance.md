# Stage 6 打印级学习报告验收清单

目标：验证 Export Center 生成的学习报告适合打印、复习、答辩 proof 和普通用户保存。报告必须是学习材料，不应是 UI、调试数据或模型交互数据的转储。

| 项目 | 操作 | 预期 | 失败排查方向 |
|---|---|---|---|
| PDF 可直接打印 | 在 Export Center 选择 PDF，保存到用户可见目录，用系统 PDF 预览打开并进入打印预览。 | 页面可翻页，标题、正文、知识点、证据和复习内容可读；不依赖 app 私有目录。 | 检查 PDF renderer、分页、字体、文件写入和 MIME。 |
| HTML 浏览器打印 | 导出 HTML，用浏览器打开并进入打印预览。 | 版式适合打印，段落清楚，证据 quote 不被截断。 | 检查 HTML 模板、CSS、转义和浏览器兼容性。 |
| Markdown 适合笔记软件 | 导出 Markdown，使用笔记软件或文本编辑器打开。 | 标题层级清楚，列表可读，适合二次整理。 | 检查 Markdown 标题层级、换行、列表缩进。 |
| TXT 不乱码 | 导出 TXT，用系统文本预览打开。 | 中文不乱码，结构虽然朴素但可读。 | 检查 UTF-8 写入、文件后缀和系统打开方式。 |
| 课程概要 | 打开任一完整报告。 | 包含课程标题、来源、模型/fallback 简述、知识点和题目数量。 | 检查 full report 模板和 CourseSession / provenance 数据。 |
| 核心知识点 | 查看报告知识点部分。 | 每个知识点有标题、摘要、难度/重要度或等价信息。 | 检查 AnalysisResult -> export mapping。 |
| 证据链 | 查看 evidence 部分。 | 能看到来自课堂文本、OCR、字幕或转写稿的可追溯 quote。 | 检查 EvidenceResolver、MaterialBundle marker、导出模板。 |
| 微测题 | 查看 quiz 部分。 | 包含题干、选项、正确答案和解释。 | 检查 quiz export section。 |
| 需要多练清单 | 查看 practice / weakness / need-more-practice 部分。 | 显示需要多练的知识点、触发原因和搜索词。 | 检查 ReviewTask、WeaknessHub、练习搜索词生成。 |
| 复习计划 | 查看 review plan 部分。 | 包含任务、优先级、预计时间和复习原因。 | 检查 ReviewPlanner 和 LearningStore snapshot。 |
| 问这节课 | 查看 Ask This Lesson 部分。 | 包含问答记录、groundedness、证据引用状态；无依据问题不变成外部百科回答。 | 检查 Ask export、groundedness 和 evidence refs。 |
| 资料来源摘要 | 查看 source summary。 | 显示文本、Live、OCR、字幕、材料来源计数，不显示本地完整路径。 | 检查 MaterialSourceSummary 和导出追加逻辑。 |
| 隐私说明 | 查看报告末尾或摘要。 | 明确报告不包含本地凭据、模型交互原始数据或私有配置内容。 | 检查报告模板是否补齐 privacy note。 |
| 不含 UI 转储 | 搜索报告。 | 不出现 Compose/UI tree、按钮状态、屏幕 dump 类内容。 | 检查是否误导出 UI state 或日志。 |
| 不含调试原始信息 | 搜索报告。 | 不出现调试原始包、网络原始体、异常原文堆栈等。 | 检查日志拼接和 export source。 |
| 不含 JSON 转储 | 搜索报告。 | 不出现整段 JSON 对象或数组转储；业务内容应格式化展示。 | 检查 parser/serializer 是否直接写入 export。 |
| 不含模型输入字段 | 搜索报告。 | 不出现模型输入字段原文或消息数组字段原文。 | 检查导出是否误用 provider request。 |
| 不含推理内容字段 | 搜索报告。 | 不出现模型内部推理字段全文。 | 检查 response reader 和 export 安全过滤。 |
| 不含鉴权/令牌字段 | 搜索报告。 | 不出现鉴权头字段、令牌前缀或真实应用凭据。 | 检查 SafeExportText、ExportCenter、history/learning store。 |
| 不含应用标识/密钥/API 凭据 | 搜索报告。 | 不出现真实应用标识、应用密钥或 API 凭据。 | 立即停止 proof 录制，检查配置导入、日志、导出模板。 |

## 验收建议

先用本地 fallback 或 fake 数据跑一次格式验收，再用真实 BlueLM 跑一次内容质量验收。若 PDF 中文排版不稳定，以 HTML/Markdown 作为主要 proof，PDF 作为基础版补充。

