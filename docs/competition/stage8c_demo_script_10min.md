# Stage 8C Demo Script (8–10 分钟)

> 适用场景：复赛现场演示或录屏配音。默认只展示脱敏界面；不展示本地配置文件、密钥输入框、完整鉴权头、模型原始请求/响应、模型内部推理内容。

---

## 1. 开场 30 秒：问题与定位

**讲解词：**
大学生课后复习面临三个问题：笔记不完整、不知道重点在哪、不知道是否真学会了。ClassMate 不是又一个录音总结工具——它把课堂资料变成可验证的知识点、证据链、微测、自适应复习、练习和可打印学习报告。我们做的是学习闭环。

**操作步骤：**
- 启动 App，停留在首页 Dashboard。
- 指向今日状态、导入资料入口、最近课程列表。
- 指向底部导航：Timeline / Ask / Review / Practice。

**画面重点：**
- 首页是学习任务入口，不是空白欢迎页。
- 看到"导入课堂资料"主入口和最近课程卡片。

**截图/录屏点：** `stage8c_demo_01_home.png`

**风险兜底话术：** 如果首页加载慢，直接说"首页是学习入口，接下来我们看核心流程"并跳到下一段。

---

## 2. 架构 60 秒：云端蓝心 + 端侧蓝心

**讲解词：**
ClassMate 的智能核心是 BlueLM 双模型架构。云端 BlueLM 是主路径，负责课程分析、高质量问答、微测生成和报告生成。端侧 BlueLM 3B 是本地智能兜底——云端不可用时，端侧在设备本地完成课程分析和问答，数据不离开手机。两种模型都失败时走安全占位，诚实告知用户，不编造假结果。

**操作步骤：**
- 进入 Settings → 模型配置。
- 展示 Official BlueLM 云端配置（掩码显示密钥）。
- 展示端侧 BlueLM 3B 诊断状态：模型路径 OK、init success、generate success。
- 展示三种 provider path 标签：CLOUD_BLUELM / 端侧蓝心 / SAFETY_PLACEHOLDER。

**画面重点：**
- Official BlueLM 标注为"主路径"。
- 端侧 BlueLM 标注为"本地智能兜底"。
- 密钥掩码显示。

**截图/录屏点：** `stage8c_demo_02_provider_config.png` / `stage8c_demo_03_ondevice_diag.png`

**风险兜底话术：** 如果 Settings 诊断状态不是全绿，诚实说"当前诊断显示 X 状态，后续我们会展示历史成功记录"。

---

## 3. 真机 Proof 60 秒

**讲解词：**
端侧不是 mock。我们在 vivo X300 Pro（OriginOS 6 / Android 16）上完成了真机验证。请看：模型目录 `/sdcard/1225` 授权成功；文本 init 和 generate 成功；多模态 callVit 返回 code 0，generate 成功。SDK 二进制 AAR 通过反射桥接调用，不入 Git 仓库。可以通过 javap 验证 AAR 类签名。

**操作步骤：**
- 展示 Settings 诊断页：模型路径 OK、文本 init/generate success。
- 展示多模态诊断：callVit code 0、generate success。
- 切换到文件管理器展示 `/sdcard/1225` 目录存在。
- 终端展示 `git check-ignore app/libs/llm-sdk-release.aar` 返回路径（说明 AAR gitignored）。

**画面重点：**
- Settings 诊断页四个绿色状态。
- `/sdcard/1225` 目录可见。
- gitignore 规则生效。

**截图/录屏点：** `stage8c_demo_04_device_proof.png`

**风险兜底话术：** 如果现场诊断某项不是绿色，说"这个状态受当前设备环境影响，我们有历史验证记录可以展示"。

---

## 4. 文本输入 90 秒：Timeline → Evidence

**讲解词：**
现在演示最核心的流程。我们用一段大学物理"电磁感应"课堂文本做演示。导入后，云端 BlueLM 进行课程分析，生成 Knowledge Timeline——不是一段不可追溯的总结，而是结构化的知识点列表。每个知识点都有标题、重要性、难度和原文证据。点开证据，可以看到知识点的原文出处和来源标记。

