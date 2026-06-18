# ClassMate 文档导航

这份索引用于快速找到 ClassMate 复赛材料、架构方案、测试素材、后续任务包和设计参考。这里只列文件名和用途，不复述长文档内容。

## Current Status First

最新当前态以 `docs/current/` 为准：

- `current/official_provider_network_smoke_run.md`：官方 provider smoke 当前矩阵，OCR / QUERY_REWRITE / TEXT_SIMILARITY / EMBEDDING 已真实 network `PASS`。
- `current/official_provider_smoke_setup.md`：provider smoke 安全运行方式、配置解释、timeout 与 Query Rewrite schema 修复记录。
- `current/stage10_baseline.md`：当前产品基线和 L3 readiness 说明。
- `current/full_e2e_device_acceptance_run.md`：历史端到端设备验收记录；下一主线是 App-level L3 云真机/真机闭环验证。

旧 Stage / competition / testing 文档保留历史证据链，不一定代表最新状态。Query Rewrite live-smoke blocked 已由 Claude 专项定位为请求体 schema mismatch 并修复；最新状态以四项 provider `PASS` 矩阵为准。

## 1. 复赛 / 答辩材料

### 演示脚本

- `competition/demo-script.md`：通用演示脚本。
- `competition/rematch-demo-script.md`：复赛现场讲解脚本。
- `competition/ask_lesson_demo_script.md`：Ask This Lesson 差异化演示。
- `competition/ocr_ppt_demo_script.md`：OCR / PPT / 板书资料导入演示。

### 真机 smoke

- `competition/stage3-device-smoke-checklist.md`：Stage 3 真机逐项检查。
- `competition/stage3_post_push_smoke.md`：Stage 3 push 后 smoke 指南。
- `competition/stage5_smoke_checklist.md`：Stage 5 最短验收清单。

### 评委问答

- `competition/judge-qna.md`：评委可能问题与回答。
- `competition/review-readiness.md`：答辩准备与项目定位说明。
- `competition/competitive-reference.md`：录音转写 / AI 总结类产品参照。
- `competition/feature_gap_vs_tingnao_full.md`：与同类产品的能力差距和超越路线。
- `competition/classmate_vs_recording_ai_strategy.md`：追平基础输入能力、强化学习闭环的策略。

### 功能矩阵

- `competition/capability-matrix.md`：能力状态矩阵。
- `competition/feature-acceptance-matrix.md`：功能验收矩阵。
- `competition/ask_lesson_acceptance_checklist.md`：Ask This Lesson 验收清单。
- `competition/ocr_ppt_acceptance_checklist.md`：OCR / PPT / 板书验收清单。

### 安全 proof

- `competition/security-proof-checklist.md`：安全与脱敏检查清单。
- `competition/device-recording-shot-list.md`：真机录屏分镜和不要拍到的内容。

### Stage 5 材料

- `competition/stage5_smoke_checklist.md`：Export Center、本地音视频 / 字幕、OCR、Ask、安全边界的 5 分钟验收。
- `competition/roadmap_next_3_sprints.md`：后续 3 个 sprint 路线图。
- `competition/stage3_release_handoff.md`：Stage 3 交接材料，可作为后续交接格式参考。
- `competition/stage3_pr_description.md`：PR 描述模板。
- `competition/stage3_commit_commands.md`：提交命令建议。
- `competition/stage3_issue_backlog.md`：后续 issue 草案索引。

## 2. 架构设计

- `architecture/overview.md`：项目架构总览。
- `architecture/asr_ocr_lesson_fusion_plan.md`：ASR / OCR / LessonMaterialBundle 资料融合方案。
- `architecture/course_term_glossary_plan.md`：课程术语表与热词方案。
- `competition/roadmap_next_3_sprints.md`：后续能力路线和 sprint 切分。
- `decisions/0001-bluelm-first.md`：BlueLM-first 路线决策。
- `decisions/0002-core-kotlin-jvm-module.md`：core JVM 模块决策。
- `decisions/0003-focus-default-theme.md`：Focus 默认主题决策。
- `product/vision.md`：产品愿景。

## 3. 测试素材

