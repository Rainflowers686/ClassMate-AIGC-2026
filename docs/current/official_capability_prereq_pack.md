> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Official Capability Prereq Pack

Date: 2026-06-21

This file is now a current ClassMate prereq pointer, not a raw official-navigation inventory.

## Current Product Matrix

Use these current documents for product decisions:

- `docs/current/official_tool_matrix.md`
- `docs/current/official_tool_productization_matrix.md`
- `docs/current/project_current_status_v1_9.md`

ClassMate product scope contains exactly 18 effective learning-loop capabilities. The Android App should not present unrelated official navigation items as project abilities.

## Raw Official Docs Boundary

The locally captured official docs remain under `.codex_work/official_docs/` and are not product documentation. Do not copy credentials, endpoints, or request/response bodies into current product docs unless they are sanitized and needed for engineering alignment.

## Current Validation Rule

- Official runtime success must be proven by app-level path or cloud-device validation.
- Smoke success alone is not product completion.
- Missing config or runtime failure must fall back without blocking the learning loop.
- Experimental image, video, and bilingual classroom paths stay hidden by default and must not claim generated media unless a real runtime succeeds.
