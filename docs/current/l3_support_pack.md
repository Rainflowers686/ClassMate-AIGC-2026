# L3 Support Pack

Date: 2026-06-20

## Import Templates

### Markdown Question Bank

```text
Q: 法拉第定律主要描述什么关系？
A. 感应电动势与磁通量变化率的关系
B. 电阻与温度的关系
C. 电荷量与时间的关系
D. 光强与距离的关系
Answer: A
Explanation: 法拉第定律说明感应电动势大小与磁通量变化率成正比。
```

### CSV Question Bank

```csv
stem,a,b,c,d,answer,explanation
法拉第定律描述什么,磁通量变化率,温度,电阻,光强,A,感应电动势与磁通量变化率相关
```

## Demo Seed Data

The app contains a stable L3 demo seed:

- Lesson: L3 演示课：电磁感应
- Question bank: 3 evidence-bound multiple choice questions
- Purpose: repeatable 2-3 minute app demo without secrets or network smoke

The seed data does not replace real parsing or pipeline logic.

## Import Failure Messages

- Empty file/content: ask the user to paste or select valid text.
- Unsupported format: explain that Word/Excel is seam-only and should be converted to text/CSV first.
- Question format error: ask for Q:/Answer:/Explanation: or CSV headers.
- Provider not configured: show manual fallback; do not claim provider success.
- ASR not configured: allow manual transcript fallback.

## Seam Status

| Capability | Status |
| --- | --- |
| Translation | seam only; future multilingual material aid |
| TTS | seam only; future listen-review, no voice clone |
| Function Calling | local orchestrator step-log skeleton |
| ASR Long | seam only until official config and task flow are wired |
| On-device fallback | present for local suggestions/diagnostics; L3 uses explicit local pipeline fallback |

## Function Closure v1.1 Support Notes

- Practice default: COMPLETE. "专项练习" now requires answer selection and submit before showing answer/explanation/evidence.
- Self-assessment: COMPLETE as separate "回忆复盘 / 自评复习" path. It keeps the old self-report buttons but is not the practice default.
- ExamSession: PARTIAL. It starts, records answers, submits, scores, and writes wrong answers back; advanced timer/sections are TASK_3_FUTURE.
- Wrong book reachability: COMPLETE. Review shows recent wrong answers with user answer, correct answer, explanation, and evidence.
- Word/Excel bank import: SEAM_ONLY / PARSER_PENDING. The UI points users to Markdown/CSV templates until a native parser is added.
- Manual transcript fallback: COMPLETE. It is marked MANUAL_TRANSCRIPT_FALLBACK and enters the same evidence pipeline without claiming official ASR success.

## v1.2 Additions

- Input Superhub: COMPLETE/PARTIAL. TXT/MD/CSV are real; DOCX/XLSX/PPTX are BEST_EFFORT; PDF is PARSER_PENDING; audio/image are artifact/seam paths.
- Knowledge graph: PARTIAL. Related/example edges are generated and visible as a lightweight knowledge map.
- Similar question recommendation: SEAM_ONLY / LOCAL_FALLBACK. It creates recommendation records but not a full recommendation product page.
- NextReviewPolicy: COMPLETE rule seam. WEAK/LEARNING due today; REVIEWING tomorrow; MASTERED after three days.
- Diagnostics matrix: COMPLETE. Shows capability/status labels only; no key, auth, or endpoint values.
