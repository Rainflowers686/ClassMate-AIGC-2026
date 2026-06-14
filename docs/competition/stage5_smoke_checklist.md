# Stage 5 最短 Smoke Checklist

目标：Claude 完成 Stage 5 后，用 5 分钟确认主链路、导出中心、多资料输入、Ask This Lesson 和安全边界没有断。测试时不要拍摄本地凭据、调试导入明文或私有配置文件。

## Export Center

| 项目 | 操作 | 预期 | 截图位置 | 失败排查方向 |
|---|---|---|---|---|
| 保存到文件 | 打开一次已有分析结果，进入导出中心，选择保存报告。 | 成功提示包含文件名和路径摘要；文件可打开。 | 导出成功提示、文件管理器列表。 | 检查存储写入方式、文件名安全处理、导出状态提示。 |
| 系统分享 | 在导出中心点击系统分享。 | 出现 Android 分享面板，分享内容是脱敏学习报告。 | 分享面板首屏。 | 检查 Intent 类型、FileProvider 或临时 URI 配置。 |
| PDF | 选择 PDF 导出。 | 若已实现则生成 PDF；若仍未实现，必须显示诚实不可用提示。 | PDF 结果或不可用提示。 | 检查 PdfDocument/Print 路径和错误提示。 |
| HTML | 选择 HTML 导出并打开。 | 包含课程名、知识点、证据、微测、复习任务。 | 浏览器或文件预览。 | 检查 HTML 转义和资料来源摘要。 |
| Markdown | 选择 Markdown 导出。 | Markdown 结构清晰，能看到 Timeline、Quiz、Review。 | 文本预览。 | 检查换行、标题层级、证据 quote。 |
| MindMap | 导出思维导图结构。 | root 是课程标题，子节点是知识点，包含重要度/难度。 | 导出内容中的思维导图段落。 | 检查 MindMapModel 生成和导出模板。 |
| Word 兼容 HTML | 选择 Word 兼容导出。 | 文案诚实说明是 Word 可打开 HTML，不伪装真实 docx。 | 导出选项和结果文件。 | 检查格式说明和文件后缀策略。 |
| Slides HTML | 选择 Slides HTML。 | 生成可演示的分节 HTML，内容不丢失。 | Slides HTML 首屏。 | 检查每页分块、标题和证据摘要。 |

## 本地音视频 / 字幕

| 项目 | 操作 | 预期 | 截图位置 | 失败排查方向 |
|---|---|---|---|---|
| 粘贴音频转写稿 | 复制 `docs/testing/stage5_quick_inputs.md` 中的电磁感应转写稿，作为音频转写输入。 | 进入本节课资料，生成 Timeline、Quiz、Review。 | Import 输入页、Timeline 首页。 | 检查 MaterialBundle source type 和文本拼接。 |
| 粘贴视频字幕 | 复制 C++ 字幕测试文本。 | 时间戳保留为资料证据上下文，分析结果可追溯。 | Timeline 证据页。 | 检查字幕 parser 或 plain text 保留策略。 |
| 时间戳保留 | 查看证据 quote 附近文本。 | 能看到类似 `[00:01:05]` 的时间戳。 | Evidence 详情。 | 检查清洗逻辑是否误删时间戳。 |
| 进入 MaterialBundle | 生成分析后查看资料来源摘要。 | 显示本地音频/视频/字幕来源，非纯粘贴文本。 | 导出报告或分析日志摘要。 | 检查 assembler 输入源映射。 |
| 生成 Timeline | 点击生成知识时间线。 | 结果页显示知识点、证据和微测入口。 | Timeline 首屏。 | 检查 CourseAnalyzer raw text 是否为空。 |

## OCR / PPT / 板书

| 项目 | 操作 | 预期 | 截图位置 | 失败排查方向 |
|---|---|---|---|---|
| 粘贴 PPT OCR | 选择课件/PPT OCR，粘贴数项级数 PPT OCR 文本。 | 可加入本节课资料。 | OCR 输入区和已加入列表。 | 检查空文本 guard、source label。 |
| 粘贴板书 OCR | 选择板书 OCR，粘贴偏序关系板书文本。 | 可加入本节课资料。 | 已加入 OCR 列表。 | 检查 kind 到 source type 映射。 |
| 生成 Timeline | 同时带课堂文本和 OCR 文本生成分析。 | Timeline 结果包含 OCR 资料中的知识点。 | Timeline 知识点列表。 | 检查 MaterialBundle plain text 来源 marker。 |
| Evidence OCR 来源 | 打开证据链。 | 证据显示课件 OCR、板书 OCR 或讲义/PDF OCR 来源。 | Evidence 详情。 | 检查 segment marker 和 UI label 映射。 |
| Ask 引用 OCR 来源 | 对 OCR 文本提问。 | grounded 答案能引用 OCR quote；无依据问题不胡编。 | Ask 回答卡片。 | 检查 Ask parser、EvidenceResolver、fallback 标签。 |

## Ask This Lesson

| 项目 | 操作 | 预期 | 截图位置 | 失败排查方向 |
|---|---|---|---|---|
| grounded | 问“偏序关系需要满足哪三个性质？” | 返回 grounded，引用自反、反对称、传递证据。 | Ask 回答与引用证据。 | 检查 evidence quote 定位。 |
| partial | 问“根值判别法为什么 L=1 时失效？” | 返回 partial，说明本节课只给结论。 | Ask 状态标签。 | 检查 groundedness 降级逻辑。 |
| not_found | 问“C++ 智能指针 shared_ptr 的引用计数怎么实现？” | 返回 not_found 或明确本节课无依据，不编造。 | Ask 回答卡片。 | 检查无证据拒答规则。 |
| 引用证据 | 查看 Ask 引用区域。 | quote 可在 Timeline/Evidence 中找到。 | Ask 引用区域和 Evidence 页。 | 检查 quote normalization。 |
| 不胡编 | 连续问两个超出课堂范围的问题。 | 都不扩展成无依据答案。 | Ask 结果。 | 检查 fallback 回答模板。 |

## 安全

| 项目 | 操作 | 预期 | 截图位置 | 失败排查方向 |
|---|---|---|---|---|
| 不截图凭据 | 录屏前避开调试导入明文和本地配置文件。 | proof 中没有任何本地凭据。 | 录屏目录预览。 | 删除问题截图，重新录制。 |
| 导出脱敏 | 打开导出文件全文搜索敏感类别：鉴权头、令牌前缀、模型输入、消息数组、推理内容字段。 | 全部无命中。 | 搜索结果页。 | 检查导出模板和日志拼接来源。 |
| 不爬取平台 | 测网络视频入口。 | 明确提示不抓取第三方平台，只接受用户有权使用的文本/字幕。 | 入口提示。 | 检查入口文案和点击行为。 |
| 不伪装 ASR/OCR | 测音频、视频、OCR 入口。 | 明确标注当前是粘贴/手动资料或接口预留。 | Import Hub。 | 检查 capability 文案。 |

