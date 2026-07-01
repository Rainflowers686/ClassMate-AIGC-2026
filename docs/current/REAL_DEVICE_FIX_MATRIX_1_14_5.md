# Real Device Fix Matrix - 1.14.5 / 118

| Issue from 问题2.docx | Current fix | Verification | Still needs real-device check |
| --- | --- | --- | --- |
| ASR still says no speech service | Recording remains available; realtime ASR shows system-setting and transcript-paste fallback on import/focus paths | ASR readiness tests and manual focus/import page check | ROM-specific settings screens may vary |
| OCR extracts irrelevant emphasis words | `SubjectKnowledgeExtractor` filters classroom prompts, UI noise, dates/pages, and filler before L3 knowledge generation | `SubjectKnowledgeExtractorTest`, `L3LearningPipelineTest.noisyOcrBuildsSubjectKnowledgeNotClassroomPrompts` | Real OCR accuracy still depends on image quality and official/system OCR availability |
| No system speech recognition service | App does not create fake transcript; user can record only or paste/import transcript | ASR state tests plus real-device no-recognizer scenario | Whether ACTION_VOICE_INPUT_SETTINGS opens directly depends on ROM |
| Review plan summarizes "同学们注意" | Review queue now follows accepted subject knowledge points only | L3 pipeline regression test checks review text has no prompt noise | Need visual confirmation on real course data |
| Course related knowledge inaccurate | Related knowledge starts from accepted knowledge points and cites current-course evidence only | `LearningLoopRefinementEnginesTest.relatedKnowledgeIgnoresClassroomPromptNoise` | Borderline low-evidence topics remain marked for review |
| Quiz not tied to knowledge | Local/model fallback questions are filtered by accepted knowledge and evidence binding | L3 quiz tests check knowledge/evidence binding and no prompt noise | Real BlueLM outputs still need live validation |
| First install theme/font defaults | New installs default to Focus Immersion + Academic typography; saved preferences persist | Theme repository and theme option tests | Existing users keep their previous saved settings |

## Claim Boundary

- Course related knowledge is in-course retrieval and summarization, not an external API.
- Local fallback is not labeled as BlueLM.
- System ASR/TTS are Android system capabilities, not vivo official ASR/TTS.
