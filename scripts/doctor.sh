#!/usr/bin/env bash
set -euo pipefail
PROJECT="${1:?}"; OUTPUT="${2:?}"; R="$OUTPUT/PLAY-REPORT.txt"
TARGET="$(grep -RhoE 'targetSdk(Version)?[[:space:]=()]+[0-9]+' "$PROJECT" --include='*.gradle' --include='*.gradle.kts' 2>/dev/null | grep -oE '[0-9]+' | sort -nr | head -1 || true)"
{
 echo "REPO2PLAY PLAY STORE DOCTOR"; echo "=========================="; echo "Detected targetSdk: ${TARGET:-unknown}"
 if [ "${TARGET:-0}" -ge 36 ] 2>/dev/null; then echo "PASS targetSdk >= 36"; else echo "WARNING targetSdk below engine baseline 36 or not detected"; fi
 echo "NOTE: Technical preflight; not a guarantee of Google Play approval."
} > "$R"
cat "$R"