- `testing/classmate_sample_lessons.md`：标准课堂文本库总文档。
- `testing/sample_lessons/README.md`：独立样例课堂文件说明。
- `testing/sample_lessons/gaoshu_series_convergence_long.txt`：高数数项级数长文本。
- `testing/sample_lessons/physics_electromagnetic_induction_long.txt`：大学物理电磁感应长文本。
- `testing/sample_lessons/discrete_relation_poset_long.txt`：离散数学关系与偏序长文本。
- `testing/sample_lessons/cpp_pointer_reference_long.txt`：C++ 指针与引用长文本。
- `testing/sample_lessons/marxism_materialism_practice_long.txt`：马原实践与认识长文本。
- `testing/sample_lessons/mixed_ai_algorithm_intro_long.txt`：AI / 机器学习流程长文本。
- `testing/ask_lesson_question_bank.md`：Ask This Lesson 问题库。
- `testing/ocr_slide_blackboard_samples.md`：OCR / PPT / 板书测试素材。
- `testing/stage5_quick_inputs.md`：Stage 5 快速输入素材。

## 4. GitHub Issues 草案

`issues/` 下是后续可复制到 GitHub Issues 的任务包，覆盖 ASR、OCR、资料融合、术语表、Ask This Lesson、本地音视频和导出增强。

- `issues/01_asr_provider_abstraction.md`
- `issues/02_vivo_asr_integration.md`
- `issues/03_transcript_session_and_segments.md`
- `issues/04_sentence_level_audio_text_sync.md`
- `issues/05_speaker_label_basic.md`
- `issues/06_ocr_provider_abstraction.md`
- `issues/07_vivo_ocr_integration.md`
- `issues/08_ppt_image_import_and_ocr.md`
- `issues/09_blackboard_photo_ocr.md`
- `issues/10_lesson_material_bundle_fusion.md`
- `issues/11_course_term_glossary.md`
- `issues/12_ask_lesson_provider_grounded_qa.md`
- `issues/13_local_audio_upload_transcription.md`
- `issues/14_local_video_upload_safe_pipeline.md`
- `issues/15_export_multimodal_evidence_report.md`

## 5. 提示词包

- `claude_stage4_asr_ocr_foundation.md`：位于提示词目录，交给 Claude 的 Stage 4 ASR / OCR 地基开发材料。
- `codex_stage4_low_risk_tasks.md`：位于提示词目录，交给 Codex 的低风险文档、样例、任务整理材料。

## 6. 设计参考

- `design_refs/classmate_design_system.html`：ClassMate 设计系统参考。
- `design_refs/classmate_focus.html`：Focus 默认主题参考。
- `design_refs/classmate_flow.html`：Flow / Live 沉浸体验参考。
- `design_refs/classmate_liquid_glass.html`：活力视觉方向参考。
- `design_refs/design-handoff-summary.md`：设计交接摘要。

## 7. 当前推荐阅读顺序

### 复赛演示前 10 分钟速读

1. `competition/rematch-demo-script.md`
2. `competition/stage5_smoke_checklist.md`
3. `competition/judge-qna.md`
4. `competition/security-proof-checklist.md`

### Claude 接手开发前阅读

1. `competition/roadmap_next_3_sprints.md`
2. `architecture/asr_ocr_lesson_fusion_plan.md`
3. `architecture/course_term_glossary_plan.md`
4. `issues/10_lesson_material_bundle_fusion.md`
5. `claude_stage4_asr_ocr_foundation.md`

### Codex 接手低风险任务前阅读

1. `docs/INDEX.md`
2. `testing/stage5_quick_inputs.md`
3. `testing/ask_lesson_question_bank.md`
4. `testing/ocr_slide_blackboard_samples.md`
5. `codex_stage4_low_risk_tasks.md`

## 8. 当前最高优先级

1. Stage 5 Mega Sprint：Export Center + 本地音视频 / 字幕导入 + ASR 地基 + OCR 多页 + 双语报告。
2. 不做第三方平台视频爬取，不做未授权内容抓取。
3. 暂缓云同步 / 团队协作，除非后续有服务器和合规方案。
4. 密钥永远不入仓、不进日志、不进截图、不进导出。

