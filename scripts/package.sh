#!/usr/bin/env bash
set -euo pipefail
OUTPUT="${1:?}"; [ -f "$OUTPUT/app-release.apk" ]; [ -f "$OUTPUT/app-release.aab" ]
(cd "$OUTPUT" && sha256sum app-release.apk app-release.aab > SHA256SUMS.txt)
echo "PASS Final package prepared"
