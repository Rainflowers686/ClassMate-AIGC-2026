# Document Index - ClassMate 1.14.9

版本：`1.14.9 / versionCode 122`

本轮新增当前入口：

| 文件 | 类型 | 状态 |
| --- | --- | --- |
| `docs/current/CHANGELOG_1_14_9.md` | 当前版本变更 | 新增 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_9.md` | 真机回归修复矩阵 | 新增 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_9.md` | 真机复测手册 | 新增 |

1.14.9 之后，练习完成不再自动退出；课程详情、知识点时间线和复习路径的微测入口统一到 Practice 主流程；复习计划、课程总结、相关知识点和微测题只使用 accepted subject knowledge points + evidence binding。BlueLM 底层对齐 qwen3.5-plus 官方接口，快速/均衡/专业分别映射为 low/medium/high + enable_thinking false/false/true；普通用户仍只看到“蓝心 / 蓝心大模型”，不展示底层模型名或 reasoning 内容。

版本：`1.14.8 / versionCode 121`

本轮新增当前入口：
| 文件 | 类型 | 状态 |
| --- | --- | --- |
| `docs/current/CHANGELOG_1_14_8.md` | 当前版本变更 | 新增 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_8.md` | 真机 BlueLM 主链路修复矩阵 | 新增 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_8.md` | 真机 BlueLM 主链路复测手册 | 新增 |

1.14.8 之后，BlueLM 是课程分析、课程总结/相关知识、微测生成、反馈重写/替换题、弱点变式和 AI 精修导出的优先 provider。dry-run 只用于诊断，不能作为主流程开关；正式请求失败后才进入端侧或本地 fallback。官方 ASR 主路线继续为官方实时 ASR、官方长语音转写、手动转写，系统 SpeechRecognizer 仅为用户主动选择的设备 fallback。

版本：`1.14.7 / versionCode 120`

本轮新增当前入口：

| 文件 | 类型 | 状态 |
| --- | --- | --- |
| `docs/current/CHANGELOG_1_14_7.md` | 当前版本变更 | 新增 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_7.md` | 真机官方服务诊断修复矩阵 | 新增 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_7.md` | 真机官方服务 dry-run 手册 | 新增 |
| `scripts/qa/provider_live_smoke.ps1` | 本地官方服务 smoke 脚本 | 新增 |

1.14.7 之后，BlueLM、官方 ASR、官方 TTS/OCR 的配置读取、readiness、dry-run、错误分类和脚本验证以上述文件以及 `OFFICIAL_INTERFACE_REFERENCE_INDEX.md`、`CLAUDE_OFFICIAL_INTERFACE_LOOKUP_GUIDE.md` 为准。缺配置输出 SKIP，不等于功能失败；本地 fallback 不等于官方成功。

版本：`1.14.6 / versionCode 119`

本轮新增当前入口：

| 文件 | 类型 | 状态 |
| --- | --- | --- |
| `docs/current/CHANGELOG_1_14_6.md` | 当前版本变更 | 新增 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_6.md` | 真机问题修复矩阵 | 新增 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_6.md` | 真机复测手册 | 新增 |

1.14.6 之后，官方 ASR 主路线、系统 ASR 可选 fallback、蓝心配置诊断、录音后官方转写和微测相关性 gate 以上述三个文件为准。

版本：`1.14.5 / versionCode 118`

本轮新增当前入口：

| 文件 | 类型 | 状态 |
| --- | --- | --- |
| `docs/current/CHANGELOG_1_14_5.md` | 当前版本变更 | 新增 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_5.md` | 真机问题修复矩阵 | 新增 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_5.md` | 真机复测手册 | 新增 |

1.14.5 之后，OCR/文本学科知识点过滤、微测相关性、复习计划知识点口径、课程相关知识课内检索、ASR 无服务 fallback 和新装默认外观状态以上述三个文件为准。

版本：`1.14.4 / versionCode 117`

本轮新增当前入口：

| 文件 | 类型 | 状态 |
| --- | --- | --- |
| `docs/current/CHANGELOG_1_14_4.md` | 当前版本变更 | 新增 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_4.md` | 真机问题修复矩阵 | 新增 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_4.md` | 真机复测手册 | 新增 |

1.14.4 之后，ASR 无系统服务、蓝心失败、微测相关性、反馈即时优化、复习知识点摘要等当前状态以以上三个文件为准。

版本：`1.14.3 / versionCode 116`
本轮候选改动：录音/ASR readiness、反馈即时优化、复习知识摘要、题目详解增强。

## 文档盘点表

