#!/usr/bin/env bash
# ClassMate secrets scan (used by CI). Exits non-zero if a real-looking secret or a
# forbidden file is tracked by git. Placeholders (YOUR_..., REPLACE_ME, ...) are allowed,
# so config.example.json passes while a real key would fail.
set -uo pipefail

status=0
echo "== ClassMate secrets scan =="

# 1) Forbidden files must never be tracked.
while IFS= read -r f; do
  case "$f" in
    config.local.json|secrets.properties|.env|.env.*|*.jks|*.keystore|keystore.properties|signing.properties)
      echo "::error:: forbidden file is tracked by git: $f"
      status=1
      ;;
  esac
done < <(git ls-files)

# 2) Real-looking secret values assigned to secret-like fields in tracked text files.
PLACEHOLDER='YOUR_|REPLACE_ME|CHANGEME|PLACEHOLDER|EXAMPLE|XXXX|0000000000'
FIELDS='appId|appKey|apiKey|app_id|app_key|api_key|secret|token'

while IFS= read -r f; do
  [ -f "$f" ] || continue
  # Test fixtures intentionally contain synthetic, real-looking credentials to exercise the
  # detector; real secrets never belong in tests. Skip test sources for the value scan.
  case "$f" in */src/test/*) continue;; esac
  while IFS= read -r pair; do
    val=$(printf '%s' "$pair" | grep -oE '"[^"]*"[[:space:]]*$' | tr -d '"' | sed 's/[[:space:]]*$//')
    [ -n "$val" ] || continue
    printf '%s' "$val" | grep -qiE "$PLACEHOLDER" && continue
    if [ "${#val}" -ge 10 ]; then
      echo "::error:: possible real secret in $f -> $pair"
      status=1
    fi
  done < <(grep -oEi "\"($FIELDS)\"[[:space:]]*:[[:space:]]*\"[^\"]+\"" "$f" 2>/dev/null || true)
done < <(git ls-files '*.json' '*.kt' '*.kts' '*.xml' '*.properties' '*.md' '*.yml' '*.yaml')

# 3) Private key material (exclude this scanner dir to avoid matching its own pattern).
if git grep -nE '[-]{5}BEGIN [A-Z ]*PRIVATE KEY[-]{5}' -- . ':!scripts/secrets_scan' >/dev/null 2>&1; then
  echo "::error:: private key material found in tracked files"
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "OK: no secrets or forbidden files detected."
fi
exit "$status"