## 9. Stage 7 演示与 Proof 包

### Stage 7 Demo / Proof 文档

- `competition/stage7_final_demo_script.md`：8-10 分钟复赛总演示脚本。
- `competition/stage7_judge_qna_50.md`：50 个评委问答。
- `competition/stage7_competitor_positioning.md`：录音转写类产品对比与 ClassMate 定位。
- `competition/stage7_proof_screenshot_list.md`：复赛截图 / 录屏 proof 清单。
- `competition/stage7_feature_matrix_for_reviewers.md`：评审版功能矩阵。
- `competition/stage7_submission_package_checklist.md`：复赛提交包检查清单。

### Stage 7 Scripts

- `scripts/qa/stage7_status_snapshot.ps1`：状态快照，不运行 Gradle。
- `scripts/qa/stage7_demo_preflight.ps1`：演示前轻量检查。
- `scripts/qa/stage7_sensitive_text_audit.ps1`：敏感文本和夸大宣传 WARN 扫描。
- `scripts/proof/build_stage7_proof_pack.ps1`：复赛 proof pack 生成器。
- `scripts/proof/README.md`：proof pack 脚本用法。

### Stage 7 Regression Plan

- `testing/stage7_full_regression_plan.md`：完整真机回归路线。
- `testing/stage7_scripts_usage.md`：Stage 7 QA 脚本使用说明。
- `testing/stage7_asr_smoke.md`：Live ASR 实验 smoke。

### Stage 7 Demo Assets

- `demo_assets/stage7_demo_narration_short.md`：2 分钟短旁白。
- `demo_assets/stage7_demo_narration_long.md`：8-10 分钟完整旁白。
- `demo_assets/stage7_test_questions_for_live_demo.md`：现场演示口播素材、Ask 问题和导出检查问题。

## 10. Stage 7H 交付自动化与项目管理

### 演示资产

- `demo_assets/stage7_slide_outline.md`：12 页复赛 PPT 大纲。
- `demo_assets/stage7_slide_image_prompt_pack.md`：12 页设计工具提示词包。
- `demo_assets/stage7_video_recording_script.md`：完整版、短版和 30 秒开场录制脚本。

### 复赛提交材料

- `competition/stage7_readme_draft.md`：可迁移到 README 的项目介绍草稿。
- `competition/stage7_release_notes_draft.md`：复赛版本 release notes 草稿。
- `competition/stage7_architecture_one_pager.md`：评审可读的一页架构说明。

### GitHub 项目管理

- `issues/stage7_github_issues_backlog.md`：30 个后续 GitHub Issue 草案。
- `issues/stage7_labels_and_milestones.md`：labels 和 milestones 建议。
- `issues/stage7_commit_split_plan.md`：当前混合工作区后续拆 commit 计划。

### Proof / Demo 自动化

- `scripts/proof/check_stage7_proof_pack.ps1`：proof pack 校验脚本。
- `scripts/demo/make_stage7_demo_folder.ps1`：演示文件夹生成脚本。
- `testing/stage7_proof_scripts_usage.md`：Stage 7 proof/demo 脚本使用说明。

### 测试与风险

- `testing/stage7_device_test_result_template.md`：60 项真机测试结果记录模板。
- `testing/stage7_bug_report_template.md`：bug 报告模板。
- `testing/stage7_risk_register.md`：Stage 7 风险台账。

## 11. Stage 8A OnDevice BlueLM / Model Config QA Pack

本节是 Codex Stage 8A-Docs 新增的端侧 BlueLM 3B 与模型配置 QA 材料索引。当前材料只定义架构、验收、审查和静态审计，不代表端侧 SDK 已经接通或真机测试已通过。

### OnDevice architecture

- `architecture/stage8_ondevice_bluelm_architecture.md`：端侧 3B BlueLM / Official BlueLM / LocalRule 分层架构、ProviderChain、任务 profile、权限风险和文本审核 SDK 后续接入边界。

### Smoke checklist

