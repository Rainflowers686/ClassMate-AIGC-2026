> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# ClassMate Test Assets Manifest v1

This manifest lists small, non-secret materials for cloud real-device validation. Do not commit binary photos, recordings, APKs, AABs, AARs, private class notes, or credentials.

## 1. Classroom Text

Use this text for paste/Markdown validation:

```text
法拉第电磁感应定律说明：穿过闭合回路的磁通量发生变化时，回路中会产生感应电动势。
楞次定律说明：感应电流的方向总是阻碍引起它的磁通量变化。
如果磁场增强，感应电流会产生反向磁场；如果磁场减弱，感应电流会产生同向磁场。
复习时要区分“磁通量变化”是原因，“感应电动势/感应电流”是结果。
常见错误是只记公式 E=-dΦ/dt，却没有说明负号代表楞次定律的阻碍方向。
```

Expected generated topics:

- 磁通量变化
- 感应电动势
- 楞次定律方向
- 公式负号含义

## 2. Markdown Document

Save as `classmate_lesson_sample.md` on the test machine:

```markdown
# 电磁感应课堂笔记

## 核心概念

- 磁通量变化会产生感应电动势。
- 楞次定律用于判断感应电流方向。
- 负号表示感应效果阻碍原来的变化。

## 易错点

不要把“磁场存在”直接等同于“产生感应电流”；关键条件是磁通量发生变化。
```

## 3. TXT Document

Save as `classmate_lesson_sample.txt`:

```text
细胞呼吸分为有氧呼吸和无氧呼吸。有氧呼吸在线粒体中逐步释放能量，产生二氧化碳和水。无氧呼吸在氧气不足时进行，能量释放较少。复习时要比较反应场所、产物和能量释放差异。
```

## 4. Lesson Image

Create a simple photo or screenshot:

- White paper or slide with 3 lines of large text.
- Suggested text: `磁通量变化 -> 感应电动势 -> 楞次定律判断方向`.
- Avoid private classroom content.
- Store locally on the cloud device or use the camera to capture it.

Expected App behavior:

- image/photo entry creates OCR draft or allows manual recognized text.
- evidence asset label is visible.
- EvidenceDetail attempts thumbnail/original preview.

## 5. PDF Page Text Sample

Binary PDF is not committed. For PDF validation:

1. Put any harmless one-page PDF on the cloud device, for example exported from the Markdown sample above.
2. Import it through the App.
3. Use the PDF page text entry with this manual page text:

```text
PDF 第 1 页：电磁感应复习。磁通量变化是产生感应电动势的条件，楞次定律用于判断方向。负号表示阻碍变化。
```

Expected App behavior:

- PDF shows parser pending/manual page text path.
- page text creates DOCUMENT evidence with page hint.

## 6. Recording / Manual Transcript

Use a 10 to 20 second recording or paste this transcript:

```text
00:00 今天复习电磁感应。磁通量发生变化时，闭合回路中会产生感应电动势。
00:20 第二点是楞次定律。感应电流方向总是阻碍原来的磁通量变化。
00:40 第三点是常见错误，不能只背公式，还要解释负号代表阻碍方向。
```

Expected App behavior:

- recording artifact or ASR job card is visible.
- transcript confirmation enters the L3 learning loop.
- EvidenceDetail for AUDIO shows file/time/transcript.

## 7. Deliberate Wrong Answer Path

For any generated single-choice question:

1. Choose an option that is not marked as correct after submission.
2. Submit.
3. Open Review.
4. Verify WrongBook detail and retry.

Expected:

- WrongBook stores user answer, correct answer, mistake reason, remediation hint, knowledge point, and evidence.
- Retry changes mastery/review queue/retryCount.

## Asset Safety Rules

- Do not use real student names, school IDs, phone numbers, or private exams.
- Do not commit binary assets unless explicitly approved.
- Do not include `config.local.json`, keys, auth headers, endpoints, APKs, AABs, AARs, fonts, or model files.
