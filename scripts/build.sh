#!/usr/bin/env bash
set -euo pipefail
PROJECT="${1:?}"; GRADLE="${2:?}"; MODULE="${3-}"; OUTPUT="${4:?}"; cd "$PROJECT"
PREFIX=""; [ -n "$MODULE" ] && PREFIX=":${MODULE}:"
"$GRADLE" "${PREFIX}assembleRelease" --no-daemon --stacktrace
"$GRADLE" "${PREFIX}bundleRelease" --no-daemon --stacktrace
APK="$(find "$PROJECT" -type f -name '*.apk' ! -path '*/intermediates/*' | grep '/release/' | head -1 || true)"
AAB="$(find "$PROJECT" -type f -name '*.aab' ! -path '*/intermediates/*' | grep '/release/' | head -1 || true)"
[ -n "$APK" ] || { echo "APK not found"; exit 1; }; [ -n "$AAB" ] || { echo "AAB not found"; exit 1; }
cp "$APK" "$OUTPUT/app-release.apk"; cp "$AAB" "$OUTPUT/app-release.aab"
echo "PASS APK"; echo "PASS AAB"
