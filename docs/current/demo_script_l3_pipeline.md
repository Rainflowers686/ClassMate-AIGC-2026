# L3 Pipeline Demo Script

Date: 2026-06-20

Target length: 2-3 minutes.

## Script

1. Open ClassMate Home.
2. Tap `整理一份新资料`.
3. Open `L3 演示包` or paste a short classroom material.
4. In the material tray, confirm the text and optional question bank.
5. Tap `生成 L3 本地学习闭环`.
6. On the course page, show:
   - summary
   - knowledge map
   - L3 学习闭环 card
   - provider seam chips for OCR / Query Rewrite / Embedding / Text Similarity
   - L3 能力诊断
7. Tap `专项练习`.
8. Show that only stem/options are visible before submit.
9. Choose a wrong option and tap `提交答案`.
10. Show:
    - user answer
    - correct answer
    - explanation
    - source evidence
11. Tap `完成练习`, then open `复习计划`.
12. Show:
    - wrong book count
    - wrong question detail
    - review queue item
    - weak mastery state
13. Optional: return to the course page and tap `模拟考试` to show ExamSession v1 scoring.
14. Optional: tap `随机小测` to show question sampling from the same generated/imported pool.

## Honest Notes

- OCR / Query Rewrite / Text Similarity / Embedding have passed provider smoke, but this demo does not run network smoke.
- ASR Long is not presented as completed. Recording produces an app-private audio artifact record; manual transcript fallback remains available.
- Word/Excel import is seam-only; use Markdown/CSV text for this demo.
- `回忆复盘 / 自评复习` is separate from `专项练习`; do not demo self-report buttons as real practice.
- DOCX/XLSX/PPTX are best-effort in v1.2. PDF remains artifact/manual fallback unless OCR text is supplied.
