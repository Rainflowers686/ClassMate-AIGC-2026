# Stage 5 明天早上测试顺序

目标：先确认仓库和 APK 健康，再做真机主链路 smoke，最后决定修 bug 还是继续推进真实 ASR/OCR provider。

## 1. 先看 GitHub Actions

- 打开仓库 Actions 页面。
- 确认最新 push 已触发 CI。
- 优先看 Android CI 和 secrets scan。
- 如果失败，先截图失败 workflow、job、关键日志段，再决定是否本地复现。

## 2. 本地运行 preflight

```powershell
.\scripts\qa\stage5_preflight.ps1
```

预期：PASS。若失败，先处理 tracked 私有文件、secrets scan、qwen guard 或危险权限问题。

## 3. 安装 APK

```powershell
.\scripts\qa\stage5_device_helper.ps1 -Info
.\scripts\qa\stage5_device_helper.ps1 -FindAdb
.\scripts\qa\stage5_device_helper.ps1 -Devices
.\scripts\qa\stage5_device_helper.ps1 -Install
.\scripts\qa\stage5_device_helper.ps1 -Launch
```

如果 adb 不通，改用云真机手动上传 APK。

## 4. BlueLM diagnostic

- 设置页导入本机临时模型配置。
- 运行 BlueLM 连接测试。
- proof 截图只拍 masked 状态和 OK 结果，不拍真实凭据。

## 5. 文本样例分析

- 进入首页。
- 导入课堂资料。
- 使用示例课堂或 `docs/testing/stage5_copy_paste_pack.md` 中的高数文本。
- 生成知识时间线。
- 检查 Timeline、Evidence、Quiz 是否生成。

## 6. OCR 分析

- 在资料篮加入 PPT OCR 文本。
- 再加入板书 OCR 文本。
- 生成分析。
- 检查 Evidence 是否显示课件 OCR / 板书 OCR 来源。

## 7. SRT / VTT 转写稿分析

- 粘贴 C++ VTT 或离散数学 SRT。
- 确认时间戳保留。
- 生成 Timeline。
- 检查 Evidence 中是否能看到字幕上下文。

## 8. Transcript Editor

- 进入转写编辑相关入口。
- 修改 speaker。
- 修改文本。
- 合并片段。
- 删除片段。
- 新增片段。
- 确认不申请录音权限，不声称真实 ASR 已完成。

## 9. Ask / Quiz / Review

- Ask：测试 grounded、partial、not_found 三类问题。
- Quiz：答对和答错各一次，检查解释与证据。
- Review：确认今日任务、薄弱点、即将复习、已移除/已掌握区域。

## 10. Export Center

- 从 Course Detail 打开 Export Center。
- 导出 PDF、Markdown、MindMap、Word 兼容 HTML、Slides HTML。
- 使用“保存到文件”选择 Downloads 或云真机可见目录。
- 使用系统分享面板做一次分享 proof。
- 打开文件搜索敏感类别，确认无泄漏。

## 11. 记录失败项

每个失败项记录：

- 页面。
- 操作。
- 预期。
- 实际。
- 截图或录屏。
- 是否阻塞复赛 proof。
- 初步归因：UI、导入、Provider、验证器、导出、设备环境。

## 12. 决策

- 如果 P0 主链路失败，先修 bug。
- 如果主链路通过但 proof 不完整，优先补截图和演示视频。
- 如果主链路和 proof 都通过，再继续真实 ASR/OCR provider 调研或实现。
- 不做第三方平台抓取。
- 不声称真实 ASR/OCR、声纹身份识别或底噪增强已经完成。

## 13. 导出报告可打印性检查（Stage 6）

对 PDF / HTML / Markdown / TXT 各导出一次，逐项确认这是“可直接打印学习的整理稿”，不是 UI dump：

- 封面：课程标题、生成时间、模型路径（仅安全短标签）、资料来源摘要。
- 必须出现的章节标题：
  - 核心知识点
  - 微测题
  - 需要多练清单
  - 复习计划
  - 资料来源摘要
- “需要多练清单”中每项含：为什么需要多练、建议练习方向、推荐搜索关键词、搜索入口（文本）。
- 必须使用“需要多练”，不得出现“需要例题”。
- HTML 用浏览器打开应有 h1/h2 标题层级，适合 A4 打印；PDF 能打开且非空（中文受 Android 字体限制时以 HTML 为最稳妥打印格式）。
- 禁止出现以下任意字样（出现即判失败）：
  - Authorization、Bearer、appKey、apiKey、app_id
  - prompt、messages、reasoning_content、vendor body
  - debug 原始字段、JSON dump、evidence_segment_id、weak_point
- Markdown 复制到笔记软件应结构清晰；TXT 无乱码、无 `#`/`**` 残留标记。

### Markdown 导入抽查

- 导入一个 UTF-8 `.md`：标题/列表/表格/代码/公式/图片/链接应被规范化为干净正文，不出现 `#`、`|---|`、裸 `![]()`。
- 导入一个 GBK/GB18030 中文 `.md` 或 `.txt`：正文不应乱码。
- 导入超大文件（>2MB）应给出“文件较大”的友好提示而非崩溃。