- `testing/stage8_ondevice_smoke_checklist.md`：云真机 X300 Pro、`/sdcard/1225`、SDK、native library、init/generate、TokenCallback、interrupt、release、弱网和 unavailable 场景的真机 smoke 清单。

### Model config acceptance

- `testing/stage8_model_config_acceptance.md`：Official BlueLM AppID/AppKEY/model 持久化配置、掩码显示、测试连接、删除配置、密钥不导出和复赛短标签验收清单。
- `product/stage8_model_api_management_spec.md`：模型 API 管理产品规格，覆盖复赛模式和长期产品自定义模型接口边界。

### Talking points

- `competition/stage8_ondevice_talking_points.md`：复赛/答辩口径，说明端侧模型用于弱网、隐私和本地复习兜底，不替代 Official BlueLM 云端主路径。

### AI guardrails

- `product/stage8_ai_feature_boundaries.md`：每个核心功能接 AI 的工程边界，强调结构化数据、AI 增强层、validator、redaction 和 export safety。

### Failure matrix

- `issues/stage8_ondevice_failure_matrix.md`：端侧模型 30+ 条失败矩阵，覆盖 SDK、native、模型目录、权限、callback、validator、fallback、密钥和复赛 UI 风险。

### Review prompt

- `prompts/claude_stage8_ondevice_red_team_review.md`：Claude 完成 Stage 8A 生产代码后的红队审查提示词。

### Static audit script

- `scripts/qa/stage8_ondevice_static_audit.ps1`：WARN-only 只读静态审计脚本，不运行 Gradle，不读取 `config.local.json` 内容。
- `testing/stage8_ondevice_static_audit_usage.md`：Stage 8A 静态审计脚本使用说明。
## 12. Stage 8A-2 Real SDK Bridge QA Pack

本节是 Stage 8A-2 真实端侧 SDK bridge 前的 QA 与交接材料。当前材料记录 AAR 构建事实、真实 SDK smoke 计划、多模态 bridge 设计、Claude 实现提示词和只读脚本；不代表端侧模型已经真机跑通，也不代表多模态已经完整进入学习主链路。

### SDK build record

- `testing/stage8a2_ondevice_sdk_build_record.md`：官方 demo 复制到英文路径、local.properties、NDK/CMake、AAR 路径、大小、时间、AAR 不入仓原因和 javap 复核方式。

### Real SDK smoke

- `testing/stage8a2_real_sdk_smoke_plan.md`：AAR 静态检查、Settings 纯文本 init/generate、多模态 init/callVit、云真机模型路径和 native 风险的 smoke 清单。
- `testing/stage8a2_real_device_result_record.md`：Stage 8A 真机 proof 记录（vivo X300 Pro, OriginOS 6, Android 16），文本 init/generate success，多模态 callVit/generate success，截图不提交 Git。

### Multimodal bridge design

- `architecture/stage8a2_multimodal_bridge_design.md`：纯文本与多模态共用 LlmManager、`multimodal=true`、Bitmap RGB 转换、callVit、无参完成回调、reflection bridge 和后续 ImageUnderstandingSource 路线。

### Claude prompt and issues

- `prompts/claude_stage8a2_real_sdk_bridge.md`：明天交给 Claude 的真实 SDK bridge 实现提示词。
- `issues/stage8a2_real_sdk_bridge_backlog.md`：20 个后续 GitHub Issue 草案，覆盖 optional AAR include、reflection bridge、callVit、model path、fallback、权限守卫和真机 smoke。

### QA scripts

- `scripts/qa/stage8a2_sdk_preflight.ps1`：只读 WARN-only SDK preflight，检查 AAR、git ignore、javap、多模态签名、native libs、direct import、qwen guard 和 forbidden tracked files。
- `scripts/qa/stage8a2_demo_sdk_build_helper.ps1`：官方 demo AAR 构建辅助脚本；默认只显示帮助，只有显式参数才写 local.properties、运行构建或复制 AAR。

## 13. Stage 8A-3 Device Proof and Follow-up Pack

本节是 Stage 8A-3 端侧 SDK 真机测试与复赛 proof 材料包。当前材料记录真机测试计划、proof pack 清单、演示脚本、错误文案库、后续 issue 草案和执行单；不代表端侧模型已经真机跑通，也不代表 bridge 已完成。

