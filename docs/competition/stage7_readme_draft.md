# ClassMate README Draft

## 项目名称

ClassMate - AI 课堂学习助手

## 一句话定位

ClassMate 将课堂资料转化为可验证知识点、原文证据链、微测、自适应复习、需要多练和可导出的学习报告。

## 核心功能

- 多源导入：文本、Markdown、手动 OCR 资料、SRT/VTT/TXT 字幕或转写稿、Live 手动课堂。
- 官方 BlueLM 分析：使用 Official BlueLM/qwen3.5-plus 作为参赛合规主路径。
- Knowledge Timeline：生成结构化知识点。
- Evidence chain：每个知识点尽量绑定课堂资料证据。
- Ask This Lesson：基于本节课资料回答，支持 grounded / partial / not_found。
- Quiz：围绕知识点生成微测和解释。
- Review / Practice：基于答题和反馈生成复习与需要多练入口。
- Course Library：按课程聚合历史学习资产。
- Export Center：导出 PDF、HTML、Markdown、TXT、MindMap 和兼容 HTML 报告。

## 技术架构

- App 层：Android Compose UI、导航、导入资料篮、设置、导出和真机演示入口。
- Core 层：课程分析模型、证据、校验、解析、学习状态、报告渲染。
- Provider 层：Official BlueLM、Compatible Demo、LocalFallback。
- MaterialBundle：统一文本、字幕、OCR、Live 等资料来源。
- LearningStore：保存复习任务、答题反馈和学习状态。

## 官方 BlueLM 使用方式

Official BlueLM/qwen3.5-plus 是主路径。真实配置只应通过本机安全配置或 debug 导入进入运行态，不提交到仓库，不写入 README，不出现在日志、导出或截图中。

## 隐私与安全

- 本地配置文件不入仓。
- 日志只保留短状态字段。
- 导出报告不包含密钥、本地配置、内部模型输入/消息、供应商原始响应或推理字段。
- 截图和录屏不得展示密钥输入框或私密材料。
- Compatible Demo 仅作展示增强，Official BlueLM 是参赛主路径。

## 已实现能力

- BlueLM 文本分析。
- Compatible Demo 和 LocalFallback。
- 文本 / Markdown / 字幕转写稿 / 手动 OCR 资料流。
- Timeline、Evidence、Ask、Quiz、Review、Course Library。
- Export Center 和打印级 StudyReport。

## 实验模式能力

- Live ASR 实验模式：依赖设备系统 SpeechRecognizer，不保存原始音频。

## 暂缓能力

- 真实 vivo ASR provider。
- 真实 vivo OCR provider。
- 自动说话人分段。
- 声纹身份识别。
- 自研底噪处理。
- 第三方平台视频爬取。
- 云同步 / 团队协作。

## 本地运行说明

1. 使用 Android Studio 或 Gradle 构建项目。
2. 本地配置文件只放在开发者机器上，不提交到仓库。
3. 如需调试 BlueLM，使用 debug-only 配置导入，导入后不要截图密钥。
4. 运行 secrets scan，确认无本地配置、密钥或构建产物被追踪。

## 测试与 Proof

- 参考 `docs/testing/stage7_full_regression_plan.md`。
- 参考 `docs/competition/stage7_proof_screenshot_list.md`。
- 使用 `scripts/proof/build_stage7_proof_pack.ps1` 生成复赛 proof pack。

## 复赛演示路线

1. Settings 展示 Official BlueLM。
2. Import 展示多源资料。
3. Timeline 展示知识点和证据链。
4. Ask 展示 grounded / partial / not_found。
5. Quiz / Review 展示学习闭环。
6. Export Center 展示报告保存和分享。
7. Roadmap 展示诚实边界和决赛路线。