| 文件路径 | 类型 | 当前是否过时 | 处理方式 |
| --- | --- | --- | --- |
| `README.md` | 根入口 | 是 | 已重写为 1.14.2 总入口 |
| `CHANGELOG.md` | 根变更日志 | 缺失 | 已新建 |
| `ARCHITECTURE.md` | 根架构入口 | 是 | 更新为指向 1.14.2 架构事实 |
| `ROADMAP.md` | 根路线图 | 是 | 更新为候选版剩余验证路线 |
| `SECURITY.md` | 根安全说明 | 部分过时 | 更新为当前安全边界 |
| `docs/INDEX.md` | docs 总入口 | 是 | 更新为当前 docs/current 入口 |
| `docs/current/README.md` | 当前文档入口 | 缺失 | 已新建 |
| `docs/current/FINAL_STATUS_1_14_2.md` | 当前状态 | 缺失 | 已新建 |
| `docs/current/FINAL_STATUS_1_14_3.md` | 当前状态 | 缺失 | 已新建，覆盖 1.14.3 新增修复 |
| `docs/current/CHANGELOG_1_14_2.md` | 当前版本变更 | 缺失 | 已新建 |
| `docs/current/CHANGELOG_1_14_3.md` | 当前版本变更 | 缺失 | 已新建 |
| `docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md` | 官方能力 | 缺失 | 已新建 |
| `docs/current/official_capability_evidence_matrix_v2.md` | 官方能力旧入口 | 部分过时 | 更新为 1.14.2 映射 |
| `docs/current/official_tool_matrix.md` | 官方工具旧入口 | 部分过时 | 更新为 1.14.2 映射 |
| `docs/current/official_tool_productization_matrix.md` | 官方产品化旧入口 | 部分过时 | 更新为 1.14.2 映射 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_2.md` | 真机修复 | 缺失 | 已新建 |
| `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_3.md` | 真机修复 | 缺失 | 已新建，补充 ASR 设置入口和反馈即时优化 |
| `docs/current/REAL_DEVICE_REGRESSION_CHECKLIST.md` | 真机回归 | 缺失 | 已新建 |
| `docs/current/realdevice_regression_checklist_v1.md` | 旧真机清单 | 部分过时 | 保留并加新入口说明 |
| `docs/current/ARCHITECTURE_1_14_2.md` | 架构 | 缺失 | 已新建 |
| `docs/current/LEARNING_LOOP_ARCHITECTURE.md` | 学习闭环架构 | 缺失 | 已新建 |
| `docs/current/FALLBACK_ARCHITECTURE.md` | fallback 架构 | 缺失 | 已新建 |
| `docs/current/EXPORT_AND_POLISHED_STUDY_PACK.md` | 导出 | 缺失 | 已新建 |
| `docs/current/AI_STUDY_PACK_PRODUCT_SPEC.md` | AI 精修规格 | 缺失 | 已新建 |
| `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_2.md` | 真机手册 | 缺失 | 已新建 |
| `docs/current/FINAL_SMOKE_TEST_CHECKLIST.md` | 最终 smoke | 缺失 | 已新建 |
| `docs/current/DEMO_SCRIPT_1_14_2.md` | 演示脚本 | 缺失 | 已新建 |
| `docs/current/DEFENSE_NARRATIVE.md` | 答辩叙事 | 缺失 | 已新建 |
| `docs/current/SUBMISSION_CHECKLIST.md` | 提交清单 | 缺失 | 已新建 |
| `docs/current/DEVELOPER_SETUP_1_14_2.md` | 开发者设置 | 缺失 | 已新建 |
| `docs/current/BUILD_AND_RELEASE.md` | 构建发布 | 缺失 | 已新建 |
| `docs/current/QA_VALIDATION_COMMANDS.md` | QA 命令 | 缺失 | 已新建 |
| `docs/current/PRIVACY_SECURITY_AND_SECRETS.md` | 安全密钥 | 缺失 | 已新建 |
| `docs/current/SAFE_EXPORT_POLICY.md` | 安全导出 | 缺失 | 已新建 |
| `docs/current/asr_long_productization_report.md` | ASR 历史报告 | 部分过时 | 保留历史，新增 1.14.2 映射 |
| `docs/current/tts_translation_function_calling_report.md` | TTS/Translation 历史报告 | 部分过时 | 保留历史，新增 1.14.2 映射 |
| `docs/current/project_current_status_v1_8.md`、`project_current_status_v1_9.md` | 旧状态 | 是 | 加归档说明，当前状态见 FINAL_STATUS |
| `docs/current/*v1_1*` 至 `*v1_7*` | 阶段报告 | 是 | 加归档说明，不改历史事实 |
| `docs/competition/*`、`docs/testing/*stage*` | 历史比赛/测试材料 | 多数过时 | 保留历史；当前演示/测试见 docs/current |
| `docs/archive/*`（如后续存在） | 历史归档 | 否 | 不改历史事实，仅在当前索引提示 |

## 当前推荐阅读顺序

1. `README.md`
2. `docs/current/FINAL_STATUS_1_14_3.md`
3. `docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md`
4. `docs/current/REAL_DEVICE_FIX_MATRIX_1_14_3.md`
5. `docs/current/DEMO_SCRIPT_1_14_2.md`
6. `docs/current/REAL_DEVICE_TEST_MANUAL_1_14_2.md`
7. `docs/current/BUILD_AND_RELEASE.md`

## Official Interface Knowledge Base

These documents are the current lookup layer for vivo / BlueLM / official capability implementation work. They do not replace historical reports; they index them and state the current ClassMate 1.14.2 implementation boundary.

| File | Purpose |
| --- | --- |
| `docs/current/OFFICIAL_INTERFACE_REFERENCE_INDEX.md` | Main index for local official interface materials, code mappings, validation status, and overclaim boundaries. |
| `docs/current/CLAUDE_OFFICIAL_INTERFACE_LOOKUP_GUIDE.md` | Fast lookup guide for Claude before changing TTS, ASR, OCR, BlueLM, on-device, retrieval, or export behavior. |
| `docs/current/OFFICIAL_PROTOCOL_QUICK_REFERENCE.md` | Short protocol reference for TTS WebSocket, realtime ASR WebSocket, long ASR HTTP, BlueLM, OCR, retrieval, and system fallbacks. |
| `docs/current/OFFICIAL_SOURCE_FILE_MAP.md` | Source-file and test map for official capability development. |
| `docs/current/OFFICIAL_INTERFACE_GAPS_AND_BOUNDARIES.md` | Claim boundary and remaining validation gaps for official capabilities. |

## 旧文档处理原则

历史文档可以出现旧版本、旧问题和旧风险，但必须理解为当时记录。凡是涉及当前状态、答辩、发布、真机复测、官方能力叙事，以本索引列出的 1.14.2 文档为准。
