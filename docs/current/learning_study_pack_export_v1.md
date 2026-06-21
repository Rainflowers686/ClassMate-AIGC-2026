# Learning Study Pack Export v1

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
