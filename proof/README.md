# proof/

Evidence for the competition: screenshots, short videos, **redacted** logs and audit notes.

| Folder | What goes here |
|---|---|
| `screenshots/` | Clear, high-res captures of each key screen (Home, Knowledge timeline, Evidence detail, Quiz, Review plan, Settings). |
| `videos/` | Short screen recordings of the end-to-end flow. |
| `logs_redacted/` | Logs that contain ONLY whitelisted fields (provider / status / latency / validation / fallback_used / error_type). See [`sample_analysis_redacted.log`](logs_redacted/sample_analysis_redacted.log). |
| `audit/` | Notes that a build / secrets scan passed, version matrix, etc. |

## Hard rules for anything committed here

- **Never** include a real AppID / AppKEY / Authorization header.
- **Never** include raw prompts, full course text, or vendor response bodies.
- Raw `*.log` files are git-ignored; only intentionally-named redacted logs belong here.

See [SECURITY.md](../SECURITY.md).
