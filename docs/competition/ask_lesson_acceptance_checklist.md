# Ask This Lesson 验收清单

本清单用于真机 smoke test 和答辩前验收。不要截图任何真实密钥、本地配置文件、完整模型输入、请求消息、供应商原始返回或内部思考文本。

| # | 必测项 | 操作 | 预期 | 截图位置 | 失败时该查哪里 |
|---|---|---|---|---|---|
| 1 | 空问题按钮禁用 | 打开已分析课程的 Ask This Lesson，不输入问题 | 提交按钮不可用，或点击后只提示先输入问题 | Ask 页面输入框和按钮 | UI state、按钮 enabled 条件、空字符串 trim 逻辑 |
| 2 | 有证据问题返回 grounded | 用高数样例问“通项 an 趋于 0 是否能保证级数收敛？” | 返回 grounded，答案提到必要条件和调和级数反例 | 答案卡片、证据区 | Ask parser、证据定位、question bank 样例文本是否一致 |
| 3 | 部分依据返回 partial | 用物理样例问“线圈方向不垂直时如何计算动生电动势？” | 返回 partial，说明只讲到有效分量 | groundedness 标签、答案文本 | 证据召回、partial 降级逻辑 |
| 4 | 无依据返回 not_found | 用 AI 样例问“Transformer 的自注意力机制怎么计算？” | 返回 not_found，不输出公式或课外长解释 | 答案状态 | 范围判断、fallback 答案模板、无证据处理 |
| 5 | 引用证据可见 | 对 grounded 答案展开证据 | 能看到 quote 和 segment / 来源片段 | Evidence refs 区域 | EvidenceResolver、MaterialEvidenceRef 或 lesson segment mapping |
| 6 | provider 失败 fallback | 临时切到不可用 provider 或断网后提问 | 有本地兜底答案，明确标注 fallback，不假装官方主路径成功 | 答案状态和 provider 标签 | Provider resolver、fallback 标记、错误映射 |
| 7 | local_only 零网络 | 切到 local_only 后提问 | 只走本地兜底，不出现网络调用诊断 | Settings profile + Ask 结果 | provider profile、生效配置、日志标签 |
| 8 | 导出问答记录不泄漏敏感信息 | 完成一次 Ask 后导出学习报告 | 导出只包含问题、答案、证据摘要和状态，不含敏感运行内容 | 导出文件预览 | ContentExporter、ask answer serialization、redaction tests |
| 9 | 不记录敏感运行内容 | 完成 Ask 后查看日志和 debug 面板 | 日志仅有短标签、状态、延迟、错误类型，不含完整模型输入、请求消息、供应商原始返回或内部思考文本 | Settings logs | RedactedLogger、Ask engine logging、debug preview |
| 10 | qwen guard 不变 | 检查当前 BlueLM 文本模型请求守卫测试或代码审计结果 | qwen3.5-plus 仍保留关闭深度思考的 guard | Settings model summary 或测试报告 | Vendor request builder、已有 qwen guard 测试 |
| 11 | RECORD_AUDIO 不存在 | 检查 Manifest 或 AndroidManifestTest | App 没有申请 RECORD_AUDIO | Manifest 检查结果 | `app/src/main/AndroidManifest.xml`、manifest test |
| 12 | not_found 不进入复习任务 | 问一个无依据问题后查看 Review | 不应因为 not_found 生成新的知识点复习任务 | Review 页面 | LearningStore 写入条件、Ask result handling |
| 13 | partial 不伪造证据 | 对 partial 答案查看引用 | 只引用可定位证据，不补不存在 quote | Evidence refs 区域 | EvidenceResolver、partial 降级逻辑 |
| 14 | 切换历史课程后上下文正确 | 在 History 打开另一门课再提问 | 回答只基于当前打开课程 | History + Ask 页面 | selected session/result、History open state |

## 推荐 smoke 顺序

1. 加载高数样例，生成时间线，问 grounded 问题。
2. 加载物理样例，问 partial 问题。
3. 加载 AI 样例，问 not_found 问题。
4. 切换 local_only，重复一个问题，确认本地兜底。
5. 导出报告，检查问答记录只含脱敏学习内容。
6. 回到 Settings，检查日志没有敏感运行内容。
