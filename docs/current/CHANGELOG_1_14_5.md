# ClassMate 1.14.5 / versionCode 118

## Summary

This release fixes the latest real-device quality regressions around OCR/text knowledge extraction, quiz relevance, review-plan wording, related-knowledge summaries, ASR fallback entry points, and first-install appearance defaults.

## Changes

- Added a shared subject-knowledge filter for OCR, transcript, document, and manual text paths.
- Prevented classroom prompts such as "同学们注意", "重点来了", "大家记一下", and UI words such as "页面/按钮/点击/上传/下载" from becoming knowledge points.
- Reused accepted evidence-bound knowledge points for the knowledge framework, related knowledge, review plan, quiz generation, and feedback replacement questions.
- Pure noise or very low-quality OCR no longer creates fake knowledge points or fake quizzes; users can manually correct OCR/transcript text and retry.
- Related knowledge remains course-local: it starts from accepted course knowledge points and cites in-course evidence instead of external search results.
- Added a visible speech-setting and transcript-paste fallback path on the focus realtime-ASR surface.
- Fresh installs now default to the "沉浸学习" theme and "端正阅读" typography; saved user preferences are not overwritten.

## Validation Boundary

- Official OCR/ASR/TTS network success still depends on real AppKey, permission state, device ROM, and interface availability.
- System ASR remains an Android system fallback, not a vivo official capability.
- The subject filter is conservative and evidence-bound; difficult OCR should still be manually reviewed.
