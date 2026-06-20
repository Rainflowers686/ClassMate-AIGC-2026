# Input Superhub v1.2

Date: 2026-06-20

## Purpose

Input Superhub unifies classroom text, OCR text, question bank text, office files, audio artifacts, and manual transcript fallback before the L3 pipeline.

## Supported Inputs

| Type | Status | Handling |
| --- | --- | --- |
| Paste text | COMPLETE | Stored as course text and can generate L3 summary/evidence/KP/questions. |
| TXT | COMPLETE | Best-effort text decode. |
| Markdown | COMPLETE | Markdown normalized to plain text. |
| CSV | COMPLETE | Passed to question bank parser when it matches template. |
| DOCX | BEST_EFFORT | Reads `word/document.xml` via ZIP/XML and extracts text. |
| XLSX | BEST_EFFORT | Reads shared strings and sheet rows; supports simple question bank header. |
| PPTX | BEST_EFFORT | Reads slide XML text. |
| PDF | PARSER_PENDING | Artifact only; user should paste OCR/text fallback. |
| Image/photo | OCR_READY_SEAM | OCR provider is config-gated; manual OCR text can enter evidence. |
| Audio/recording | PARTIAL | Artifact plus ASR Long job seam; manual transcript fallback is the completed path. |
| Video | ARTIFACT_ONLY | No platform scraping, no video parsing in this stage. |

## Failure States

- EMPTY_FILE
- READ_FAILED
- UNSUPPORTED_FORMAT
- FORMAT_ERROR
- PARSER_PENDING
- TEMPLATE_REQUIRED
- ASR_NOT_CONFIGURED
- OCR_READY_SEAM

These states are shown as product status, not hidden as silent failures.

## Security

Input Superhub never reads `config.local.json`, never prints secrets, and does not upload files during tests.
