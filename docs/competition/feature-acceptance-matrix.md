# 功能验收矩阵

状态定义：

- 已完成：已有功能与基本验证。
- 需真机验证：已实现，但需要更多手机端 smoke 和截图。
- 占位：有入口、文案、数据结构或路线图，不声称完成。
- 暂缓：当前阶段明确不做。

| 模块 | 功能 | 状态 | 入口 | 依赖 | 是否联网 | 是否涉及密钥 | 是否已有测试 | 验收步骤 | 失败时截图位置 | 风险等级 | 下一步 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Provider | BlueLM official | 已完成 | Settings / official_bluelm | vivo AIGC 文本模型 | 是 | 是，masked | 是 | 导入本地配置后运行连接测试和课堂分析 | Settings diagnostic / redacted log | 高 | 真机长文本回归。 |
| Provider | qwen3.5-plus enable_thinking=false | 已完成 | BlueLM 请求构造 | qwen3.5-plus | 是 | 否 | 是 | 本地查字段并跑请求构造测试 | 命令输出 | 中 | 防回归。 |
| Provider | Compatible demo | 已完成 | Settings / demo_compatible | 兼容接口配置 | 是 | 是，masked | 是 | 运行 Compatible diagnostic | Settings diagnostic | 中 | 始终标注 demo。 |
| Provider | LocalFallback | 已完成 | local_only / fallback | 本地规则 | 否 | 否 | 是 | local_only 分析文本 | redacted log / Timeline provenance | 低 | 保持兜底标签。 |
| Import | paste | 已完成 | Import Hub | 文本输入 | 分析阶段可能联网 | 否 | 是 | 粘贴公开文本并分析 | Import / Timeline | 低 | 增加样例脚本。 |
| Import | txt/md | 需真机验证 | Import Hub 文件选择 | Android 文档选择器 | 分析阶段可能联网 | 否 | 是 | 导入 txt/md 并分析 | Import / file picker | 中 | 测不同文件管理器。 |
| Import | audio/video/OCR/link placeholder | 已完成 | Import Hub 占位卡 | 无 | 否 | 否 | 是 | 点击占位入口 | Toast / placeholder card | 低 | 接官方能力前保持占位。 |
| Live | Live Companion manual | 需真机验证 | Home -> Live | 手动片段 | 生成分析时可能联网 | 否 | 是 | 开始、片段、暂停、结束 | Live status card | 中 | 录制 proof。 |
| Live | Live -> Timeline | 需真机验证 | Live Generate timeline | Provider chain | 视 profile 而定 | 视 profile 而定 | 是 | 结束课堂后生成 Timeline | Analyze / Timeline | 中 | 真机完整闭环。 |
| Live | Live -> History | 需真机验证 | 分析完成后 History | HistoryStore | 否 | 否 | 间接 | 完成 Live 分析后查 History | History recent record | 中 | 多课程回归。 |
| Live | Live -> LearningStore | 需真机验证 | Review | LearningStore | 否 | 否 | 间接 | Live 分析后进入 Review | Review task | 中 | 观察任务生成。 |
| Timeline | Knowledge Timeline | 已完成 | Timeline | 分析结果 | 否 | 否 | 是 | 查看知识点列表 | Timeline | 中 | 真实课程质量评审。 |
| Evidence | Evidence chain | 已完成 | Timeline -> Evidence | EvidenceResolver/Validator | 否 | 否 | 是 | 打开知识点证据 | Evidence detail | 高 | 继续防伪证据。 |
| Quiz | Quiz | 已完成 | Timeline -> Quiz | Quiz data | 否 | 否 | 是 | 答题并查看解释 | Quiz result | 中 | 教育价值审计。 |
| Review | ReviewTask | 已完成 | Review | LearningStore | 否 | 否 | 是 | 查看任务和优先级 | Review task card | 中 | 调优优先级。 |
| Review | Weakness Hub | 需真机验证 | Review | ReviewTask counters | 否 | 否 | 是 | 触发答错/反馈后查看 | Weakness Hub | 中 | 真机截图。 |
| History | History | 已完成 | History tab | HistoryStore | 否 | 否 | 是 | 重新打开记录 | History / Timeline | 低 | 长列表体验。 |
| History | Course Library | 需真机验证 | History | History 聚合 | 否 | 否 | 是 | 多次同课程分析后查看 | Course card/detail | 中 | 优化课程归一。 |
| Ask | Ask This Lesson | 需真机验证 | Timeline | 当前课程结果 | 当前骨架本地；未来可能联网 | 否 | 是 | 提问并查看 groundedness | Ask card | 中 | 后续接受控模型问答。 |
| Export | Export | 已完成 | Home / History / Review | Exporter | 否 | 否 | 是 | 导出 md/html/txt | Export toast / file preview | 高 | 敏感词检查。 |
| Export | MindMap | 已完成 | Export report | MindMapModel | 否 | 否 | 是 | 导出后查看 Mind map | Report section | 低 | 后续图形 UI。 |
| Review | Video white-list recommendations | 已完成 | Review / Export | 白名单搜索 | 点击外部搜索时联网 | 否 | 是 | 触发弱点后点击搜索 | Browser search page | 中 | 不爬取、不下载。 |
| Settings | Capability Roadmap | 已完成 | Settings | Roadmap data | 否 | 否 | 是 | 查看三类路线图 | Roadmap card | 低 | 答辩截图。 |
| Settings | BuildInfo | 已完成 | Settings | BuildInfo | 否 | 否 | 间接 | 查看版本和 commit | Build card | 低 | 发布前确认。 |
| Theme | Focus theme | 已完成 | Settings theme | Compose theme | 否 | 否 | 是 | 切换并检查主要页面 | Home / Timeline | 低 | 默认主题。 |
| Theme | Flow theme | 需真机验证 | Settings theme / Live | Compose theme | 否 | 否 | 是 | 切换 Flow 查看 Live | Live / Home | 中 | 局部增强。 |
| Theme | Vitality theme | 需真机验证 | Settings theme | Compose theme | 否 | 否 | 是 | 切换 Vitality 查看主流程 | Home / Review | 低 | 后置微调。 |
| Security | Privacy/security | 已完成 | Settings / docs | 安全规则 | 否 | 否 | 是 | 查看文案并跑检查 | Settings / command output | 高 | 发布前复核。 |
| Security | Secrets scan | 已完成 | scripts | 扫描脚本 | 否 | 否 | 是 | 跑 PowerShell scan | 终端输出 | 高 | 每次提交前跑。 |
| CI | GitHub Actions | 需真机验证之外 | GitHub | Actions | 是 | 否 | 是 | 查看 Android CI / Secrets Scan | GitHub Actions 页面 | 中 | 提交后确认绿。 |
