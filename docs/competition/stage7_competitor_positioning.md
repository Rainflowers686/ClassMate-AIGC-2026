# Stage 7 Competitor Positioning

## 听脑类产品强项

这里把竞品抽象为“录音转写 / AI 总结类产品”，不针对具体品牌做贬低。

- 实时录音和转写体验成熟。
- 音视频上传入口完整。
- AI 摘要和会议纪要生成快。
- 分享导出路径清晰。
- 多场景模板适配强。
- 部分产品有词库、热词和术语优化。

## ClassMate 当前已追平 / 部分追平

- 文本、Markdown 导入：已实现。
- SRT/VTT/TXT 字幕或转写稿：已实现，可进入资料流。
- OCR/PPT/板书：手动资料流已实现，真实 provider 待接入。
- Live：手动课堂和系统 ASR 实验模式已具备演示入口。
- AI 问答：Ask This Lesson 已围绕本节课证据设计。
- 导出分享：Export Center 已支持多格式、保存和系统分享。

## ClassMate 不追的能力和原因

- 第三方平台视频爬取：版权和平台规则风险高，不作为产品方向。
- 自研声纹身份识别：隐私敏感且不是复赛核心学习闭环。
- 自研底噪处理：优先依赖系统或 provider，避免投入到底层音频算法。
- 云同步 / 团队协作：需要服务器、账号和合规设计，复赛暂缓。

## ClassMate 核心差异化

ClassMate 的核心不是录音本身，而是学习闭环：

- 课堂资料结构化为知识点。
- 原文证据链可追溯。
- 微测和解释基于本节课资料。
- ReviewTask 和 LearningStore 形成复习闭环。
- 需要多练能连接练习搜索和错题反馈。
- Course Library 按课程聚合学习资产。
- Export Center 输出可打印学习报告。
- Official BlueLM 是合规主路径，LocalFallback 保证演示韧性。

## 复赛讲法

复赛重点说：

> 听脑类产品把“录音变成总结”做得很好；ClassMate 在补齐输入能力的同时，把重点放在“总结之后如何学习”。我们用官方 BlueLM 主路径，把课堂资料转成证据化知识点、微测、复习和练习闭环。

避免说：

- 我们已经全面超过所有录音转写产品。
- 我们已经完成真实 vivo ASR/OCR provider。
- 我们能抓取第三方平台视频内容。

## 决赛讲法

决赛重点说：

> 决赛版会把真实 vivo ASR/OCR provider 接到现有 MaterialBundle 架构里，让录音、字幕、课件、板书、手动笔记都成为可追溯证据来源，再继续增强学习画像和复习策略。

决赛优先级：

1. 真实 ASR/OCR provider。
2. 多来源证据融合。
3. 更强的 Ask This Lesson grounding。
4. 更完整的 Adaptive Practice。
5. 打印级报告和演示视频。

