# Stage 7 GitHub Issues Backlog

## P0 真机测试阻塞

### 1. Fix Stage 7C Review/Practice compile blockers
- Labels: p0, bug, review-loop, test
- Background: Adaptive Practice 半成品可能阻塞 app 构建。
- Acceptance: app unit test 和 assembleDebug 通过。
- Non-goals: 不重写 Review 架构。
- Risk: 影响复赛 APK。
- Proof/Test: Stage 7 full regression。

### 2. Run full real-device smoke for latest APK
- Labels: p0, test, proof, competition
- Background: 需要确认云真机和本地真机表现。
- Acceptance: 完成 60 项测试记录。
- Non-goals: 不临时加新功能。
- Risk: 演示中断。
- Proof/Test: device test result template。

### 3. Validate BlueLM long lesson path after Stage 7 changes
- Labels: p0, provider, test
- Background: 主链路必须稳定。
- Acceptance: 标准长课样例分析成功，fallback 标注正确。
- Non-goals: 不改 provider 协议。
- Risk: 官方路径演示失败。
- Proof/Test: BlueLM diagnostic + Timeline。

### 4. Verify no sensitive files are tracked
- Labels: p0, security, proof
- Background: 复赛提交前必须确认。
- Acceptance: forbidden tracked files 无输出，secrets scan 通过。
- Non-goals: 不扫描用户私有文件内容。
- Risk: 密钥泄漏。
- Proof/Test: security summary。

### 5. Confirm Export Center works on cloud device
- Labels: p0, export, test
- Background: 普通用户需要拿到报告。
- Acceptance: 保存到文件、分享和下载目录兜底至少一条成功。
- Non-goals: 不做云同步。
- Risk: 报告无法交付。
- Proof/Test: Export screenshot。

## P0 Export / StudyReport

### 6. Polish printable StudyReport layout
- Labels: p0, export, ui
- Acceptance: PDF/HTML 报告适合打印，标题、证据、微测不混乱。
- Non-goals: 不生成原生 docx/pptx。
- Risk: 答辩材料观感。
- Proof/Test: report acceptance checklist。

### 7. Add report safety final audit
- Labels: p0, security, export
- Acceptance: 导出不含密钥、本地配置、内部模型交互和推理字段。
- Non-goals: 不改变学习内容。
- Risk: proof 泄漏。
- Proof/Test: sensitive text audit。

## P0 BlueLM 主链路

### 8. Capture final Official BlueLM proof screenshots
- Labels: p0, provider, proof
- Acceptance: Settings、diagnostic、Timeline、Evidence 四类 proof。
- Non-goals: 不提交密钥截图。
- Risk: 合规路径说服力不足。
- Proof/Test: screenshot list。

### 9. Keep qwen guard regression test green
- Labels: p0, provider, test
- Acceptance: qwen3.5-plus guard 存在，深度思考关闭逻辑不回退。
- Non-goals: 不换模型。
- Risk: 长响应超时。
- Proof/Test: qwen guard check。

## P1 ASR provider

### 10. Design vivo ASR provider integration
- Labels: p1, asr, provider
- Acceptance: 官方文档确认 endpoint/body/response 后出实现计划。
- Non-goals: 不猜接口。
- Risk: 合规接入延迟。
- Proof/Test: architecture note。

### 11. Stabilize system ASR experimental UX
- Labels: p1, asr, ui
- Acceptance: 权限、不可用、错误状态文案清楚。
- Non-goals: 不保存原始音频。
- Risk: 真机差异。
- Proof/Test: ASR smoke。

### 12. Add ASR transcript quality checklist
- Labels: p1, asr, test
- Acceptance: 覆盖口播、时间戳、手动编辑和资料篮。
- Non-goals: 不做声纹识别。
- Risk: 转写质量不可控。
- Proof/Test: live demo questions。

## P1 OCR provider

### 13. Design vivo OCR provider integration
- Labels: p1, ocr, provider
- Acceptance: 官方文档确认后实现 provider seam。
- Non-goals: 不猜 endpoint。
- Risk: OCR 质量影响证据。
- Proof/Test: OCR samples。

### 14. Multi-page OCR material polish
- Labels: p1, ocr, ui
- Acceptance: 多页课件/讲义来源 marker 清晰。
- Non-goals: 不做真实图片识别。
- Risk: 来源混乱。
- Proof/Test: OCR proof list。