**操作步骤：**
- 进入 Import Hub → 粘贴文本（使用 test_inputs 中的物理电磁感应文本）。
- 点击"开始分析"。
- 展示分析进度（provider: CLOUD_BLUELM → 处理中）。
- 分析完成，进入 Timeline。
- 展示知识点列表：法拉第定律、楞次定律、感应电动势等。
- 点开一个知识点，展示 Evidence quote 和 source marker。
- 滚动展示 3–4 个知识点及其证据。

**画面重点：**
- 知识点不是一句话总结，而是结构化卡片（标题 + 重要性 + 难度）。
- Evidence 可追溯到原文。
- provider path 标签 CLOUD_BLUELM。

**截图/录屏点：** `stage8c_demo_05_text_import.png` / `stage8c_demo_06_timeline.png` / `stage8c_demo_07_evidence.png`

**风险兜底话术：** 如果云端分析超时，说"网络条件有限，我们改为展示端侧分析结果"并跳转到 fallback 演示（第 8 段）。

---

## 5. 图片/拍照 90 秒：多模态草稿 → 用户确认 → 课程分析

**讲解词：**
课堂不止有文本。学生对板书或课件拍照后，端侧多模态模型理解图片内容，生成可编辑的文字草稿。注意：草稿不会自动进入知识库。用户在草稿编辑页确认和修改内容后，点击确认才进入课程分析。如果取消，草稿不会落库。

**操作步骤：**
- 进入 Import Hub → 拍照（用空白纸或准备的测试图片）。
- 展示拍摄界面，拍照。
- 展示"正在生成草稿"——端侧 VIT 编码 + 推理。
- 进入草稿编辑页：展示 AI 生成的文字描述。
- 编辑草稿中某一行（例如补充一个公式名称）。
- 点击"确认并分析"。
- 展示分析结果：进入 Timeline。
- 可选：再拍一张照，在草稿编辑页点击"取消"，展示 History 中无新增记录。

**画面重点：**
- 草稿编辑页的"可编辑"功能（输入框可修改）。
- "确认并分析"按钮和"取消"按钮并存。
- 确认后的 Timeline 结果。

**截图/录屏点：** `stage8c_demo_08_camera.png` / `stage8c_demo_09_draft_edit.png` / `stage8c_demo_10_draft_confirm.png`

**风险兜底话术：** 如果多模态 callVit 返回非 0，说"当前设备多模态诊断状态受限，但我们有历史成功记录"并展示历史截图。

---

## 6. Ask / Quiz / Review / Practice 90 秒

**讲解词：**
知识时间线不是终点。基于这节课的知识点，学生可以：Ask — 在课堂资料范围内提问，答案必须 grounded 有依据；Quiz — 自动生成的微测，检测掌握程度；Review — 自适应复习，错题进入"需要多练"；Practice — 系统生成练习搜索词，引导去外部平台针对性练习。

**操作步骤：**
- 从 Timeline 进入 Ask，输入"法拉第定律的数学表达式是什么？"。
- 展示 Ask 结果：答案 + Evidence 引用 + grounded 标签 + provider 来源。
- 进入 Quiz，展示 2–3 道选择题。
- 故意选错一题，展示正确答案和解释。
- 进入 Review，展示"需要多练"列表中有刚才答错的知识点。
- 进入 Practice，展示搜索词卡片（如"法拉第定律 习题 大学物理"）。

**画面重点：**
- Ask 的 grounded 标签和证据引用。
- Quiz 错题的解释。
- Review 的"需要多练"和答错关联。
- Practice 搜索词卡片（不是内嵌网页）。

**截图/录屏点：** `stage8c_demo_11_ask.png` / `stage8c_demo_12_quiz.png` / `stage8c_demo_13_review.png` / `stage8c_demo_14_practice.png`

**风险兜底话术：** 如果 Quiz 生成慢，说"微测生成需要分析完整课程，我们先看 Review 和 Practice"并调整顺序。

---

## 7. Export 60 秒：可打印学习报告

**讲解词：**
ClassMate 的学习成果可以导出。Export Center 支持 PDF、HTML、Markdown 格式。报告包含知识点摘要、证据引用、微测结果和复习建议。导出前有安全 redaction 检查，确保不含敏感内容。排版适合直接打印成纸质资料。

