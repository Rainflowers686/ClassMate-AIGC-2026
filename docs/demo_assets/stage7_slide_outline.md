# Stage 7 Slide Outline

## 1. 封面：ClassMate AI 课堂学习助手
- 页面目标：建立项目名称和一句话定位。
- 推荐截图：首页 Dashboard 或 Course Detail 顶部。
- 讲解要点：不是普通录音总结，是课堂学习闭环。
- 关键数据/证据：Official BlueLM 主路径、Timeline、Review、Export Center。
- 不要展示：密钥输入、本地配置、完整日志。
- 设计建议：Focus 风格，留白充足，标题大，右侧放 App 首屏截图。

## 2. 痛点：课堂资料碎片化，录音总结不等于学习
- 页面目标：说明为什么只做转写/摘要不够。
- 推荐截图：导入入口和资料篮。
- 讲解要点：文本、课件、板书、字幕、Live 片段分散；学生需要复习和练习。
- 关键数据/证据：多来源导入、MaterialBundle、证据来源 marker。
- 不要展示：真实课堂隐私材料。
- 设计建议：左侧痛点三条，右侧资料碎片示意卡。

## 3. 产品闭环：资料 -> 知识点 -> 证据 -> 微测 -> 复习 -> 练习 -> 导出
- 页面目标：展示 ClassMate 的端到端价值。
- 推荐截图：Timeline、Quiz、Review、Export Center 四连图。
- 讲解要点：每一步都服务学习，不停在摘要。
- 关键数据/证据：学习状态、复习任务、需要多练。
- 不要展示：模型内部交互。
- 设计建议：横向流程图，使用清晰箭头和短标签。

## 4. 官方 BlueLM 合规路径
- 页面目标：解释参赛合规主路径。
- 推荐截图：Settings Provider 配置和诊断 OK。
- 讲解要点：Official BlueLM/qwen3.5-plus 是主路径；Compatible Demo 是展示增强；LocalFallback 是兜底。
- 关键数据/证据：连接测试、qwen guard、secrets scan。
- 不要展示：密钥、完整鉴权内容。
- 设计建议：三列路径卡：Official / Demo / Fallback。

## 5. 多源导入能力
- 页面目标：展示输入地基已扩展。
- 推荐截图：Import Flow、Material Tray、Transcript Editor、OCR 输入。
- 讲解要点：文本、Markdown、手动 OCR、字幕/转写稿、Live 手动和系统 ASR 实验。
- 关键数据/证据：资料篮来源类型。
- 不要展示：真实文件路径、个人材料。
- 设计建议：网格入口卡片，标注“已支持 / 实验 / 待接入”。

## 6. Knowledge Timeline 与证据链
- 页面目标：证明不是简单摘要。
- 推荐截图：Timeline 和 Evidence Detail。
- 讲解要点：知识点、重要性、难度、证据 quote、来源 marker。
- 关键数据/证据：Evidence/Validator。
- 不要展示：供应商原始响应。
- 设计建议：左侧时间线，右侧证据放大卡。

## 7. Ask This Lesson 防胡编问答
- 页面目标：展示 grounded / partial / not_found。
- 推荐截图：三个 Ask 结果。
- 讲解要点：只基于本节课资料回答，无依据不强答。
- 关键数据/证据：引用证据和 groundedness 状态。
- 不要展示：内部构造的模型输入。
- 设计建议：三栏对比：有证据 / 部分依据 / 无依据。

## 8. Quiz / Review / 需要多练
- 页面目标：展示学习闭环。
- 推荐截图：Quiz 答案解释、Review 任务、需要多练入口。
- 讲解要点：答题反馈进入复习任务，练习搜索只打开外部搜索结果。
- 关键数据/证据：ReviewTask、LearningStore、Practice Search。
- 不要展示：第三方平台账号页面。
- 设计建议：复习任务卡 + 搜索词卡。

## 9. Live ASR 实验模式
- 页面目标：诚实展示追平录音转写类输入能力的方向。
- 推荐截图：Live 页面、ASR 实验提示。
- 讲解要点：依赖系统 SpeechRecognizer，不保存原始音频；真实 vivo ASR provider 待接入。
- 关键数据/证据：Live 片段可进入分析。
- 不要展示：录音隐私、声纹身份识别承诺。
- 设计建议：实验模式标识明显，避免写成正式能力。

## 10. Export Center / 打印级 StudyReport
- 页面目标：说明用户能拿到报告。
- 推荐截图：Export Center、PDF/HTML 报告。
- 讲解要点：PDF/HTML/Markdown/TXT/MindMap，保存到文件、下载目录和系统分享。
- 关键数据/证据：报告包含知识点、证据、微测、复习、资料来源和隐私说明。
- 不要展示：本地私有路径、调试原始内容。
- 设计建议：报告预览 + 格式 chip。

## 11. 与听脑类产品对比
- 页面目标：明确定位，不贬低竞品。
- 推荐截图：多源导入、学习闭环、课程库。
- 讲解要点：听脑类强在转写总结；ClassMate 补齐输入地基，优势在学习闭环。
- 关键数据/证据：证据链、微测、复习、练习和导出。
- 不要展示：竞品截图或贬低性文案。
- 设计建议：两列对比，语言克制。

## 12. 决赛路线与安全边界
- 页面目标：总结下一阶段方向。
- 推荐截图：Settings 能力路线图、secrets scan。
- 讲解要点：接真实 vivo ASR/OCR provider，强化多来源证据和学习画像；不爬取平台，不做声纹身份识别，云同步暂缓。
- 关键数据/证据：Proof pack、QA scripts、风险台账。
- 不要展示：任何密钥或未完成能力夸大。
- 设计建议：路线图 + 安全边界 checklist。

