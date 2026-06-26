# ClassMate

> 把一节课的真实材料，变成能复习、能练、能回看出处、能导出带走的学习资产。

ClassMate 是一个面向学生真实学习场景的**多模态 AI 学习闭环 App（Android）**。它不是又一个聊天壳，也不只是 OCR / 转写 / 刷题工具——我们真正想解决的是"听完课、拍完照、录完音之后呢？"这个问题：那些材料，怎么变成第二天、考试前还能用得上的东西。

## 它是什么，不是什么

- **是**：把课堂文本 / 图片 / 文档 / 转写，整理成知识点、微测、错题、复习计划，而且每一条都能回到它在原始资料里的出处。
- **不是**：一个"问一句答一句"的通用助手。
- **不是**：把网上题库塞给你刷的工具——题目来自**你自己的资料**。

一句话定位：**面向学生真实学习场景的多模态 AI 学习闭环。**

## 核心学习闭环

这是整个 App 的主线，也是我们花了最多力气、并且已经在 vivo 真机上跑通验证的部分：

```text
资料输入 → 证据资产(EvidenceAsset) → 知识点 → 微测 → 错题本 → 复习计划 → 学习诊断 → Study Pack 导出
```

- **资料输入**：粘贴文本、Markdown、图片 / 拍照 OCR、文档、录音 / 转写都能进来。
- **证据资产**：原始内容完整保留、不静默丢字；之后每个知识点、每道题都挂着它的出处。
- **知识点 / 微测 / 错题 / 复习 / 诊断**：从你的资料里"长"出来，而不是从通用题库里拷。
- **Study Pack 导出**：PDF / DOCX / HTML / Markdown / 纯文本，可打印、可归档、可线下复习。

## 当前真实能力状态（重要，而且我们想说实话）

这一节我们想写得好看，但更想写得诚实，所以先把底交清楚：

- **主学习链路已经在 vivo 真机验证过**，是我们的黄金标准。
- **18 项能力都按主链标准做了 L3-readiness 对齐**（入口、证据、路由、失败终态、兜底、测试或抽测清单都对齐了）——但这**不等于这 18 项都已经在真机完整跑通**，有一部分仍然需要 vivo 真机或云真机抽测。
- 路由统一是 **`云端蓝心(BlueLM) → 端侧 3B → 本地基础整理(LOCAL_RULE)`**。
- 云端和端侧都失败、但只要你还有可用输入，App 会**自动进入「本地基础整理」**生成可用的学习结果，而不是甩给你一个空白的安全占位。
- **图片生成、视频生成、同声传译这类实验能力默认关闭**，不影响主链路。

每个能力具体的状态、入口、证据策略、失败终态，都在这里（中文，单一事实来源是代码里的 capability readiness registry，有守卫测试盯着、不让文档和实际跑偏）：

→ [docs/current/official_18_capability_l3_readiness.md](docs/current/official_18_capability_l3_readiness.md)

## 18 项有效能力概览

> 状态都对应代码 registry，尽量不夸大。"主链路"列表示它是不是用户主路径的一部分。

| 能力             | 当前状态         | 在 ClassMate 里干什么                    | 主链路 |
| ---------------- | ---------------- | ---------------------------------------- | ------ |
| 大模型           | 真机已验证       | 课堂分析、知识点、微测、解析、复习、诊断 | 是     |
| 通用 OCR         | 已用于学习闭环   | 图片 / 拍照转文字，存为可编辑证据        | 是     |
| 端侧 3B 大模型   | 待真机抽测       | 弱网 / 云端不可用时的文本 fallback       | 兜底   |
| 长语音转写       | 待真机抽测       | 录音转写为可编辑稿 + AUDIO 证据          | 兜底   |
| 文本翻译         | 本地兜底         | 证据 / 知识点的双语注记                  | 否     |
| 文本向量         | 本地兜底         | 相似知识点、检索增强                     | 否     |
| 文本相似度       | 本地兜底         | Ask / 练习的证据排序                     | 否     |
| 查询改写         | 本地兜底         | Evidence-grounded Ask 检索               | 否     |
| 音频生成         | 本地兜底         | 课程精华 / 朗读脚本（不伪造音频）        | 否     |
| Function calling | 接缝就绪         | 受控内部工具编排                         | 否     |
| 实时短语音识别   | 接缝就绪         | 课堂伴学实时转写（接缝）                 | 否     |
| 长语音听写       | 接缝就绪         | 音频转写候选                             | 否     |
| 方言自由说       | 接缝就绪         | 方言 / 口音转写增强                      | 否     |
| 端侧文本审核     | 接缝就绪         | 导出 / 分享前的安全检查                  | 否     |
| 端侧能力文件     | 配置后可用       | 端侧模型目录诊断                         | 否     |
| 图片生成         | 实验 · 默认关闭 | 学习图解 prompt（不出真图）              | 否     |
| 视频生成         | 实验 · 默认关闭 | 复习短视频 storyboard                    | 否     |
| 同声传译         | 实验 · 默认关闭 | 双语草稿（不伪装实时）                   | 否     |

