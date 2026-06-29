> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# ClassMate Real Device Regression Checklist v1

Use this checklist after installing the debug build on a real device. The app version is read from `BuildConfig`; verify the Settings/About/Diagnostics version matches `app/build.gradle.kts`.

## Main Route

1. Home -> Import material -> Course detail -> Evidence detail -> Practice -> Wrong book -> Review -> Export.
2. Every step must continue without crashes, stale data, raw ids, provider traces, or false official-capability claims.

## P0/P1 Regression Points

### Multi-image OCR

1. Pick at least three classroom images at once.
2. Confirm the OCR draft shows each image as a separate item in the selected order.
3. Confirm one failed or unreadable image shows a visible error but the other images remain available.
4. Before confirming the draft, verify no knowledge points, quiz, or review plan are published.
5. Confirm the OCR draft. The generated course should contain image-section text and each successful image should have a separate `OCR_IMAGE` evidence entry.
6. Start a second image import and confirm the previous batch is not reused.

### OCR Quality

1. Use an image with question numbers, bullet points, and formula-like lines.
2. Verify question numbers, bullets, and formula symbols are preserved in the editable draft.
3. Use a low-quality or unreadable image. The app should recommend manually checking the OCR result and should not silently publish bad text.

### Import Capability Honesty

1. Text, TXT/Markdown, image OCR, subtitles, transcript paste, recording, and PDF page text must show honest availability.
2. Embedded video subtitles must not be presented as automatic extraction. Use manual subtitles or transcript import instead.
3. Missing permission or missing configuration must show a clear fallback path.

### Evidence Ownership

1. Open evidence from Course detail, Practice, Wrong book, and Review.
2. Strong evidence opens as "View evidence"; weak evidence shows "Check evidence"; missing evidence shows "No traceable evidence" in the active language.
3. Evidence from deleted, sample, or other courses must not be reused as strong evidence.

### Course Deletion

1. Delete a course from the visible delete entry.
2. Confirm the warning mentions related knowledge points, quiz, wrong book, review tasks, drafts, and local records.
3. After deletion, Home, History, Review, Wrong book, and export drafts must not show broken references to the deleted course.
4. Repeating deletion or deleting sample data must not affect other courses.

### Recording And ASR

1. Start recording with live ASR readiness enabled.
2. If SpeechRecognizer is unavailable or permission is missing, the app should show a Chinese friendly reason and manual transcript fallback.
3. Stop recording with transcript text: audio evidence and transcript evidence are both present.
4. Stop recording without transcript text: audio evidence is present, but no fake transcript evidence is created.
5. Cancel recording: no audio evidence and no transcript evidence are created.

### Quiz And Weakness Loop

1. Imported, generated, random, exam, and retry questions must have usable options, answer, explanation, and evidence state.
2. Bad questions must not enter Practice, Wrong book, or export.
3. Answer wrong: Wrong book, weak knowledge point, mastery, and ReviewPlan must point to the same knowledge point.
4. Answer correctly on retry: mastery/review priority should improve without deleting history.

### Export Safety

1. Export PDF, Word-compatible HTML/DOCX, Markdown, TXT, and Study Pack.
2. Empty courses or missing evidence should produce an honest empty-state document, not a crash.
3. Weak evidence is marked "证据待核对"; missing evidence is marked "暂无可回溯证据".
4. Exported content must not contain secrets, `config.local.json`, provider traces, `LOCAL_FALLBACK`, `BuildConfig`, `Semantic index`, `Tool steps`, `ASR Long job`, `PDF page`, `Import report`, `Transcript timeline`, `assetId`, `MIME`, or raw `kp_` / `q_` / `ev_` ids.

### i18n And User Copy

1. Switch Chinese, English, and Follow system.
2. User-facing evidence, recording, import, export, quiz, review, and HelpHint copy should follow the selected language where it has been migrated.
3. Developer codes may appear only in diagnostics, not normal learning pages.

## Known Honest Limits

1. Official OCR/ASR and retrieval capabilities are configuration-gated.
2. Embedded video subtitles are not automatically extracted.
3. Evidence relation is a conservative lexical guard, not full semantic proof.
4. Some legacy screens still contain Chinese-only copy; do not mark that as full i18n completion.
