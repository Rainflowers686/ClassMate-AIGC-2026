# Input Superhub v1.2

Date: 2026-06-20

Current status note: v1.2 is superseded by Championship Upgrade v1.3 for current implementation details. v1.3 adds `ImportReport`, PDF page-level fallback state, manual page text readiness, and visible import diagnostics. See `docs/current/championship_upgrade_v1_3.md`.

## Purpose

Input Superhub unifies classroom text, OCR text, question bank text, office files, audio artifacts, and manual transcript fallback before the L3 pipeline.

## Supported Inputs

| Type | Status | Handling |
| --- | --- | --- |
| Paste text | COMPLETE | Stored as course text and can generate L3 summary/evidence/KP/questions. |
| TXT | COMPLETE | Best-effort text decode. |
| Markdown | COMPLETE | Markdown normalized to plain text. |
| CSV | COMPLETE | Passed to question bank parser when it matches template. |
| DOCX | BEST_EFFORT with quality guard | Reads `word/document.xml` via ZIP/XML and extracts text; empty, suspicious, or too-short output is flagged. |
| XLSX | BEST_EFFORT with quality guard | Reads shared strings and sheet rows; supports simple question bank header and flags template problems. |
| PPTX | BEST_EFFORT with quality guard | Reads slide XML text and flags empty/suspicious output. |
| PDF | PARSER_PENDING | Artifact/page model only; user should paste page text or use OCR seam. |
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

## v1.3 Addendum

- PDF import now creates a page-level fallback object with `PAGE_OCR_SEAM_READY`.
- Manual PDF page text can move a page to `MANUAL_PAGE_TEXT_READY` and feed the same L3 pipeline.
- Import attempts create `ImportReport` records with success count, warning count, failed items, fallback used, and next action.
- This remains honest: native PDF text extraction and real page OCR execution are still future work unless a later validation proves them.

## v1.5 Quality Guard

`ExtractedTextQuality` is evaluated for DOCX/XLSX/PPTX/PDF artifacts:

- empty output -> `EMPTY_FILE`
- suspicious character ratio high -> `TEMPLATE_REQUIRED`
- very short output -> `PARTIAL`
- PDF native text parser -> `PARSER_PENDING`
- readable Office output -> `COMPLETE` / best-effort ready

`ImportReport` includes the quality status and quality message so a demo does not silently treat corrupted Office extraction as a clean success.

## Security

Input Superhub never reads `config.local.json`, never prints secrets, and does not upload files during tests.
