# Stage 8C Proof 截图清单

截图前请开启勿扰，隐藏账号信息，不拍密钥输入框，不拍完整日志，不拍本地私密路径。

| # | 推荐文件名 | 截图内容 | 证明点 | 不可暴露信息 | 失败替代 proof |
|---|---|---|---|---|---|
| 1 | `stage8c_01_model_dir_authorized.png` | 端侧模型目录授权成功 | `/sdcard/1225` 可访问 | 私密目录、账号 | Settings 诊断显示 modelPath ok |
| 2 | `stage8c_02_text_init_generate_ok.png` | 文本 init/generate 成功 | 端侧文本模型可运行 | 完整输入输出 | 诊断短状态 |
| 3 | `stage8c_03_multimodal_callvit_ok.png` | callVit/generate 成功 | 多模态诊断可运行 | 图片隐私、完整输出 | callVit code 0 摘要 |
| 4 | `stage8c_04_cloud_analysis_ok.png` | 文本课程分析成功 | 云端蓝心主路径可用 | 密钥、完整日志 | Timeline 成功页 |
| 5 | `stage8c_05_ondevice_analysis_fallback_ok.png` | 云端失败后端侧 CourseAnalysis 成功 | 端侧 fallback seam 可用 | 完整模型输出 | provider path + validation pass |
| 6 | `stage8c_06_invalid_json_no_persist.png` | invalid JSON 后不落库 | parse failed 安全拦截 | 原始响应正文 | History 无新增记录 |
| 7 | `stage8c_07_validator_failed_no_persist.png` | validators failed 后不落库 | 校验器仍生效 | 原始模型内容 | validation failed 摘要 |
| 8 | `stage8c_08_dual_fail_safety_placeholder.png` | 双模型失败安全占位 | LocalRule 已降级为安全占位 | 完整错误堆栈 | 安全占位 UI |
| 9 | `stage8c_09_image_import_entry.png` | 图片导入入口 | 图片入口可见 | 相册隐私 | Import Hub 截图 |
| 10 | `stage8c_10_camera_import_entry.png` | 拍照导入入口 | 拍照入口可见 | 真实人脸/环境隐私 | 空白纸测试 |
| 11 | `stage8c_11_image_draft_editable.png` | 图片多模态生成可编辑草稿 | 不自动污染知识库 | 完整图片路径 | 草稿编辑页 |
| 12 | `stage8c_12_draft_confirm_analysis.png` | 草稿确认进入 CourseAnalysis | 用户确认后才分析 | 原始完整输出 | Timeline 结果 |
| 13 | `stage8c_13_draft_cancel_no_persist.png` | 草稿取消不落库 | 取消安全 | 私密路径 | History count 对比 |
| 14 | `stage8c_14_ask_cloud_label.png` | Ask 来源标签：云端蓝心 | 来源透明 | 密钥 | Ask 结果 |
| 15 | `stage8c_15_ask_ondevice_label.png` | Ask 来源标签：端侧蓝心 | 端侧来源透明 | 完整模型输出 | provider path |
| 16 | `stage8c_16_ask_safety_placeholder.png` | Ask 来源标签：安全占位 | 不伪装 AI 成功 | 完整错误 | fallback 文案 |
| 17 | `stage8c_17_report_ondevice_suggestion.png` | Report 端侧建议 | 端侧可做建议增强 | 内部推理字段 | 报告摘要 |
| 18 | `stage8c_18_practice_next_step.png` | Practice 下一步建议 | 练习闭环可演示 | 第三方平台隐私 | 搜索词卡片 |
| 19 | `stage8c_19_export_redaction_ok.png` | Export 安全检查 | 导出不含敏感类别 | 密钥、完整模型交互 | 搜索结果截图 |
| 20 | `stage8c_20_aar_gitignored.png` | AAR gitignored | SDK 不入仓 | 本地完整私密路径 | `git check-ignore` 输出 |
| 21 | `stage8c_21_ci_pass.png` | GitHub Actions 通过 | CI 健康 | token、账号隐私 | workflow summary |
| 22 | `stage8c_22_qwen_guard.png` | qwen guard 静态检查 | 云端长文本保护仍在 | 源码密钥 | QA script 输出 |
| 23 | `stage8c_23_no_direct_sdk_import.png` | 无直接 SDK import | reflection bridge 边界 | 无 | preflight 输出 |
| 24 | `stage8c_24_native_libs_arm64.png` | native libs arm64-v8a | AAR 适配目标 ABI | SDK 私有内容大段 | preflight 摘要 |
| 25 | `stage8c_25_permissions_center.png` | 权限中心 | 权限边界清楚 | 账号/通知内容 | 系统权限页 |
| 26 | `stage8c_26_course_library_record.png` | Course Library 记录 | 成功落库后可回看 | 私人课程名 | 测试课程 |
| 27 | `stage8c_27_quiz_evidence.png` | Quiz 证据解释 | 微测仍有证据链 | 完整原文长段 | 题目和证据短句 |
| 28 | `stage8c_28_review_task.png` | Review 任务 | 学习闭环正常 | 个人学习隐私 | 测试课程任务 |
| 29 | `stage8c_29_pdf_report.png` | PDF/报告预览 | 打印级报告可用 | 本地文件路径 | 文件名和摘要 |
| 30 | `stage8c_30_device_helper_preflight.png` | stage8c helper preflight | QA 自动化可复现 | 私密路径 | PASS/WARN 摘要 |
