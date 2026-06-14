# Stage 7 Proof Screenshot List

截图原则：不拍密钥明文、不拍本地配置文件、不拍完整鉴权内容、不拍模型原始交互、不拍私密账号和真实学生隐私。

## Settings / Provider / BlueLM Diagnostic

1. `settings_provider_profile`：Settings 模型配置；已导入配置；证明 Official BlueLM / Compatible / LocalFallback 分层；不能拍密钥输入框。
2. `settings_bluelm_ready`：BlueLM 状态；已完成 debug 导入；证明官方路径可用；不能拍密钥明文。
3. `settings_diagnostic_ok`：连接测试结果；点击测试连接；证明 HTTP 200/OK；不能拍完整请求或响应。
4. `settings_capability_roadmap`：能力路线图；无前置；证明已支持/实验/待接入/暂缓；不能出现夸大文案。
5. `settings_privacy_security`：隐私说明；无前置；证明密钥和内部交互不进日志/导出；不能拍 debug 输入区。

## Import Flow

6. `import_type_picker`：导入资料类型页；打开 Import；证明文本、示例、文件、OCR、字幕入口；不能拍私人文件列表。
7. `material_tray_text`：资料篮文本；加入课堂文本；证明资料进入 MaterialBundle；不能拍隐私文本。
8. `import_settings_subject`：分析设置；资料篮后进入；证明课程标题/科目/模型 profile；不能拍密钥。
9. `sample_lesson_loaded`：示例课堂；选择示例课堂；证明长文本测试入口；不能拍真实学生材料。

## Markdown 导入

10. `markdown_picker`：文件选择；选择测试 Markdown；证明 .md 可导入；不能拍真实文件路径。
11. `markdown_clean_text`：导入后资料篮；导入完成；证明 Markdown 转纯文本；不能拍私人文档。

## OCR 资料流

12. `ocr_slide_input`：课件 OCR 输入；粘贴课件 OCR；证明手动 OCR 资料流；不能声称自动识别。
13. `ocr_blackboard_input`：板书 OCR 输入；粘贴板书 OCR；证明板书来源；不能拍个人照片。
14. `ocr_pdf_input`：PDF/讲义 OCR 输入；粘贴讲义 OCR；证明讲义来源；不能拍本地路径。
15. `ocr_material_list`：资料列表；加入多条 OCR；证明来源类型可区分；不能拍敏感文件名。

## Transcript / SRT / VTT

16. `transcript_import_type`：转写稿导入；选择 SRT/VTT/TXT；证明字幕入口；不能拍真实视频平台账号。
17. `transcript_editor_segments`：转写编辑器；导入字幕；证明时间戳和段落；不能拍隐私转写。
18. `transcript_speaker_manual`：手动说话人标签；修改 speaker；证明手动标签；不能声称声纹完成。
19. `transcript_material_tray`：转写稿进入资料篮；保存转写；证明可进入分析。

## Live ASR 实验模式

20. `live_manual_mode`：Live 手动课堂；打开 Live；证明手动/模拟边界；不能声称真实 provider。
21. `live_asr_experiment_notice`：ASR 实验提示；点击 ASR 区域；证明依赖系统识别、不保存原始音频。
22. `live_segment_added`：Live 片段；添加片段；证明课堂资料可生成时间线。
23. `live_to_timeline`：Live 结束分析；结束课堂；证明进入正式分析。

## Timeline / Evidence

24. `timeline_overview`：知识时间线；分析成功；证明知识点结构化。
25. `timeline_source_marker`：来源 marker；打开含 OCR/转写来源的课程；证明来源可追溯。
26. `evidence_detail_quote`：证据详情；点开知识点；证明 evidence quote。
27. `validator_pass_log`：短日志；分析成功；证明校验通过；不能拍模型原始响应。

## Ask This Lesson

28. `ask_grounded`：Ask grounded；问有证据问题；证明引用证据回答。
29. `ask_partial`：Ask partial；问部分依据问题；证明降级。
30. `ask_not_found`：Ask not_found；问超出本课问题；证明拒绝胡编。

## Quiz

31. `quiz_question`：微测题；进入 Quiz；证明题目来自知识点。
32. `quiz_answer_explanation`：答案解释；完成答题；证明解释和证据。
33. `quiz_feedback`：反馈按钮；答错或太难；证明进入学习状态。

## Review / 需要多练

34. `review_today_tasks`：今日复习；进入 Review；证明任务生成。
35. `review_weakness`：薄弱点；产生错题/太难；证明薄弱点聚合。
36. `review_need_more_practice`：需要多练；标记需要多练；证明练习入口。

## Practice Search

37. `practice_search_panel`：练习搜索；打开找练习；证明只打开外部搜索。
38. `practice_search_copy`：复制搜索词；无浏览器或手动复制；证明 fallback。
39. `practice_no_crawling_copy`：说明文案；打开面板；证明不爬取平台。

## Course Library

40. `course_library_overview`：课程库；有多门课程；证明按课程聚合。
41. `course_library_search`：搜索课程；输入关键词；证明搜索。
42. `course_library_filter`：筛选 chip；切换待复习/BlueLM；证明筛选。
43. `course_detail`：课程详情；点课程卡；证明课程级入口。

## Export Center / StudyReport

44. `export_center_formats`：导出中心；打开导出；证明 PDF/HTML/Markdown/TXT/MindMap 等格式。
45. `export_save_as`：保存到文件；选择位置；证明用户能拿到文件。
46. `export_share_sheet`：系统分享；点击分享；证明可分享。
47. `study_report_pdf`：PDF 报告；打开 PDF；证明打印级报告。
48. `study_report_security`：导出文件安全检查；打开报告；证明不含敏感配置或内部模型内容。

## Security Proof

49. `secrets_scan_pass`：本地 secrets scan；运行脚本；证明安全扫描通过。
50. `git_sensitive_ls_empty`：敏感追踪检查；运行 git ls-files；证明无敏感文件被追踪。
51. `manifest_permissions`：权限检查；展示脚本结果；证明权限边界。

## GitHub Actions / CI

52. `actions_latest_green`：GitHub Actions 页面；打开最新 run；证明 CI 状态。
53. `actions_secrets_scan`：Secrets Scan job；进入 job；证明扫描通过。
54. `actions_android_ci`：Android CI job；进入 job；证明构建/测试结果。

