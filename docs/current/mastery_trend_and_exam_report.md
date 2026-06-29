> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Mastery Trend and Exam Report

Date: 2026-06-20

## Mastery Trend

Status: `COMPLETE` local rule-based trend.

- `MasteryHistoryEvent` records answer correct/wrong and state transitions.
- `MasteryTrendStats` aggregates daily correct/wrong counts, weak/mastered trends, streak, lapse count, and a seven-day summary.
- Review and course detail can surface trend status.

## Exam Report

Status: `COMPLETE` local report.

- `ExamResultReport` includes score, accuracy, duration, question breakdown, knowledge point breakdown, weak knowledge points, wrong questions, evidence coverage, review recommendations, and Markdown report text.
- Wrong answers still enter wrong book, review queue, and mastery updates through the existing practice loop.

## Future

Task 5 can add richer trend charts, true spaced-repetition modeling, and file export for exam reports.
