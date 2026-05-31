#!/usr/bin/env bash
# Runs the same checks as CI, locally.
set -euo pipefail
cd "$(dirname "$0")/../.."
echo "== core tests =="
./gradlew :core:test --console=plain
echo "== app unit tests =="
./gradlew :app:testDebugUnitTest --console=plain
echo "== assemble debug =="
./gradlew :app:assembleDebug --console=plain
echo "== secrets scan =="
bash scripts/secrets_scan/secrets_scan.sh
echo "All local checks passed."
