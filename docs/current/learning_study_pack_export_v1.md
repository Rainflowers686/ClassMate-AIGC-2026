# Learning Study Pack Export v1

## 中文摘要（当前真实状态）

Study Pack 来自 L3 学习闭环（非原文堆砌），包含：课程标题与生成时间、来源类型、AI 整理摘要、知识点、易错点、微测（含答案与解析）、错题本（错因与补救）、20 分钟复习计划、学习诊断、证据来源索引、低置信说明、云端 / 端侧 / 本地能力使用简述。导出脱敏：不含密钥、config 内容、请求体、provider 调试串、内部 smoke 文案。标题与栏目名跟随 App 语言，用户原始内容不强制翻译。导出失败有文件保存兜底，不崩溃。

---

Date: 2026-06-21

## Purpose

ClassMate study pack export turns the L3 learning loop into printable and shareable learning material. It is not a raw text copy and not a debug dump.

## Supported Output Paths

| Format | Status | Notes |
| --- | --- | --- |
| PDF | PARTIAL / READY | Uses existing PDF renderer; suitable for A4 print validation. |
| DOCX | PARTIAL / READY | Uses existing DOCX renderer path; no new dependency added. |
| Word-compatible HTML | READY | Can be opened by Word/WPS when DOCX is not desired. |
| Markdown | READY | Structured source for review, sharing, or conversion. |
| HTML | READY | Browser-readable study pack. |
| Text | READY | Copy-friendly fallback. |

## Content Contract

The export includes:

- course title
- generated time
- source types
- AI-organized summary
- knowledge points
- key concepts
- easy mistakes
- micro quiz
- correct answers and explanations
- wrong book
- mistake reason
- remediation hint
- 20-minute review plan
- learning diagnosis
- evidence source index
- low-confidence notes
- short cloud/edge/local capability-use note

## Safety Contract

Exports must not contain:

- keys or app credentials
- `config.local.json`
- Authorization headers
- request bodies or provider debug payloads
- internal smoke wording
- adapter/runtime implementation details

## User Path

Course Detail -> Learning loop generated -> Generate study pack / export learning material -> choose PDF, DOCX, Word-compatible HTML, Markdown, HTML, or Text.

If export fails, the user should be able to retry, switch to HTML/Text, or export the currently recognized content.