### Device test

- `testing/stage8a3_real_device_test_sheet.md`：55+ 项真机测试记录表，覆盖安装/环境、纯文本、多模态、fallback、安全、复赛演示六组。
- `testing/stage8a3_tomorrow_work_order.md`：明天 Claude 工作执行单，包含前置确认、预检、bridge 实现、真机测试、失败分类、提交策略和禁止事项。

### Competition proof pack

- `competition/stage8a3_ondevice_proof_pack_checklist.md`：17 张复赛 proof 截图清单，每项含文件名建议、截图位置、证明点、不能暴露的信息和失败替代方案。
- `competition/stage8a3_ondevice_demo_script.md`：6–8 分钟端侧模型专题演示脚本，11 段话术，覆盖端侧分层、SDK 诊断、纯文本/多模态演示、断网 proof、安全隐私和失败兜底。
- `competition/stage8a2_judge_qna_ondevice.md`：25 个评委问答（Stage 8A-2），覆盖端侧接入、真实 SDK、多模态、隐私、fallback、证明等维度。

### Product copy

- `product/stage8a2_ondevice_user_copy.md`：端侧模型 UI 文案（Stage 8A-2），覆盖 BlueLM 3B 说明、多模态实验、模型路径、SDK/初始化/VIT 状态提示、隐私和权限。
- `product/stage8a3_ondevice_error_copy.md`：20 条端侧 SDK 错误文案库，每条含用户文案、调试标签、建议操作和是否阻塞复赛。

### Follow-up issues

- `issues/stage8a3_after_bridge_followups.md`：30 个后续 GitHub Issue 草案，覆盖性能记录、Ask/Practice/Report 接入、多模态图片理解、ImageUnderstandingSource、权限审计、错误码映射、内存泄漏、APK 体积、决赛预留等。

### QA scripts

- `testing/stage8a2_manual_command_cheatsheet.md`：Stage 8A-2 手动命令速查，覆盖 AAR 构建、javap、git check-ignore、secrets scan、qwen guard 等 9 个命令区。
- `scripts/qa/stage8_ondevice_static_audit.ps1`：WARN-only 静态审计脚本（已增强 Stage 8A-2 检查项），含 TokenCallback.onComplete() 无参检查和 callVit/multimodal 引用扫描。
- `scripts/proof/build_stage8_ondevice_proof_pack.ps1`：Stage 8A-3 proof pack 生成器，支持 `-DryRun` / `-Build` / `-Zip` / `-Open`，不复制 APK/AAR 和敏感配置。

## 14. Stage 8A-4 Post-Bridge Red Team and Regression Pack

本节是 Stage 8A-4 后置红队审查与回归测试材料包。当前材料在 Claude 完成真实 bridge 后使用，用于 adversarial 审查、回归测试和答辩失败话术；不代表端侧模型已经真机跑通。

### Regression plan

- `testing/stage8a4_post_bridge_regression.md`：20 项完整回归测试路线，覆盖编译测试、静态检查、代码审查、真机功能、fallback、redaction 和破坏性测试，按依赖顺序排列。

### Red team review

- `prompts/claude_stage8a2_post_bridge_red_team.md`：后置红队审查提示词，13 个审查维度：direct import、CI-safe、TokenCallback 签名、callVit 范围、AAR 不提交、权限审计、prompt/output 不记录、云端主路径、qwen guard、ProviderResolver/validators、Settings 外部模型文案、fallback、真机失败文案诚实性。

### Judge Q&A / failure talking points

- `competition/stage8a4_ondevice_failure_talking_points.md`：10 个端侧真机失败场景答辩话术，覆盖 init 失败、模型路径、权限、native、NPU、callVit、多模态 generate、断网、LocalRule、综合性质疑"你到底做完了没有"。

### QA scripts

- `scripts/qa/stage8a4_post_bridge_static_check.ps1`：只读 WARN-only 后置静态检查脚本，10 项检查：direct import、onComplete 旧签名、callVit/multimodal 引用、AAR gitignored、forbidden tracked files、qwen guard、ProviderResolver 完整性、validators 完整性、Manifest 危险权限、Settings 外部模型文案。

