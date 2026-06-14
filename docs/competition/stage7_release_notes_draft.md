# Stage 7 Release Notes Draft

## 版本定位

Stage 7 是复赛演示前的功能整合版本，重点是把 ClassMate 从“可分析课堂文本”推进到“可演示的课堂学习闭环产品”。

## 版本亮点

- Official BlueLM/qwen3.5-plus 主路径。
- 多源导入：Markdown、手动 OCR、字幕/转写稿、Live。
- Knowledge Timeline 与 Evidence chain。
- Ask This Lesson 防胡编问答。
- Quiz、Review、需要多练和练习入口。
- Course Library 搜索、筛选和课程详情。
- Export Center 支持多格式报告、保存和分享。
- Stage 7 proof pack 和 QA 脚本。

## 新增能力

- 复赛演示材料包。
- proof pack generator。
- demo folder generator。
- 演示旁白、PPT 大纲、截图清单。
- 真机测试结果模板、风险台账、bug 模板。
- GitHub Issues backlog、labels 和 milestones 建议。

## 修复问题

- 导出文件难以在普通手机上找到的问题已通过 Export Center 方向解决。
- 首页和课程库信息架构更清晰。
- Settings 能力路线图更诚实，区分已支持、实验、待接入和暂缓。

## 已知限制

- 当前 Review/Practice/Adaptive Practice 可能仍在 Stage 7C 半成品阶段，需等生产代码稳定后验证。
- Live ASR 是系统识别实验模式，不等于真实 vivo ASR provider。
- OCR 是手动资料流，不等于真实 vivo OCR provider。
- PDF 中文排版需要真机和打印场景继续确认。
- 云同步、团队协作、声纹身份识别、自研底噪处理暂缓。

## 安全说明

- 不提交本地配置和密钥。
- 不截图密钥输入框。
- 不导出内部模型交互或推理字段。
- 练习搜索只打开外部搜索结果，不抓取平台内容。

## 演示建议

1. 使用标准样例课，避免真实隐私材料。
2. 先跑 BlueLM diagnostic。
3. 录屏前清空或避开 debug 配置输入框。
4. 展示 not_found 问答，证明防胡编边界。
5. 导出报告后检查敏感内容。

## 后续路线

- 完成 Stage 7C Practice 编译与闭环验证。
- 接入真实 vivo ASR/OCR provider。
- 增强多来源证据融合。
- 优化打印级 StudyReport。
- 继续打磨 UI 和 i18n。

