# Real Device Fix Matrix - ClassMate 1.14.2

版本：`1.14.2 / 115`
最新提交：`7473fb1 fix(product): repair final real-device import and quiz blockers`

## 五改问题矩阵

| 问题 | 修复状态 | 修复提交/阶段 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- | --- |
| 官方 ASR/TTS 未配置 | 已给系统/手动 fallback；官方网络需配置 | 1.13.7 / 1.13.8 / 1.14.2 | ASR/TTS tests + 真机手册 | 真实 AppKey 网络 |
| 多图片/多文件资料库 | 已支持资料篮、多图 OCR、失败项隔离 | 54d75e4 以后 | OcrImportAssemblerTest + 真机多图 | 官方 OCR 配置 |
| 云端/端侧不可用 | 云端 -> 端侧 -> 本地规则 | 多轮 fallback 修复 | CourseAnalyzerTest / OnDeviceFallbackPolicyTest | 端侧目标机 |
| 知识结构敷衍 | 知识结构大纲与 evidence 绑定 | 1.14.0+ | 真机生成课程检查 | 长材料质量 |
| 学习空间排版 | 主入口收敛，非本轮文档改动 | 已稳定 | UI guard | 多设备屏幕 |
| 反馈闭环 | 反馈进入错题/薄弱/复习 | 1.14.x | LearningEnhancementRoutingTest | 真机交互 |
| 外部搜索 | 浏览器 Intent，不是 API 推荐 | e49f1ca+ | BilibiliSearchTest | 浏览器可用 |
| B 站搜索依赖知识点 | 根据课程/知识点构造搜索 URL | e49f1ca+ | BilibiliSearchTest | 目标设备 Intent |
| 删除学习诊断/建议下一步 | 主叙事收敛，不再作为孤立噪声 | 1.14.0 | UI guard | 现场展示 |
| 删除问这节课 | 不作为主入口；保留证据型 Ask 能力，不主打 | 1.14.0 | 文档和 UI 检查 | 讲解口径 |
| 反馈无作用 | 反馈推动需复核/已复习/薄弱点 | 1.14.x | state tests | 真机复测 |
| 返回键 | 系统返回键接入 | e086d4b | SystemBackNavigationTest | 真机按键 |
| 心流背景音乐 | 已有授权循环背景音 | 后续 UI 修复 | FlowMusicLifecycleTest | 真机音频焦点 |
| 演示数据 | 演示数据明确标识 | 多轮 guard | ProductCopyConsistencyTest | 现场避免误说 |
| 图片题显示图片 | 图片证据可回溯 | 7473fb1 | Evidence tests + 真机图片题 | 真机图片加载 |
| 已复习/薄弱点 | reviewed/weak 状态进入复习 | 1.14.x | Weakness/Review tests | 真机复测 |
| 删除问这节课相关内容 | 主入口删除，叙事聚焦学习闭环 | 1.14.0 | 文档检查 | 讲解口径 |
| 随机小测/做微测入口重复 | 小测入口收敛 | 1.14.0 | UI guard | 真机导航 |
| 系统语言切换不全 | 主学习路径 i18n guard | fd79b90+ | Strings/Localization tests | 真机系统语言 |
| 蓝心调用过久/失败 | 慢响应、重试、fallback | 多轮修复 | current_preflight | 真实网络 |

## 六改问题矩阵

| 问题 | 修复提交 | 当前状态 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- | --- |
| OCR 无效 | `7473fb1` | 失败段可手动输入；OCR 未配置不再卡死 | `OcrFallbackFlowTest` + 图片导入 | 官方 OCR 配置 |
| 系统无法语言识别 | `520a082` + `7473fb1` | 系统 ASR 不可用时提示并保留录音/手动转写 | `SpeechRecognitionDiagnosticsTest` | 目标 ROM 语音服务 |
| 资料篮页面按钮只有 ... | `1.14.x` UI 收敛 | 主按钮完整文案，导入路径可达 | UI guard + 真机目测 | 小屏幕 |
| 没有微测题 | `7473fb1` | 有本地 answerable fallback，不出现空链路 | `PracticeAnswerableGateTest` | 真机生成 |
| 官方 ASR 未配置 | `520a082` + `7473fb1` | 显示配置/fallback 状态，录音和手动转写可继续 | ASR fallback tests | 真实 AppKey |

## 当前推荐复测

1. 冷启动导入图片，故意使用一张模糊图，确认可手动补充。
2. 生成课程后检查知识结构、证据、微测。
3. 做错图片题，进入错题和复习计划，再回看图片证据。
4. 关闭/缺失官方 ASR 配置时录音，确认录音保存和手动转写。
5. 导出普通版和 AI 精修版，检查无密钥、无内部状态、无 raw id。