## 15. Stage 8A-5 Milestone PR / Release / Submission Draft Pack

本节是 Stage 8A-5 里程碑文档包，包含 PR 描述、Release Notes、评审一页摘要、用户故事地图和提交前 checklist。面向复赛提交和项目交接。

### PR & Release

- `pr/stage8_ondevice_pr_description.md`：端侧 BlueLM 3B PR 描述草稿，含背景、API 验证、安全边界、测试计划、AAR 不入仓原因、复赛展示口径、风险与缓解。
- `release/stage8_ondevice_release_notes_draft.md`：Release notes 草稿，分 Added / Changed / Security / Known Limitations / Not Included Yet / Device Testing Status 六节，明确标注真机测试待执行。

### Competition submission

- `competition/stage8_ondevice_one_page_summary.md`：评审一页摘要，含三层架构图、云端主路径、端侧亮点与诚实边界、学习闭环差异化、隐私与弱网价值、当前完成度表、下一步规划。

### Product

- `product/stage8_ondevice_user_story_map.md`：五类用户故事地图（大学生、考研备考者、在职学习者、课程老师、弱网/隐私场景），每类含使用流程、端侧价值、风险；附优先级矩阵和不适合场景。

### Commit readiness

- `testing/stage8_ondevice_commit_checklist.md`：生产代码提交前 checklist，含必跑命令（A）、必查文件（B）、禁止暂存文件（C）、git add 范围建议（D）、commit message 建议（E）、CI 检查（F）、真机 smoke（G）。

## 16. Stage 8A-6 Documentation Consistency Audit

本节是对 Stage 8A / 8A-2 / 8A-3 / 8A-4 / 8A-5 全部 35 个文档和脚本的一致性审计。审计结论：**全部通过，无实质性错误**。

### Audit report

- `testing/stage8_ondevice_docs_consistency_audit.md`：完整审计报告，含 11 项事实基线、35 个检查文件列表、9 项专项检查（错误包名、旧签名、callVit 返回类型、过度表述等）、审计结论和文档质量评价。

### Enhanced QA scripts

- `scripts/qa/stage8a4_post_bridge_static_check.ps1`：已增强 Stage 8A-6 检查项：新增 #11 错误包名 `com.blue.lm.sdk` 检测、#12 错误 `void callVit` 返回类型检测、#13 过度表述短语检测（含免责声明过滤）。

## 17. Stage 8B Multimodal Pipeline + Local Moderation Roadmap

本节是 Stage 8B 多模态学习管线与端侧审核路线图。当前为架构设计和规划文档，不声称功能已实现或真机已跑通。

### Architecture

- `architecture/stage8b_multimodal_learning_pipeline.md`：端侧多模态进入学习主链的完整架构设计，包含目标链路（截图→VIT→MaterialBundle→Ask/Quiz/Report）、4 种新来源类型、安全边界 7 条、prompt 设计 5 类、6 阶段渐进路线。
- `architecture/stage8b_local_text_moderation_plan.md`：端侧文本审核 SDK（CmsLocalFrame）接入规划，6 个接入点（导出/分享/AI输出/练习解析/多模态入库/自定义模型）、UI 文案（0/1/2）、fallback 策略、proof 截图建议。

### Product & Competition

- `product/stage8b_image_learning_user_flow.md`：12 步图片学习用户流程，从拍照到 StudyReport，每步含页面入口、用户动作、系统行为、AI 参与点、失败兜底和隐私提示。
- `competition/stage8b_multimodal_talking_points.md`：复赛多模态答辩口径，8 个重点问题（学习场景/OCR区别/端侧先行/不上传原因/诚实完成度/兜底/决赛方向）和 7 条禁止话术。

### Testing

- `testing/stage8b_multimodal_test_inputs.md`：6 大多模态测试素材说明（高数课件/物理板书/离散数学图论/C++代码/ML公式/错误图片），每项含期望输出、不应编造内容、Ask/Quiz 问题、Report 呈现方式。