### 15. OCR typo handling strategy
- Labels: p1, ocr, test
- Acceptance: 错字版本样例可测试降级/修正。
- Non-goals: 不伪造证据。
- Risk: 知识点错误。
- Proof/Test: OCR risk notes。

## P1 Practice / Review

### 16. Complete Adaptive Practice session
- Labels: p1, review-loop, feature
- Acceptance: 错题、薄弱点、需要多练模式可演示。
- Non-goals: 不重写 LearningStore。
- Risk: 编译和 UX 半成品。
- Proof/Test: practice smoke。

### 17. Connect need-more-practice search panel
- Labels: p1, review-loop, ui
- Acceptance: 搜索词可复制，外部搜索可打开，不抓取平台。
- Non-goals: 不后台爬取。
- Risk: 平台内容不可控。
- Proof/Test: Practice Search proof。

### 18. Practice result enters StudyReport
- Labels: p1, export, review-loop
- Acceptance: 报告显示练习结果和需要多练摘要。
- Non-goals: 不导出调试原始内容。
- Risk: 报告过长。
- Proof/Test: report acceptance。

## P1 Course Library

### 19. Verify course search/filter/sort on device
- Labels: p1, ui, test
- Acceptance: 标题、知识点、provider 搜索可用。
- Non-goals: 不改持久化结构。
- Risk: 聚合误导。
- Proof/Test: course library smoke。

### 20. Course detail proof polish
- Labels: p1, ui, proof
- Acceptance: 课程详情入口清晰，不堆叠。
- Non-goals: 不大改视觉。
- Risk: 评委不易理解。
- Proof/Test: Course Detail screenshot。

## P2 UI polish

### 21. Fix small-screen text overflow
- Labels: p2, ui
- Acceptance: 主要按钮和 chip 不竖排。
- Non-goals: 不做大视觉重构。
- Risk: 云真机截图差。
- Proof/Test: small screen smoke。

### 22. Focus theme final pass
- Labels: p2, ui
- Acceptance: 首页、课程库、导入、导出视觉一致。
- Non-goals: 不追求重 blur。
- Risk: 观感不成熟。
- Proof/Test: screenshot set。

### 23. Flow mode proof page
- Labels: p2, ui, proof
- Acceptance: Live/ASR 实验相关 Flow 视觉可展示。
- Non-goals: 不播放真实白噪音。
- Risk: 被误解为音频能力。
- Proof/Test: Flow screenshot。

## P2 i18n

### 24. Finish Chinese copy audit
- Labels: p2, ui, docs
- Acceptance: 用户可见英文只保留技术短码。
- Non-goals: 不翻译 BlueLM/OCR/ASR/PDF。
- Risk: 答辩观感。
- Proof/Test: text audit。

### 25. English mode smoke
- Labels: p2, test
- Acceptance: 如果开启英文，不出现明显乱码和按钮溢出。
- Non-goals: 不追求完整商业文案。
- Risk: i18n 回归。
- Proof/Test: Strings test。

## P2 Documentation

### 26. Promote README draft to root README
- Labels: p2, docs
- Acceptance: 根 README 更新但不含密钥。
- Non-goals: 不写真实配置。
- Risk: 文档过度承诺。
- Proof/Test: README review。

### 27. Build final proof pack and zip
- Labels: p2, proof, competition
- Acceptance: proof pack 通过 checker。
- Non-goals: 不复制 APK 和本地配置。
- Risk: 包内材料缺失。
- Proof/Test: proof checker。

## P3 云同步 / 团队协作

### 28. Research cloud sync architecture
- Labels: p3, feature
- Acceptance: 输出隐私、账号、成本和合规方案。
- Non-goals: 复赛不实现。
- Risk: 服务器成本和隐私。
- Proof/Test: architecture doc。

## P3 声纹 / 底噪研究

### 29. Research speaker diarization options
- Labels: p3, asr
- Acceptance: 区分手动标签、provider 分段、声纹识别。
- Non-goals: 不声称已完成声纹身份识别。
- Risk: 隐私敏感。
- Proof/Test: research note。

### 30. Research audio denoise strategy
- Labels: p3, asr
- Acceptance: 明确依赖系统/provider 或后续服务能力。
- Non-goals: 不自研底层音频算法。
- Risk: 真机环境噪声。
- Proof/Test: risk register。

