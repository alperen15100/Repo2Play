#!/usr/bin/env bash
set -euo pipefail
PROJECT="${1:?}"
OUTPUT="${2:?}"
R="$OUTPUT/PLAY-REPORT.txt"

{
  echo "REPO2PLAY PLAY STORE DOCTOR"
  echo "=========================="
  echo
  if find "$PROJECT" -type f -name AndroidManifest.xml ! -path '*/build/*' | grep -q .; then
    echo "PASS AndroidManifest found"
  else
    echo "WARNING AndroidManifest not found"
  fi

  TARGET="$(grep -RhoE 'targetSdk(Version)?[[:space:]=()]+[0-9]+' "$PROJECT" --include='*.gradle' --include='*.gradle.kts' 2>/dev/null | grep -oE '[0-9]+' | sort -nr | head -1 || true)"
  COMPILE="$(grep -RhoE 'compileSdk(Version)?[[:space:]=()]+[0-9]+' "$PROJECT" --include='*.gradle' --include='*.gradle.kts' 2>/dev/null | grep -oE '[0-9]+' | sort -nr | head -1 || true)"

  echo "Detected targetSdk: ${TARGET:-unknown}"
  echo "Detected compileSdk: ${COMPILE:-unknown}"

  if [ "${TARGET:-0}" -ge 36 ] 2>/dev/null; then
    echo "PASS targetSdk >= 36"
  else
    echo "WARNING targetSdk below current engine baseline 36 or not detected"
  fi

  if grep -R -q 'android:debuggable="true"' "$PROJECT" --include='AndroidManifest.xml' 2>/dev/null; then
    echo "WARNING debuggable=true detected"
  else
    echo "PASS no explicit debuggable=true"
  fi

  echo
  echo "NOTE: Technical preflight only; not a guarantee of Google Play approval."
} > "$R"

cat "$R"