### Backlog & Prompts

- `issues/stage8b_multimodal_and_moderation_backlog.md`：30 个 GitHub Issue 草案，覆盖 RGB转换、prompt模板、4 种图像来源、元数据、redaction、validator、MaterialBundle、Timeline、Ask引用、Quiz生成、StudyReport、隐私、审核 seam、proof 等。
- `prompts/claude_stage8b_multimodal_pipeline.md`：Claude 未来任务提示词（前提：Stage 8A-2 bridge 完成），目标为最小闭环：图片→MaterialBundle→Timeline。
- `prompts/codex_stage8b_testing_and_proof.md`：Codex 后续任务提示词（前提：Claude 生产代码完成），6 项产出（真机测试表、proof 截图清单、审核测试模板、评委问答、proof pack 增强、INDEX 更新）。

## 18. Stage 8A-7 Device-Wait QA Helper Pack

本节是 Stage 8A-7 云真机排队等待期间的 QA 辅助材料包。当前材料为只读辅助脚本、真机测试 runbook、bug triage 模板、proof 命名规范、文案审查表和预写失败 issue 草案；**不代表端侧模型已经真机跑通**，也不代表多模态已完整进入学习主链路。

### Device-wait QA script

- `scripts/qa/stage8a7_device_wait_helper.ps1`：只读/辅助 PowerShell 脚本，支持 `-Info` / `-Preflight` / `-AdbDevices` / `-Install` / `-Launch` / `-LogcatOnDevice` / `-PullScreenshots` / `-AllLight` / `-Help` 参数，不运行 Gradle、不读 config.local.json 内容、不复制 APK/AAR 到 git 目录。

### Testing & runbook

- `testing/stage8a7_cloud_device_test_runbook.md`：云真机测试操作手册，按顺序覆盖排队前准备、进入云真机后检查、端侧纯文本测试、多模态 diagnostic 测试、fallback 测试、学习主链回归、失败分类与处理、测后整理和快速命令速查。
- `testing/stage8a7_ondevice_bug_triage_template.md`：端侧 SDK bug triage 模板，每 bug 含 Bug ID、设备信息、复现步骤、错误详情、logcat 关键行、截图编号、是否阻塞复赛演示、初步归因、临时 workaround、后续 owner 和是否需要 Claude 修复。

### Competition proof naming

- `competition/stage8a7_device_test_proof_naming.md`：proof 截图/录屏命名规范，覆盖 13 类 proof（SDK_PRESENT、AAR_GITIGNORED、JAVAP_MULTIMODAL、SETTINGS_TEXT_INIT_SUCCESS、SETTINGS_TEXT_GENERATE_SUCCESS、SETTINGS_MULTIMODAL_CALLVIT_SUCCESS、SETTINGS_MULTIMODAL_GENERATE_SUCCESS、FALLBACK_LOCAL_RULE、NO_DANGEROUS_STORAGE_PERMISSION、QWEN_GUARD、SECRETS_SCAN_OK、CI_ANDROID_OK、CLOUD_DEVICE_FAILURE_FALLBACK），含推荐文件名、截图内容和禁止暴露信息。

### Product copy review

- `product/stage8a7_settings_diagnostic_copy_review.md`：Settings 诊断页面文案审查表，15 条审查项（SDK 已发现/未发现、模型路径、初始化和生成成功/失败、多模态支持、VIT 编码成功/失败、LocalRule 兜底、断网、不上传、不保存原图、实验功能说明），每条含推荐文案、禁止文案、原因和截图适配性。

### Pre-written failure issues

- `issues/stage8a7_cloud_device_failure_issues.md`：20 个云真机失败场景预写 GitHub Issue 草案，覆盖模型路径、init 超时、native 加载、NPU/APU、callVit、无 token、onComplete 未调用、onError 无码、Settings 空状态、fallback 未显示、bridge crash、release 生命周期、重复 init、ABI 不匹配、minSdk 覆盖、路径权限、多模态 prompt、RGB 转换、proof 截图缺失和云真机文件不可部署。
## 19. Stage 8C QA / Device Test / Proof Pack

