# Architecture - ClassMate 1.14.2

## 分层

| 层 | 责任 | 关键模块 |
| --- | --- | --- |
| App UI | 用户入口、状态展示、证据回溯、导出操作 | `ui/screens/*`、`ui/components/*` |
| ViewModel | 学习闭环状态机、导入、录音、Practice/Review、导出触发 | `AppViewModel.kt`、`ClassMateUiState.kt` |
| Core analysis | 课程分析、prompt、验证、fallback、QuizQuality | `core/analysis`、`core/provider`、`core/practice` |
| Provider | 蓝心 HTTP、官方 OCR/ASR/TTS/retrieval seams | `core/provider`、`core/official/ws`、`app/asr` |
| Evidence | EvidenceAsset、EvidenceOwnership、EvidenceRelation | `core/evidence`、`app/data/EvidenceAssetStore.kt` |
| Review loop | WrongBook、Mastery、Weakness、ReviewPlan | `core/review`、`core/weakness`、`app/l3` |
| Export | StudyReport、SafeExportText、PDF/DOCX/HTML/MD/TXT | `core/exporting`、`app/exporting`、`LearningExportEngine.kt` |

## 主数据流

```text
ImportCourseScreen / Transcript / Recording
  -> MaterialSource / EvidenceAsset
  -> AppViewModel.startAnalysis / L3 publish
  -> CourseAnalyzer + provider/fallback
  -> KnowledgePoint + Evidence + Quiz
  -> Practice attempt
  -> WrongBook / Mastery / ReviewPlan
  -> ExportCenter / PolishedStudyPack
```

## 风险控制

- Provider 失败不阻断学习闭环。
- EvidenceOwnership 避免跨课程证据污染。
- QuizQuality / isAnswerableQuiz 避免坏题进入练习。
- SafeExportText 清理密钥、内部状态、provider trace 和 raw id。
- i18n guard 避免普通用户页泄漏开发者文案。