**操作步骤：**
- 进入 Export Center。
- 选择当前课程。
- 选择 PDF 格式。
- 展示 PDF 预览（目录、知识点、证据、微测结果）。
- 展示 Markdown 格式作为备选。
- 说明"可分享"或"可保存到文件"。

**画面重点：**
- PDF 结构完整：目录 → 知识点 → 证据 → 微测 → 复习建议。
- 导出不含原始模型输出、密钥、用户路径。

**截图/录屏点：** `stage8c_demo_15_export.png`

**风险兜底话术：** 如果 PDF 渲染慢，先展示 Markdown 预览，说"PDF 和 Markdown 内容一致，格式不同"。

---

## 8. 弱网/失败 Fallback 60 秒：端侧蓝心 / 安全占位

**讲解词：**
弱网或断网场景怎么办？我们打开飞行模式演示。导入新课程文本，发起分析。云端不可用 → 自动切换端侧 BlueLM 3B。端侧分析完成，Timeline 正常展示，来源标签标记为端侧蓝心。如果端侧也失败——假设我们移除模型目录——系统走安全占位，明确告知用户"当前无法完成智能分析"，不生成假结果。

**操作步骤：**
- 开启飞行模式。
- 导入一段新测试文本。
- 发起分析 → 展示 "CLOUD_BLUELM 不可用，尝试端侧 BlueLM 3B"。
- 端侧分析成功 → Timeline 展示 + 端侧蓝心标签。
- 可选：展示 validators 对端侧结果同样生效（如需展示拦截，讲逻辑即可）。
- 关闭飞行模式，可选展示安全占位 UI（如果设备上可模拟端侧失败）。

**画面重点：**
- provider 切换提示。
- 端侧蓝心来源标签。
- 安全占位 UI（如有条件展示）。

**截图/录屏点：** `stage8c_demo_16_fallback_flow.png`

**风险兜底话术：** 如果端侧在当前设备不可用，说"端侧依赖具体设备的模型预置状态。我们准备了端侧成功的历史录屏可供查看"并展示录屏。

---

## 9. vivo 特色与后续系统级嵌入 60 秒

**讲解词：**
ClassMate 深度利用 vivo 生态。当前已接入：vivo BlueLM 云端 API、BlueLM 端侧 SDK（3B 参数、多模态 VIT）、系统 SpeechRecognizer 用于 Live ASR 实验。决赛规划：接入 vivo ASR/OCR/TTS 官方 SDK、接入 vivo CmsLocalFrame 端侧文本审核、接入 vivo 负一屏学习卡片、蓝心小V 语音问答入口、通知栏复习提醒、桌面课程快捷方式。目标是让学习入口融入 vivo 系统体验。

**操作步骤：**
- 展示 Settings 中 vivo 相关配置项。
- 展示 roadmap 文档或架构图中的 vivo 接入点。
- 口述决赛规划（不要做成已完成的 UI）。

**画面重点：**
- 当前已完成的 vivo 接入点用实线/绿色标注。
- 决赛规划的接入点用虚线/灰色标注。

**截图/录屏点：** `stage8c_demo_17_vivo_ecosystem.png`

**风险兜底话术：** 如果被问具体时间线，说"这些在决赛阶段逐步推进，当前已在架构层预留接口"。

---

## 10. 总结 30 秒

**讲解词：**
ClassMate 的差异化：① 学习闭环 — 不是单次 AI 总结，是输入→理解→自测→巩固→拓展→产出；② 端侧真机 proof — BlueLM 3B 在 vivo X300 Pro 上跑通，不是 mock；③ 安全诚实 — 证据可追溯、validators 防胡编、失败走安全占位不造假；④ vivo 深度 — 云端 BlueLM + 端侧 BlueLM + 系统级嵌入路线。谢谢评委。

**操作步骤：**
- 回到首页，展示完整学习循环的概念图或导航结构。
- 微笑，等待提问。

**画面重点：**
- 四大差异点可视化呈现。

**截图/录屏点：** `stage8c_demo_18_summary.png`

**风险兜底话术：** 此处不需要兜底——如果前面有跳过的段，用总结补齐关键信息即可。
