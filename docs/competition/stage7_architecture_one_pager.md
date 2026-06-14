# Stage 7 Architecture One Pager

ClassMate 不是简单套一层模型调用的 App。它把课堂资料、模型分析、本地证据校验、学习状态和导出报告串成一个学习闭环。

## App 层

App 层负责 Android Compose UI、页面导航、导入流程、资料篮、课程库、设置、Live、Ask、Quiz、Review 和 Export Center。它承载用户操作，但不直接保存密钥或内部模型交互。

## Core 层

Core 层负责稳定业务模型：课程分析结果、知识点、证据、微测、复习任务、解析、校验、学习状态和报告模型。核心逻辑尽量保持可单测。

## Provider 层

Provider 层有三类路径：

- Official BlueLM：参赛合规主路径。
- Compatible Demo：展示增强。
- LocalFallback：网络或模型不可用时保底。

Provider 失败不能伪装成功，结果来源要清楚标注。

## MaterialBundle

MaterialBundle 统一承载课堂文本、Markdown、字幕/转写稿、手动 OCR、Live 片段等来源。分析器只处理统一资料，但证据仍保留来源 marker。

## Evidence / Validator

EvidenceResolver 和 Validator 用于限制模型胡编。知识点和题目需要尽量回到本节课资料；证据不可定位时不能伪造成强证据。

## LearningStore

LearningStore 保存复习任务、答题反馈和学习状态。它不保存密钥、本地配置、内部模型输入/消息、供应商原始响应或推理字段。

## Export / StudyReport

Export Center 生成 PDF、HTML、Markdown、TXT、MindMap 和兼容 HTML 报告。StudyReport 包含课程概要、知识点、证据链、微测、复习、需要多练、资料来源和隐私说明。

## ASR Experimental

Live ASR 实验模式依赖设备系统 SpeechRecognizer，不保存原始音频，不后台录音。真实 vivo ASR provider 是后续接入项。

## Practice / Review

Review 负责任务和反馈；Practice 方向负责错题、薄弱点和需要多练闭环。Stage 7C 若仍在开发中，应在演示中标记为待验证。

## 安全边界

- 不提交本地配置和密钥。
- 不展示密钥输入框。
- 不导出内部模型交互或推理字段。
- 不爬取第三方平台内容。
- 不声称未接入的 ASR/OCR provider 已完成。

