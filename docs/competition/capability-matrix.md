# Capability Matrix

分类：

- A. 已实现并验证：已有代码能力，并有自动化测试或本地验证。
- B. 已实现但需真机继续验证：已有功能入口/实现，需要更多真机截图和体验验证。
- C. 已留接口/占位：有产品入口、数据结构或路线图，但不声称完成。
- D. 不做/暂缓：当前阶段明确不做或高风险暂缓。

| 能力 | 分类 | 入口 | 是否联网 | 是否有测试 | 风险 | 下一步 |
|---|---|---|---|---|---|---|
| BlueLM 文本生成 | A | ProviderResolver / official_bluelm / Settings diagnostic | 是 | 是 | 真机网络、配额、模型输出格式 | 继续真机长课堂 smoke，保留安全日志。 |
| qwen3.5-plus enable_thinking=false | A | BlueLM request body / diagnostic request body | 是 | 是 | 回归时误删字段导致超时 | 每次回归跑 Select-String 和请求构造测试。 |
| Compatible demo | A | demo_compatible profile / Settings diagnostic | 是 | 是 | 被误认为官方路径 | UI/文档持续标注 demo enhancement。 |
| LocalFallback | A | local_only / fallback chain | 否 | 是 | 不能被包装成官方模型能力 | 保持兜底标签清晰。 |
| Import paste | A | Import Hub | 否，分析时按 profile 可能联网 | 是 | 用户粘贴隐私文本 | 提醒只导入允许处理的课堂文本。 |
| Import txt/md | B | Import Hub 文件选择 | 否，分析时按 profile 可能联网 | 是 | Android 文件 URI 兼容性 | 真机测试不同文件管理器。 |
| audio/video/OCR/link import placeholder | A | Import Hub 占位卡片 | 否 | 是 | 文案误导为已实现 | 保持“暂未接入/可先粘贴”。 |
| Live Companion manual | B | Home -> Live Companion | 否，生成分析时按 profile 可能联网 | 是 | 用户误解为真实 ASR | 页面持续显示手动/模拟说明。 |
| ASR future | C | Capability Roadmap / Live seam | 未来可能联网 | 是，文案测试 | 被误称完成 | 决赛接 vivo 长语音/短语音前保持占位。 |
| OCR future | C | Import Hub / Capability Roadmap | 未来可能联网 | 是，文案测试 | 被误称完成 | 接 vivo OCR 后再启用真实入口。 |
| Timeline | A | Knowledge Timeline | 否 | 是 | 模型输出弱时知识点质量 | 继续真实高数课程验证。 |
| Evidence chain | A | Evidence detail / validators | 否 | 是 | evidenceQuote 定位失败 | 不伪造证据，失败则剔除或 fallback。 |
| Quiz | A | Timeline -> Quiz | 否 | 是 | 题目教育价值需人工评审 | 继续 Claude/人工审计题目质量。 |
| ReviewTask | A | Review | 否 | 是 | 任务过多或优先级不稳 | 真机测试反馈后调度表现。 |
| LearningStore persistence | A | Review / app data store | 否 | 是 | 删除历史与学习任务边界 | 保持删除 History 不删除 LearningStore。 |
| HistoryStore persistence | A | History | 否 | 是 | 历史数据过多、标题不规范 | 后续加搜索/筛选。 |
| Course Library | B | History -> Course library | 否 | 是 | 课程名轻量归一误分组 | 真机多课程数据验证。 |
| Ask This Lesson | B | Timeline -> Ask This Lesson | 当前本地兜底否；未来 provider 问答可能是 | 是 | 当前未接模型问答主路径 | 后续接受控 JSON 问答 provider，保留证据校验。 |
| Weakness Hub | B | Review -> Weakness Hub | 否 | 是 | 优先级策略仍需体验调优 | 真机做错题/反馈后观察。 |
| Export md/html/txt | A | Home / History / Review | 否 | 是 | 导出内容泄漏敏感字段 | 每次发布前搜索敏感词。 |
| MindMap | A | Export / report model | 否 | 是 | UI 图形化尚未强化 | 后续可加树状或图形视图。 |
| Video white-list recommendation | A | Review task / Export | 打开外部搜索时联网 | 是 | 版权/抓取风险 | 只保留白名单搜索，不爬取下载。 |
| Capability Roadmap | A | Settings | 否 | 是 | 文案夸大 | 持续区分 Connected / Planned / Demo。 |
| BuildInfo | A | Settings | 否 | 间接验证 | commit/builtAt 展示不准确 | 构建前确认 BuildInfo。 |
| privacy/security | A | Settings / docs / scans | 否 | 是 | 真实 key 外泄 | secrets scan + 人工检查 proof。 |
| Flow theme | B | Settings theme / Live context | 否 | 主题测试 | 不应伪装白噪音真实音频 | Flow 局部增强，真实音频后置。 |
| Focus theme | A | Settings theme / 默认体验 | 否 | 是 | 视觉继续打磨 | 作为默认主线。 |
| Vitality theme | B | Settings theme | 否 | 是 | 可能偏花哨 | 作为可选活力主题后置微调。 |

## 当前结论

Stage 3 前的产品完整度重点已经从“能分析”扩展到“像学习 App”：导入、课堂伴学、课程库、问本节课、复习薄弱点、导出和路线图都有可说明入口。下一步应优先做真机 smoke、proof 截图和真实高数课堂样例质量审计。
