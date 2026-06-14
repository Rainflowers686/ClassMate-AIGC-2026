# Codex Stage 4 低风险任务提示词

你现在在 Windows 本地项目：

`D:\Edge Download\AIGC\ClassMate`

目标：配合 Stage 4 ASR/OCR/PPT/板书/资料融合开发，做低风险、可并行的文档、测试资产、样例、检查清单和 fake-provider 测试准备。除非用户明确要求，本轮不要改生产代码。

## 硬约束

- 不读取本地真实配置内容，只能检查是否存在。
- 不写入真实 AppID / AppKEY / API key。
- 不记录认证头、完整课堂正文、完整模型输入、供应商原始返回、内部思考文本。
- 不改 BlueLM / Compatible / LocalFallback / ProviderResolver。
- 不削弱 EvidenceValidator / ResultValidator / EvidenceResolver。
- 不做第三方平台视频爬取方案。
- 不声称 ASR/OCR/视频解析已经完成。
- 不 commit、不 push、不打 tag。

## 适合 Codex 做的任务

### 1. 术语表数据扩充

- 扩充高等数学、大学物理、离散数学、C++、马原、AI 入门术语。
- 每个术语包含 aliases、subject、definition、examples、priority、source。
- 不调用模型做课程分类。
- 输出到 docs 或测试 fixtures，等待生产代码接入。

### 2. Sample lesson 扩充

- 增加可直接复制进 App 的课堂文本。
- 每段 1200-1800 中文字。
- 每段至少覆盖 6-8 个知识点和 5-8 道微测方向。
- 保证文本不含个人隐私、真实密钥或敏感字段。

### 3. Docs 更新

- 同步 README / ROADMAP / competition docs。
- 明确当前已实现能力、已留接口能力、未完成能力。
- 不夸大 ASR/OCR/端侧模型状态。

### 4. GitHub Issue 整理

- 把 Stage 4 能力拆成 P0/P1/P2/P3 issue 草案。
- 每个 issue 包含 acceptance criteria、tests、dependencies、risk。
- ASR/OCR 优先官方 vivo 能力，但不写真实 endpoint 或 key。

### 5. Fake Provider Tests 设计

- 设计 fake ASR provider 测试：手动片段、暂停继续、句段时间、speaker label。
- 设计 fake OCR provider 测试：PPT 图片文本、板书文本、证据来源。
- 设计 LessonMaterialBundle fusion 测试：多来源排序、去空、证据 ref 保留。
- 仅在用户允许改测试代码时再实现。

### 6. Export 文案检查

- 检查导出报告是否只写学习结果和证据摘要。
- 确认导出不含密钥、完整模型输入、供应商原始返回、内部思考文本。
- 补充多模态证据来源说明。

### 7. Safety Checklist

- 检查 Git 不追踪本地配置、keystore、apk/aab、build 输出。
- 检查 docs 不含真实密钥。
- 检查截图/录屏指南提醒不要拍到 debug import 明文。

## 建议执行顺序

1. 先做 docs 与 sample lessons；
2. 再整理 issue 草案；
3. 再补测试计划；
4. 最后跑轻量检查：`git diff --check`、`git status --short`、本地 secrets scan。

## 完成报告格式

- 新增/修改文件列表；
- 每类文件摘要；
- 是否触碰 app/src 或 core/src；
- 是否读取真实本地配置；
- 是否写入真实密钥；
- 轻量检查结果；
- 是否建议 commit；
- 建议 commit message。
