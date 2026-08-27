#!/usr/bin/env bash
set -uo pipefail
TARGET="${1:?target required}"; OUTPUT="${2:?output required}"
ENGINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rm -rf "$OUTPUT"; mkdir -p "$OUTPUT"
REPORT="$OUTPUT/BUILD-REPORT.txt"
printf 'REPO2PLAY V10 CLEAN ENGINE\n=========================\nRepository: %s\nBranch: %s\n\n' "${TARGET_REPOSITORY:-unknown}" "${TARGET_BRANCH:-unknown}" > "$REPORT"
fail(){ echo "FINAL RESULT: BLOCKED - $1" | tee -a "$REPORT"; echo "$1" > "$OUTPUT/ERROR.txt"; exit 1; }
"$ENGINE/scripts/detect-project.sh" "$TARGET" "$OUTPUT/detect.env" >> "$REPORT" 2>&1 || fail "Android application project could not be detected."
source "$OUTPUT/detect.env"
"$ENGINE/scripts/resolve-gradle.sh" "$PROJECT_DIR" "$OUTPUT/gradle.env" >> "$REPORT" 2>&1 || fail "Compatible Gradle could not be prepared."
source "$OUTPUT/gradle.env"
"$ENGINE/scripts/build.sh" "$PROJECT_DIR" "$GRADLE_CMD" "$APP_MODULE" "$OUTPUT" >> "$REPORT" 2>&1 || fail "Android release build failed."
"$ENGINE/scripts/doctor.sh" "$PROJECT_DIR" "$OUTPUT" >> "$REPORT" 2>&1 || true
"$ENGINE/scripts/package.sh" "$OUTPUT" >> "$REPORT" 2>&1 || fail "Packaging failed."
echo "FINAL RESULT: SUCCESS" >> "$REPORT"
