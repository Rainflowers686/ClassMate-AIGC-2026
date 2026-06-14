# OCR / PPT / 板书资料导入验收清单

用途：Stage 4D 真机测试和答辩前验收。当前目标是验证“手动 OCR / 接口预留”的资料接入、来源标记、证据链、问答和导出表现；不验证真实 OCR 调用。

| # | 必测项 | 操作 | 期望 | 截图位置 | 失败时排查方向 |
|---|---|---|---|---|---|
| 1 | 课件 OCR 文本可加入本节课资料 | 在 Import Hub 选择 PPT / 课件图片入口，粘贴高数课件 OCR 文本 | 资料被加入，来源显示为课件 OCR 或等价标签 | Import Hub 资料列表 | 来源类型映射、空文本校验、资料列表状态 |
| 2 | 板书 OCR 文本可加入本节课资料 | 粘贴“物理板书 OCR：楞次定律方向判断” | 资料被加入，来源显示为板书 OCR | Import Hub 资料列表 | 板书入口标识、MaterialBundle source type |
| 3 | PDF / 讲义 OCR 文本可加入本节课资料 | 选择 PDF / 讲义截图入口，粘贴一段讲义 OCR 文本 | 资料被加入，来源显示为讲义 OCR 或导入 OCR | Import Hub 资料列表 | PDF 入口占位、source label 文案 |
| 4 | 无 OCR 文本时不能误加入 | 不输入 OCR 文本，点击加入 | 不创建资料条目，显示需要先提供文本 | Import Hub toast 或空状态 | 空字符串 trim、按钮 enabled 条件 |
| 5 | OCR 来源进入 Timeline 分析 | 加入 OCR 资料后生成 Timeline | 时间线包含可由 OCR 文本支持的知识点 | Timeline 页面 | fusion 输入、CourseAnalyzer 输入构造、来源合并 |
| 6 | Evidence 能引用 OCR 片段 | 打开由 OCR 支持的知识点证据 | 证据 quote 能定位到 OCR 片段，来源可见 | Evidence 页面 | EvidenceResolver、source segment mapping |
| 7 | Ask This Lesson 能引用 OCR 来源 | 对 OCR 中明确出现的问题提问 | 答案为 grounded，证据来自 OCR 片段 | Ask 页面答案和证据区 | Ask evidence refs、groundedness 降级逻辑 |
| 8 | Export 显示 OCR 来源摘要 | 导出学习报告 | 报告显示资料来源摘要和 OCR 证据，不含调试内容 | 导出文件预览 | ContentExporter、source summary serialization |
| 9 | 不上传文件 | 选择本地图片或 PDF，仅记录元数据或粘贴文本 | 不出现上传动作或外部网络提示 | Import Hub 文件条目 | picker 处理、transport 调用边界 |
| 10 | 不爬取网络平台 | 点击网络视频链接入口 | 提示不抓取平台内容，只允许用户提供有权使用的文本或字幕 | Import Hub link 卡片 | link 入口文案、无网络调用路径 |
| 11 | 不暴露本地敏感路径 | 选择本地文件后查看 UI 和导出 | 只显示文件名、类型、大小摘要，不显示完整私人路径 | Import Hub、Export | metadata summary、path redaction |
| 12 | 不泄漏密钥 | 查看日志、导出、History | 不出现任何访问凭据或敏感配置内容 | Settings logs、Export、History | redaction、debug preview、export fields |
| 13 | 不新增危险权限 | 检查 Manifest 或权限列表 | 没有新增录音、相机、存储等危险权限；系统 picker 不需要危险权限 | Manifest 检查结果 | AndroidManifest、permission merge |
| 14 | 错字 OCR 不直接变成错误证据 | 使用错字版本 OCR 文本 | 系统可保留原始 OCR 证据；若无法定位，不伪造 quote | Evidence 页面 | OCR cleanup、evidence resolve、validator |
| 15 | PPT 页顺序可控 | 加入两段课件 OCR 文本 | 资料顺序与用户添加顺序一致，或 UI 明确可调整 | Import Hub 资料列表 | bundle ordering、timestamp/order index |

## 推荐 smoke 顺序

1. 粘贴高数课件 OCR，生成 Timeline，查看 Evidence。
2. 粘贴物理板书 OCR，Ask 一个 grounded 问题。
3. 粘贴离散数学课件 OCR，导出报告，检查资料来源摘要。
4. 使用错字版本 OCR，确认系统不伪造证据。
5. 点击网络视频链接入口，确认只显示不抓取说明。
