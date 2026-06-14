# Stage 7 Full Regression Plan

目标：覆盖复赛演示主链路。若 Adaptive Practice 仍在开发中，只记录“待测”，不要强行当成已完成能力。

## 1. 环境确认

- 操作：运行 Stage 7 QA 脚本，查看 GitHub Actions 最新状态。
- 预期：secrets scan 通过，APK 存在，关键文档齐全。
- 失败排查：先看工作区是否有半成品生产改动。
- 截图建议：状态快照 summary、Actions 页面。

## 2. 安装 APK

- 操作：用 adb 或云真机上传安装 debug APK。
- 预期：App 正常启动。
- 失败排查：确认 APK 时间、设备兼容性、安装权限。
- 截图建议：首页。

## 3. Settings / Provider

- 操作：进入 Settings，查看模型配置和能力路线图。
- 预期：Official BlueLM、Compatible Demo、LocalFallback 分层清楚；路线图分已支持、实验、待接入、暂缓。
- 失败排查：检查配置是否导入、文案是否误导。
- 截图建议：Provider 卡、Roadmap 卡。

## 4. BlueLM Diagnostic

- 操作：运行 BlueLM connection test。
- 预期：状态 OK 或给出短错误；不显示密钥和完整模型交互。
- 失败排查：检查网络、模型权限、配置是否导入。
- 截图建议：诊断结果。

## 5. Markdown 导入

- 操作：导入 Markdown 测试样例。
- 预期：文本清洗后进入资料篮，可以分析。
- 失败排查：检查编码、BOM、表格或代码块处理。
- 截图建议：资料篮文本。

## 6. OCR

- 操作：粘贴课件 OCR、板书 OCR、PDF/讲义 OCR 测试文本。
- 预期：多条 OCR 资料出现在资料篮，并带来源类型。
- 失败排查：空文本不能加入；检查 source marker。
- 截图建议：OCR 资料列表。

## 7. Transcript / SRT / VTT

- 操作：导入 SRT/VTT/TXT 转写稿。
- 预期：段落、时间戳和说话人标签进入转写编辑器，可保存到资料篮。
- 失败排查：检查时间戳格式和空段落。
- 截图建议：Transcript Editor。

## 8. Live ASR 实验

- 操作：进入 Live，尝试手动片段和系统 ASR 实验模式。
- 预期：清楚显示实验边界；不保存原始音频；不后台录音。
- 失败排查：设备是否支持系统语音识别、麦克风权限是否授权。
- 截图建议：Live ASR 提示。

## 9. Timeline / Evidence

- 操作：生成知识时间线，打开知识点和证据。
- 预期：知识点结构清楚，证据能定位到本节课资料，来源 marker 可见。
- 失败排查：检查模型输出、JSON 解析、证据定位。
- 截图建议：Timeline、Evidence Detail。

## 10. Ask This Lesson

- 操作：分别问 grounded、partial、not_found 三类问题。
- 预期：有证据问题返回 grounded；部分依据返回 partial；无依据返回 not_found。
- 失败排查：检查问题是否超出课堂、证据 quote 是否可定位。
- 截图建议：三类 Ask 结果。

## 11. Quiz

- 操作：进入微测，答对一题和答错一题。
- 预期：显示解释和证据，反馈进入学习状态。
- 失败排查：检查题目是否绑定知识点和证据。
- 截图建议：题目、答案解释。

## 12. Review

- 操作：进入复习页，查看今日任务和薄弱点。
- 预期：任务有课程、原因、优先级和状态。
- 失败排查：检查 LearningStore 是否有任务。
- 截图建议：Review 任务列表。

## 13. Need More Practice / Practice Search

- 操作：标记需要多练或进入练习搜索。
- 预期：显示搜索词和外部搜索入口；不抓取平台内容。
- 失败排查：检查 Review/Practice 半成品是否已编译通过。
- 截图建议：练习搜索面板。

## 14. Adaptive Practice

- 操作：如果 Stage 7C 已完成，进入 Adaptive Practice；否则记录待测。
- 预期：错题、薄弱点、需要多练模式可解释。
- 失败排查：如果当前构建失败，先修 Review/Practice 未完成引用。
- 截图建议：Practice 首页或待测备注。

## 15. Course Library

- 操作：进入课程库，测试搜索、筛选、排序，点击课程详情。
- 预期：能按标题、知识点、科目和 provider 标签筛选；空态有清除筛选。
- 失败排查：确认至少有两门课程和复习任务。
- 截图建议：课程库搜索、筛选、课程详情。

## 16. Export Center

- 操作：从课程详情打开 Export Center，导出 PDF、Markdown、HTML、MindMap。
- 预期：可保存到文件、下载目录或系统分享。
- 失败排查：检查 SAF、FileProvider、文件名 sanitize。
- 截图建议：格式列表、保存成功。

## 17. StudyReport 打印

- 操作：打开 PDF/HTML 报告，检查打印级内容。
- 预期：包含课程概要、知识点、证据链、微测、复习计划、需要多练、资料来源和隐私说明。
- 失败排查：检查报告 renderer。
- 截图建议：报告首页、证据页。

## 18. Security Proof

- 操作：检查导出文件、日志、截图和 Git 追踪。
- 预期：不含密钥、完整鉴权内容、内部模型交互或模型内部推理。
- 失败排查：运行 secrets scan 和 sensitive text audit。
- 截图建议：secrets scan 通过、git ls-files 无输出。

## 19. 失败记录模板

```text
模块：
设备/系统：
前置数据：
操作步骤：
预期：
实际：
截图/录屏：
日志短标签：
是否影响复赛演示：
建议处理：
```