本节是 Stage 8C Input-to-Learning 真机测试与 proof 自动化材料。当前事实基线：Stage 8A-2.2 已在 vivo X300 Pro 上跑通端侧文本和多模态诊断；Stage 8C 已把云端蓝心、端侧蓝心和安全占位纳入课程分析安全链路。以下材料用于明天真机测试、截图 proof 和复赛交付整理。

### Device helper

- `scripts/qa/stage8c_device_helper.ps1`：Stage 8C 真机辅助脚本，支持 `-Info`、`-Preflight`、`-AdbDevices`、`-Install`、`-Launch`、`-LogcatStage8C` 和 `-AllLight`；不运行 Gradle，不清理设备数据，不复制 AAR/APK 到 Git 目录。

### Smoke and inputs

- `testing/stage8c_device_smoke.md`：Stage 8C 真机 smoke 路线，覆盖权限中心、端侧文本/多模态诊断、云端/端侧/安全占位 CourseAnalysis、图片/拍照草稿确认链路、Ask/Quiz/Review/Practice/Export 回归。
- `testing/stage8c_test_inputs.md`：可直接复制进 App 的高数、物理、离散数学、C++ 文本课程输入，纸质题目/板书图片文字内容，Ask 问题、Quiz/Review 检查点和导出关键词检查。

### Proof checklist

- `competition/stage8c_proof_screenshot_list.md`：Stage 8C proof 截图清单，覆盖模型目录授权、文本生成、多模态 callVit、课程分析、端侧 fallback、安全占位、图片/拍照草稿、Ask 来源标签、Report/Practice/Export、AAR gitignored 和 CI。

### Proof automation

- `scripts/proof/build_stage8c_proof_pack.ps1`：Stage 8C proof pack 生成器，支持 `-DryRun`、`-Zip`、`-Open`；只复制文档和脚本，记录外部 proof assets 路径，不复制 APK、AAR、config 或截图本体。

## 20. Stage 8C Competition Pack — Judge Q&A / Demo Scripts / Risk Register

本节是 Stage 8C 复赛交付文档包，包含评委问答、演示脚本、短视频脚本、风险台账和用户价值地图。当前事实基线：Stage 8A-2.2 已在 vivo X300 Pro 上跑通端侧文本和多模态诊断；Stage 8C 已把云端蓝心、端侧蓝心和安全占位纳入课程分析安全链路。

### Judge Q&A

- `competition/stage8c_judge_qna_60.md`：60 个评委问答，分七组（产品定位 8、技术架构 9、端侧能力 10、输入链路 8、学习闭环 8、安全合规 9、未来路线 8）。回答诚实，不夸大端侧能力，不展示 DeepSeek/Compatible 作为主路径，不声称多模态替代 OCR。

### Demo scripts

- `competition/stage8c_demo_script_10min.md`：8–10 分钟复赛演示脚本，10 段结构（定位、架构、真机 proof、文本→Timeline→Evidence、图片/拍照→草稿→确认、Ask/Quiz/Review/Practice、Export、弱网 fallback、vivo 特色、总结），每段含操作、讲解词、截图点和风险兜底话术。
- `competition/stage8c_2min_video_script.md`：2 分钟短视频脚本，8 段快节奏展示（问题、闭环、架构、proof、图片草稿、Ask/Quiz/Review、报告、结尾），不讲技术细节，强调学习闭环、vivo 能力和可打印报告。

### Risk register

- `issues/stage8c_risk_register.md`：45 条风险台账，分十大类（端侧模型 7、CourseAnalysis JSON 5、validators 5、图片/拍照链路 6、权限 4、真机兼容 5、导出报告 4、安全/密钥 5、评委质疑 4、时间/期末周 2）。每条含风险描述、触发条件、影响、当前缓解措施、owner 和 P0/P1/P2 优先级。

### User value map

- `product/stage8c_user_value_map.md`：五类用户价值地图（大学生课堂、考研/考公备考、在职学习/培训、教师备课/复盘、弱网/隐私/本地学习）。每类用户覆盖痛点、输入资料、处理链路、输出结果、端侧能力价值和复习闭环价值。
