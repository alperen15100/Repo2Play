#!/usr/bin/env bash
set -euo pipefail
PROJECT="${1:?}"; OUT="${2:?}"; cd "$PROJECT"
if [ -f gradlew ]; then
  chmod +x gradlew; CMD="$PROJECT/gradlew"; echo "PASS Existing Gradle wrapper"
elif [ -f gradle/wrapper/gradle-wrapper.properties ]; then
  URL="$(grep '^distributionUrl=' gradle/wrapper/gradle-wrapper.properties | cut -d= -f2- | sed 's#\\:#:#g' || true)"
  VER="$(printf '%s' "$URL" | sed -n 's/.*gradle-\([0-9][0-9.]*\)-.*/\1/p')"; [ -n "$VER" ] || exit 1
  DIR="$RUNNER_TEMP/repo2play-gradle-$VER"; rm -rf "$DIR"; mkdir -p "$DIR"
  curl -fsSL --retry 3 "https://services.gradle.org/distributions/gradle-${VER}-bin.zip" -o "$DIR/gradle.zip"; unzip -q "$DIR/gradle.zip" -d "$DIR"
  CMD="$DIR/gradle-$VER/bin/gradle"; echo "RECOVERY Missing gradlew; using Gradle $VER"
else
  VER="8.7"; DIR="$RUNNER_TEMP/repo2play-gradle-$VER"; rm -rf "$DIR"; mkdir -p "$DIR"
  curl -fsSL --retry 3 "https://services.gradle.org/distributions/gradle-${VER}-bin.zip" -o "$DIR/gradle.zip"; unzip -q "$DIR/gradle.zip" -d "$DIR"
  CMD="$DIR/gradle-$VER/bin/gradle"; echo "RECOVERY No wrapper metadata; trying Gradle $VER"
fi
"$CMD" --version
printf 'GRADLE_CMD=%q\n' "$CMD" > "$OUT"
