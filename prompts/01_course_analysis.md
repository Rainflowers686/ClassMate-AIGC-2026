# 01 — Course analysis (single-shot)

Task: from the input course title, hotword list, and segmented text, produce a
`CourseAnalysisResult` JSON: a knowledge-graph timeline, 3-5 quiz questions,
and a session-scoped review plan.

## User-turn template

```text
Course title: {{course_title}}
Hotwords: {{hotword_csv}}
Segments:
- segment_id={{seg.segment_id}} time_range={{seg.time_range}}
  text: {{seg.text}}
...

Return JSON matching CourseAnalysisResult. No Markdown. No commentary.
```

## Notes

- Single-call strategy is preferred for v0.3 (lower coordination cost than
  multi-step). If latency is bad, split into `01a_segment_to_kps.md` +
  `01b_kps_to_quiz_and_plan.md` later.
- The schema is enforced by `core/evidence/EvidenceValidator.kt` AFTER receipt.
  Even a model that lies about `source_segment_id` is caught locally.
