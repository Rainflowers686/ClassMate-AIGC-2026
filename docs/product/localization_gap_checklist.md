# ClassMate 汉化缺口检查清单

目标：保证默认中文体验完整，技术名词诚实保留，调试区默认折叠。

| 区域 | 应该中文化的典型文案 | 允许保留英文的技术词 |
|---|---|---|
| Home | 首页、今日学习、导入课堂资料、Live 课堂伴学、最近一节课、今日复习摘要、模型状态 | BlueLM、Live、Compatible、LocalFallback |
| Import | 选择资料类型、资料篮、分析设置、粘贴文本、示例课堂、加入本节课资料、生成知识时间线 | OCR、PPT、PDF、TXT、Markdown |
| Material Tray | 已加入资料、来源类型、字数、编辑、删除、调整顺序、资料会进入统一资料包 | MaterialBundle 可在说明中保留 |
| Transcript Editor | 转写片段、说话人、老师、学生、未知、合并、删除、新增、编辑文本 | speaker、ASR 可在技术说明中保留 |
| OCR | 课件 OCR、板书 OCR、讲义 OCR、当前未接入真实 OCR、不上传、不解析 | OCR、vivo OCR |
| Course Detail | 课程详情、课程概览、知识时间线、问这节课、微测、复习计划、资料来源、导出中心 | Timeline、Quiz 可作为辅助英文保留 |
| Timeline | 知识时间线、核心知识点、证据、重要度、难度、打开证据 | Evidence 可在标签中保留 |
| Ask | 问这节课、有证据、部分依据、未找到依据、本地兜底、相关知识点 | Ask This Lesson 可作为功能名保留 |
| Quiz | 微测、题目、选项、正确、错误、解释、证据解释、进入复习 | Quiz 可作为 tab/技术名保留 |
| Review | 复习、今日任务、薄弱点、即将复习、已掌握、已移除、需要多练、证据不对 | ReviewTask、LearningStore 可在技术说明中保留 |
| History | 课程库、课堂记录、最近学习时间、重新打开、删除记录、课程聚合 | History 可作为旧模块名保留 |
| Export Center | 导出中心、保存到文件、系统分享、PDF 报告、Word 兼容 HTML、演示幻灯片 HTML、不是原生格式 | PDF、HTML、Markdown、MindMap |
| Settings | 设置、模型配置、语言、隐私安全、能力路线图、调试区、构建信息 | Debug、BuildInfo、qwen3.5-plus |
| Empty states | 暂无课堂记录、暂无复习任务、请先导入课堂资料、暂无证据、暂无可导出报告 | 无 |
| Toast/Snackbar | 已保存、已取消、保存失败、已加入资料、已记录反馈、连接测试完成 | HTTP 状态码、provider 名可保留 |

## 检查原则

- 用户主路径默认中文。
- Debug、BuildInfo、模型名、文件格式名可以保留英文。
- 未实现能力必须写“占位 / 待接入 / 不解析 / 不上传”。
- 不要把 Word 兼容 HTML 写成真文档格式。
- 不要把演示幻灯片 HTML 写成真演示文稿格式。
- 不要声称真实 ASR、真实 OCR、声纹身份识别或底噪增强已经完成。

