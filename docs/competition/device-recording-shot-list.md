# 真机录屏分镜表

用途：复赛/答辩录屏、配音、现场演示排练。  
录屏前准备：开启勿扰；隐藏或裁掉状态栏隐私；不要录到真实密钥输入；Debug import 页面如有密钥输入，先清空或不要进入该输入区域。

| 镜头 | 页面 | 操作 | 画面重点 | 讲解词 | 风险提醒/不要拍到什么 |
|---|---|---|---|---|---|
| 1 | App 启动首页 | 打开 App，停留 3 秒 | ClassMate 首页、当前模式、入口清晰 | “ClassMate 是课堂学习助手，核心是学习闭环，不是普通总结工具。” | 不要露出通知、账号、私人状态栏信息。 |
| 2 | Settings 模型状态 | 进入 Settings | Official BlueLM / Compatible Demo / Local Fallback | “官方 BlueLM 是参赛合规主路径，Compatible 只是展示增强，LocalFallback 保证可用。” | 不进入或不拍 debug 密钥输入框。 |
| 3 | BlueLM diagnostic | 点击 Test BlueLM connection | provider=BLUELM、status=OK、HTTP 结果 | “连接测试只显示安全诊断，不展示真实密钥。” | 不拍到 AppID/AppKEY 明文。 |
| 4 | Import 粘贴文本 | 进入 Import Hub，粘贴公开示例文本 | 文本输入、字数、生成按钮 | “支持粘贴课堂文本，也支持 txt/md 导入。” | 不使用隐私课堂全文。 |
| 5 | 分析中 | 点击生成时间线 | Analyze 进度 | “系统按 provider profile 调用主链路。” | 不展示完整请求内容。 |
| 6 | Knowledge Timeline | 分析完成后停留 | 知识点数、微测数、原文段数、provider chip | “输出是结构化知识点时间线，不只是摘要。” | 避免长时间展示隐私原文。 |
| 7 | Evidence 证据链 | 点击知识点证据 | 原文证据片段、高亮/引用 | “每个关键结论都能回到课堂原文证据。” | 不展示敏感课堂内容。 |
| 8 | Quiz 微测 | 进入 Quiz，答一题 | 选项、正确/错误反馈、解释 | “微测服务理解、判断和应用，不是文本匹配。” | 不拍隐私学生姓名或个人信息。 |
| 9 | Review 复习计划 | 进入 Review | 今日待复习、任务原因、优先级 | “答题和反馈会进入复习调度。” | 不拍任何密钥或日志输入框。 |
| 10 | Weakness Hub | 在 Review 顶部/弱点区停留 | 薄弱点、建议行动 | “反复答错、太难、需要例题会进入薄弱点中心。” | 仅展示公开样例课程。 |
| 11 | Live Companion 手动课堂 | 进入 Live，输入标题，Start | 手动/模拟转写说明、计时 | “当前 Live 是手动/模拟转写演示，暂未接入真实 ASR。” | 不能出现录音权限弹窗；不能说已完成 ASR。 |
| 12 | Live 添加片段 | 添加 2 条片段，暂停/继续 | 片段数、最近片段 | “它为未来 ASR 留好了 transcript session 结构。” | 不输入隐私课堂内容。 |
| 13 | Live 结束生成时间线 | End class 后点击 Generate timeline | Live -> Analyze -> Timeline | “结束课堂后复用同一套分析主链路。” | 不展示完整请求或模型返回。 |
| 14 | History 最近记录 | 进入 History | Recent lessons | “每次分析都会保存历史记录。” | 删除操作前确认不误删需要展示的数据。 |
| 15 | Course Library | 点击课程卡 | 课程聚合、课堂次数、知识点/微测/复习任务 | “Course Library 把历史记录提升为课程级学习结构。” | 不展示私人课程名。 |
| 16 | Export 学习报告 | 点击导出 | 文件名、路径摘要、报告章节 | “导出包含时间线、证据、微测、复习和薄弱点。” | 导出文件只展示脱敏内容；不要展示本机隐私路径。 |
| 17 | Capability Roadmap | Settings -> Roadmap | Connected / Planned / Demo | “已接入和规划中能力被清楚区分，不夸大 ASR/OCR。” | 不说未完成能力已完成。 |
| 18 | local_only fallback 可选 | 切到 local_only 后分析短文本 | LocalFallback 标记、零网络说明 | “网络不可用时仍可保底，但会清楚标注来源。” | 不把 LocalFallback 说成官方模型输出。 |
| 19 | Compatible Demo 可选 | 切到 demo_compatible 并测试 | Compatible demo 状态 | “Compatible 是展示增强，不影响官方合规主路径。” | 不展示第三方 key；不说它是参赛主路径。 |

## 录屏后检查

- 视频中不出现真实 AppID/AppKEY/API key。
- 不出现认证头明文。
- 不出现完整请求上下文、厂商原始返回或模型思考字段。
- 不出现私人通知、账号、手机号、本机路径等隐私。
- 所有 ASR/OCR 相关描述都是“规划中/占位/后续接入”。
