# ClassMate Effective Official Capability Matrix

## 中文摘要（当前真实状态）

本项目共 18 项有效官方能力，单一事实来源为代码 `VivoOfficialProviderRegistry` 与 `CapabilityReadinessRegistry`。仅大模型与通用 OCR 标为「已用于学习闭环」；文本翻译 / 文本向量 / 文本相似度 / 查询改写 / 音频生成为本地兜底；Function calling / 实时短语音 / 长语音听写 / 方言自由说 / 端侧文本审核为接缝就绪；长语音转写与端侧 3B 对齐主链待真机抽测；端侧能力文件需配置后可用；图片生成、视频生成、同声传译为实验性默认关闭。声音复刻与地理编码已排除。逐项 L3-readiness 见 `official_18_capability_l3_readiness.md`。

---

Date: 2026-06-21

This is the current ClassMate product matrix. It is intentionally limited to capabilities that can support the Android App learning loop.

| # | Capability | Category | Priority | Implementation status | Learning-loop surface | Cloud model role | Edge model role | Fallback strategy | User-visible learning value |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Large model | reasoning | CORE | USED_IN_LEARNING_LOOP | summary / knowledge / quiz / review | organize classroom material | fallback explanation and review advice | local L3 rules | class highlights become learnable assets |
| 2 | Function calling | orchestration | CORE | FALLBACK_ONLY / SEAM_READY | diagnosis / tool plan | structured tool selection when configured | local orchestrator mirror | local orchestrator | explains why each tool was used |
| 3 | Image generation | visual study | EXPERIMENTAL | CONFIG_REQUIRED | visual study asset | generate study diagram when configured | prompt generation | prompt only | study diagram prompt is ready |
| 4 | Video generation | review media | EXPERIMENTAL | CONFIG_REQUIRED | review video plan | generate review video when configured | storyboard generation | storyboard only | review short-video script is ready |
| 5 | General OCR | vision | CORE | USED_IN_LEARNING_LOOP / CONFIG_REQUIRED | OCR image evidence | extract image text | manual confirmation | OCR text fallback | image text becomes evidence-backed learning |
| 6 | Text translation | language | ENHANCEMENT | CONFIG_REQUIRED | bilingual evidence | translate bilingual materials | original-language fallback | learn from original text | English/bilingual material can be reviewed |
| 7 | Text embedding | retrieval | CORE | FALLBACK_ONLY / READY | semantic index | official vectors when validated | local lexical vector | lexical vector | evidence and questions stay searchable |
| 8 | Text similarity | retrieval | CORE | FALLBACK_ONLY / READY | similar knowledge / wrong book | official ranking when validated | local token similarity | token similarity | similar wrong questions are linked |
| 9 | Query rewrite | retrieval | CORE | FALLBACK_ONLY / READY | Ask / review query | normalize study queries | local query planning | direct/local query | questions retrieve steadier evidence |
| 10 | Realtime short ASR | speech | CORE | SEAM_READY | short audio evidence | short ASR when configured | manual transcript | manual transcript | short speech can enter the same loop |
| 11 | Long audio dictation | speech | CORE | CONFIG_REQUIRED / FALLBACK_ONLY | timeline / evidence | long dictation when validated | manual transcript and post-process | manual transcript | recordings become segmented review material |
| 12 | Long audio transcription | speech | CORE | CONFIG_REQUIRED / FALLBACK_ONLY | timeline / quiz / review | long transcription when validated | chunk state and manual fallback | manual transcript | transcript drives knowledge and review |
| 13 | Dialect free speech | speech | CORE | FALLBACK_ONLY / SEAM_READY | audio timeline | dialect mode when exposed | conservative correction | low-confidence markers | accent/dialect segments are flagged |
| 14 | Simultaneous interpretation | bilingual speech | EXPERIMENTAL | CONFIG_REQUIRED / SEAM_READY | bilingual transcript | live interpretation when configured | transcript plus translation draft | bilingual draft only | bilingual evidence can be retained |
| 15 | Audio generation | review audio | ENHANCEMENT | CONFIG_REQUIRED | audio review | generated review audio when configured | read-aloud script | script only | listen-review script is ready |
| 16 | Edge 3B large model | on-device | CORE | FALLBACK_ONLY | offline study | cloud backup route | offline summary/quiz/review | local rules | weak-network learning continues |
| 17 | Edge text audit | on-device safety | CORE | USED_IN_LEARNING_LOOP / FALLBACK_ONLY | safety guard | check generated content | on-device/local safety | local safety rules | generated study assets are guarded |
| 18 | Edge capability files | device readiness | CORE | USED_IN_LEARNING_LOOP / FALLBACK_ONLY | device readiness | cloud path remains available | resource readiness | presence-only checks | users know if edge fallback is ready |

Experimental entries are hidden by default and appear only after the user enables them in Settings.
