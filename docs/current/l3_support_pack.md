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
