# Stage 7 Demo Narration Short

大家好，这是 ClassMate，一款面向课堂学习的 AI 助手。它不是普通录音总结工具，而是把课堂资料变成可复习、可验证、可导出的学习闭环。

复赛版的主路径使用官方 BlueLM/qwen3.5-plus。用户可以导入课堂文本、Markdown、手动 OCR 课件或板书资料，也可以导入 SRT/VTT/TXT 转写稿。Live 页面支持手动课堂和系统 ASR 实验模式；这里要诚实说明，实验模式依赖设备系统识别，不保存原始音频，真实 vivo ASR/OCR provider 还在后续接入计划中。

完成导入后，ClassMate 会生成 Knowledge Timeline。每个知识点都带有证据引用和来源标记，例如课堂文本、字幕、课件 OCR 或板书 OCR。我们不希望模型只给一个漂亮摘要，而是要让学生和评委都能追溯“这个知识点来自哪里”。

接着可以进入 Ask This Lesson。它不是开放式闲聊，而是基于本节课资料回答。如果证据充分就是 grounded，只有部分依据就是 partial，没有依据就应该 not_found，避免模型胡编。

学习闭环还包括 Quiz、Review 和“需要多练”。答错、太难、需要多练会影响复习任务，并提供外部练习搜索词；ClassMate 只打开搜索结果，不抓取第三方平台内容。

最后是 Export Center。学习报告可以导出 PDF、HTML、Markdown、TXT、MindMap 等格式，支持保存到文件、下载目录兜底和系统分享。导出内容不包含本地凭据、内部模型交互或调试原始内容。

ClassMate 的定位是：补齐录音转写类产品的输入地基，但优势放在证据链、微测、复习和练习闭环。决赛版会继续接入真实 vivo ASR/OCR provider，完善多来源证据融合和学习画像。

