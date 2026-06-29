# ClassMate Architecture

当前架构入口已更新到 `1.14.2 / versionCode 115`。详细内容见：

- [docs/current/ARCHITECTURE_1_14_2.md](docs/current/ARCHITECTURE_1_14_2.md)
- [docs/current/LEARNING_LOOP_ARCHITECTURE.md](docs/current/LEARNING_LOOP_ARCHITECTURE.md)
- [docs/current/FALLBACK_ARCHITECTURE.md](docs/current/FALLBACK_ARCHITECTURE.md)

## 当前架构摘要

ClassMate 由 Android App 层与纯 Kotlin core 层组成：

```text
UI -> AppViewModel -> core analysis/provider/review/export -> Evidence/Practice/Review/Export
```

核心主线：

```text
资料输入 -> 知识结构 -> 证据绑定 -> 微测 -> 反馈 -> 复习闭环 -> AI 精修导出
```

官方能力与 fallback：

- BlueLM cloud -> on-device 3B -> local rule。
- Official ASR/TTS -> system ASR/TTS -> manual transcript/script。
- Official OCR -> manual image text。
- Model quiz -> repaired answerable quiz -> local basic quiz。

旧版架构细节保留在 git 历史和 `docs/architecture/`，但当前发布/答辩以 `docs/current/*1_14_2*` 为准。
