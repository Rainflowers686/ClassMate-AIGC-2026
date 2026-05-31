# Security

ClassMate is built so that secrets cannot leak — by construction, not just by convention.

## Credentials

- Real `AppID` / `AppKEY` (and any API key) **must never** appear in: Git, README, issues, PRs,
  proof artifacts, logs, screenshots, tests, or `config.example.json`.
- `config.example.json` ships **placeholders only**: `YOUR_BLUELM_APP_ID`, `YOUR_BLUELM_APP_KEY`.
- Real values are injected locally via **either**:
  1. a git-ignored `config.local.json` (copy the example, fill in locally), or
  2. the **debug-only** config import entry in Settings (round 1 inspects only; it does not persist
     and never echoes a secret value — only field names / booleans).
- `Credential` is an opaque holder whose `toString()` returns `***`, so an accidental log/crash
  dump cannot print it.

## What is logged (and what is never)

`RedactedLogEntry` is the only log type for provider activity. It has exactly these fields:

```
provider, status, latency_ms, validation, fallback_used, error_type
```

There is **structurally nowhere** to record:

- AppID / AppKEY / Authorization headers,
- prompts or the full course text,
- vendor response bodies,
- user-private data.

Errors are a closed enum (`ProviderErrorType`) plus an optional numeric HTTP status. Provider
exception handling deliberately drops `e.message` so a response body can't ride along.
See a sample: [`proof/logs_redacted/sample_analysis_redacted.log`](proof/logs_redacted/sample_analysis_redacted.log).

## .gitignore (enforced)

`local.properties`, `config.local.json`, `secrets.properties`, `.env*`, `*.jks`, `*.keystore`,
`keystore.properties`, `build/`, `.gradle/`, and packaged apps (`*.apk` / `*.aab`).

## Automated checks

- **Secrets scan** ([`scripts/secrets_scan/secrets_scan.sh`](scripts/secrets_scan/secrets_scan.sh),
  PowerShell mirror for local) runs in CI on every push / PR. It fails the build if a forbidden file
  is tracked or a secret-like field holds a non-placeholder value (length ≥ 10).
- `ProviderConfigSafetyCheck.inspectExampleConfig()` asserts an example config contains only
  placeholders (unit-tested), and reports findings by **field name**, never value.
- CI never uploads the APK as an artifact.

## Evidence integrity

Beyond secrets: ClassMate refuses to present model conclusions that can't be traced to the source.
A `KnowledgePoint` / `QuizQuestion` with no locatable `EvidenceSpan` fails `ResultValidator` and
never reaches the UI (it triggers fallback instead).

## Reporting

Found a way to leak a secret, or a conclusion without evidence? Open an issue **without** including
the secret itself — use redacted logs only. See the issue templates.