## 我们觉得做对的几个点

- **证据回溯**：每个知识点、每道题都能点回它在原始资料里的出处，不是凭空生成。
- **个性化微测**：题目从你自己的资料里出，不是网上抄来的通用题。
- **错题 + 复习闭环**：不是"看完总结就完事"，错题会进错题本、关联知识点会进复习队列。
- **Study Pack 导出**：一份能打印、能归档、能线下复习的学习包。
- **三级兜底**：云端 + 端侧 + 本地，弱网或权限异常也不会让学习中断。
- **状态诚实**：实验能力默认关闭，不把"接缝 / 兜底 / 待验证"写成"已跑通"。

## 和其他工具相比，我们的侧重

不想踩竞品，就客观说说差异：

- 比起单纯的转写工具，我们更在意**转写完之后**的学习闭环。
- 比起通用刷题工具，我们更在意**题目来自你自己的资料** + **能回溯证据**。
- 比起通用聊天助手，我们更在意**结构化的学习资产**和**复习闭环**。

## 技术架构

- **Android / Kotlin / Jetpack Compose**，分 `core`（纯 Kotlin、可单测）和 `app` 两层。
- **L3 学习闭环模型** + **EvidenceAsset**：学习产物和它的出处绑在一起。
- **BlueLM cloud provider**：主分析路径（OpenAI 兼容 chat 协议对接 vivo AIGC）。
- **on-device 3B readiness / fallback**：端侧通过反射桥接，缺模型 / 权限时诚实降级、不崩。
- **LocalFallbackProvider**：本地确定性整理，保证最低可用。
- **capability readiness registry**：18 项能力真实状态的单一事实来源 + 守卫测试。
- **Study Pack export** + **localization / i18n**：`ui/i18n` 的 `Strings` 体系，中英可切。

工程上我们比较轴的一点是"别把失败藏起来"——每条路由都得有明确的来源和下一步。

## 稳定性与兜底设计

这块在真机上踩过坑，所以写得比较实：

- 云端 READ 波动：**timeout / retry / 降级重试**（重试会用更短的请求，更容易在超时内读完）。
- 长时间分析：**防息屏（keepScreenOn）** + 已用时 + 思考强度（快速 / 标准 / 深度）+ 慢响应提示 + 可操作出口，**不会无限 loading**。
- 端侧权限缺失：给「去授权 / 重新检测 / 仅用云端 / 本地基础整理 / 手动整理」这几条明确出路。
- **只要还有可用输入，就不会直接落空白安全占位**——先走 `LOCAL_RULE` 本地基础整理。
- 质量门：**CI / current_preflight / cloud_device_precheck** 都得过才算数。

## 更多文档

- 18 项能力 L3-readiness 自检表：[docs/current/official_18_capability_l3_readiness.md](docs/current/official_18_capability_l3_readiness.md)
- 云真机测试计划：[docs/current/cloud_real_device_test_plan_v1.md](docs/current/cloud_real_device_test_plan_v1.md)
- 真机 debug playbook：[docs/current/cloud_device_debug_playbook_v1.md](docs/current/cloud_device_debug_playbook_v1.md)
- 云端 + 端侧协同：[docs/current/cloud_edge_model_collaboration_v1.md](docs/current/cloud_edge_model_collaboration_v1.md)
- Study Pack 导出：[docs/current/learning_study_pack_export_v1.md](docs/current/learning_study_pack_export_v1.md)
- 蓝心云端真机排查：[docs/current/bluelm_cloud_realdevice_troubleshooting.md](docs/current/bluelm_cloud_realdevice_troubleshooting.md)

## 构建与验证

在仓库根目录执行：

```powershell
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
scripts\qa\current_preflight.ps1
scripts\qa\cloud_device_precheck.ps1
```

日常验证不要跑真实 provider 联网 smoke。

## 当前边界

- 不是 18 项能力都已经在真机完整跑通；有一部分仍需 vivo 真机或云真机抽测。
- 实验能力（图片 / 视频 / 同传）默认关闭。
- 全局中英文本地化还在逐页迁移中（已迁移：导航 / 首页 / 导入 / 历史 / 设置 / 微测页 等；其余进行中）。
- 最终视觉打磨和比赛材料由后续专项完成——本仓库先把功能底座和诚实状态做扎实。

## 安全说明

- 不提交任何密钥；`config.local.json`、`app/libs/*`、模型文件、keystore 都不进仓库。
- 日志和导出里不输出 AppKey / Authorization / 用户完整长文。
- 端侧模型文件只做"存在性"检查，不读内容、不进文档 / 日志 / 测试 / 导出。
