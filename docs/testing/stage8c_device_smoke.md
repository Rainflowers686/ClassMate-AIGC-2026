# Stage 8C Input-to-Learning 真机 Smoke 路线

本清单用于 Stage 8C 真机验收。当前事实：Stage 8A-2.2 已在 vivo X300 Pro / OriginOS 6 / Android 16 上跑通端侧文本生成与多模态 callVit；Stage 8C 已把文本、图片、拍照入口接入“用户确认后进入课程分析”的安全链路。测试时仍需逐项截图和记录，不要把失败包装成成功。

## 0. 安装 APK

- 操作步骤：安装当前 `app-debug.apk`，启动 ClassMate。
- 预期结果：App 正常打开，无崩溃。
- 失败时记录字段：设备型号、系统版本、APK 时间、安装错误文本。
- 截图命名建议：`stage8c_00_install_ok.png`。

## 1. 权限中心确认

### 模型目录访问

- 操作步骤：在系统权限/文件访问页确认模型目录访问已授权，目标目录为 `/sdcard/1225`。
- 预期结果：App 可读取 tokenizer/config，端侧诊断不报目录缺失。
- 失败时记录字段：权限状态、模型目录是否存在、诊断短错误。
- 截图命名建议：`stage8c_01_model_dir_permission.png`。

### 相机

- 操作步骤：确认相机权限已授权，再进入拍照导入。
- 预期结果：可打开相机或系统拍照入口。
- 失败时记录字段：权限状态、系统拒绝提示、入口页截图。
- 截图命名建议：`stage8c_02_camera_permission.png`。

### 图片/视频/音频

- 操作步骤：确认图片/视频/音频访问权限符合系统要求。
- 预期结果：图片导入和字幕/转写稿导入入口可用；不要求解析第三方平台内容。
- 失败时记录字段：权限状态、文件选择器错误。
- 截图命名建议：`stage8c_03_media_permission.png`。

### 麦克风、蓝牙、通知

- 操作步骤：查看麦克风、蓝牙、通知权限状态。
- 预期结果：Live ASR 或实验功能如需权限，应显示诚实说明；拒绝后 App 不崩溃。
- 失败时记录字段：权限状态、实验模式提示、崩溃日志摘要。
- 截图命名建议：`stage8c_04_runtime_permissions.png`。

## 2. 端侧模型诊断回归

### 文本 init / generate

- 操作步骤：Settings -> 端侧 BlueLM 3B 诊断 -> 文本初始化 -> 短文本生成。
- 预期结果：init 成功；generate 返回短输出；provider path 显示端侧蓝心。
- 失败时记录字段：modelPath、init code、generate error code、latency。
- 截图命名建议：`stage8c_10_ondevice_text_generate_ok.png`。

### 多模态 callVit / generate

- 操作步骤：Settings -> 多模态诊断 -> 选择测试图 -> callVit -> generate。
- 预期结果：callVit 返回 0；generate 成功；不声称已替代 OCR。
- 失败时记录字段：bitmap size、callVit code、generate code、modelPath。
- 截图命名建议：`stage8c_11_ondevice_multimodal_callvit_ok.png`。

## 3. 文本课程分析

### 云端成功路径

- 操作步骤：联网，导入高数或物理测试文本，使用 Official BlueLM 分析。
- 预期结果：CourseAnalysis 成功，结果进入 Timeline / Quiz / Review / History。
- 失败时记录字段：provider path、model、error type、validation 状态。
- 截图命名建议：`stage8c_20_cloud_course_analysis_ok.png`。

### 云端失败后端侧 fallback

- 操作步骤：模拟云端失败或断网，导入短中等长度课程文本。
- 预期结果：云端失败后尝试端侧 BlueLM 3B seam；端侧输出 JSON parse 和 validators 通过后才落库。
- 失败时记录字段：cloud error、ondevice status、parse strategy、validation error。
- 截图命名建议：`stage8c_21_ondevice_course_analysis_fallback_ok.png`。

### 双模型失败安全占位

- 操作步骤：使云端不可用且端侧 unavailable 或输出 invalid JSON。
- 预期结果：不落库；显示安全占位；不把安全占位标为成功分析。
- 失败时记录字段：cloud status、ondevice status、parse/validator 状态、history 是否新增。
- 截图命名建议：`stage8c_22_safety_placeholder_no_persist.png`。

## 4. 图片导入

### 选择课件/板书图片

- 操作步骤：导入课堂资料 -> 图片/课件/板书 -> 选择测试图片。
- 预期结果：进入端侧多模态理解流程，生成可编辑草稿。
- 失败时记录字段：文件类型、图片尺寸、callVit code、draft 状态。
- 截图命名建议：`stage8c_30_image_import_draft.png`。

### 确认后进入课程分析

- 操作步骤：编辑草稿标题/正文，点击确认并生成课程分析。
- 预期结果：草稿文本进入课程分析；成功后 Timeline 有来源标签。
- 失败时记录字段：draft length、provider path、validation 状态。
- 截图命名建议：`stage8c_31_image_draft_confirm_analysis.png`。

### 取消后不落库

- 操作步骤：生成草稿后点击取消或返回。
- 预期结果：History / Course Library 不新增课程记录。
- 失败时记录字段：取消前后 history count、session id。
- 截图命名建议：`stage8c_32_image_draft_cancel_no_persist.png`。

## 5. 拍照导入

### 拍板书/纸质题目

- 操作步骤：拍照导入 -> 拍一张板书或纸质题目。
- 预期结果：照片进入多模态理解，生成可编辑草稿。
- 失败时记录字段：相机权限、照片尺寸、callVit code。
- 截图命名建议：`stage8c_40_camera_draft.png`。

### 确认进入课程分析

- 操作步骤：编辑草稿并确认。
- 预期结果：分析结果进入 Timeline / Quiz / Review。
- 失败时记录字段：draft 内容长度、provider path、validator 状态。
- 截图命名建议：`stage8c_41_camera_confirm_analysis.png`。

## 6. Ask / Quiz / Review / Practice / Export 回归

- 操作步骤：打开刚生成课程，依次测试 Ask、Quiz、Review、Practice、Export。
- 预期结果：Ask 显示来源标签；Quiz 有证据解释；Review/Practice 有下一步建议；Export 可保存和分享。
- 失败时记录字段：页面、课程 id、provider path、是否 fallback、导出格式。
- 截图命名建议：
  - `stage8c_50_ask_source_label.png`
  - `stage8c_51_quiz_evidence.png`
  - `stage8c_52_review_practice_next_step.png`
  - `stage8c_53_export_safe_report.png`

## 7. 安全检查

- 操作步骤：打开导出文件、日志页、截图 proof，搜索密钥、鉴权头、完整模型输入、完整模型响应和内部推理字段类别。
- 预期结果：不出现敏感信息；只显示短标签、状态、长度和 provider path。
- 失败时记录字段：文件名、命中位置、截图是否需要废弃。
- 截图命名建议：`stage8c_60_export_redaction_check.png`。
