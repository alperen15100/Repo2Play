# Repo2Play V10.1 Signed Engine

This package extends V10 Clean Engine with release signing.

## What it does
- Detects Android application project/module
- Resolves Gradle wrapper or fallback Gradle
- Builds release APK
- Builds release AAB
- Signs and verifies APK
- Signs and verifies AAB
- Produces Play preflight report
- Packages results as GitHub Actions artifact

## NEW mode
A new keystore is generated for the build and included in the artifact.
Keep it safe if you need the same signing identity later.

## UPDATE mode
Requires repository secrets:
- APP_KEYSTORE_BASE64
- SIGNING_KEY_ALIAS
- SIGNING_STORE_PASSWORD
- SIGNING_KEY_PASSWORD

The same signing identity must be used for an existing app update.

## Private target repo
Add:
- TARGET_REPO_TOKEN

with read access to the target repository.

## Output
- app-release-signed.apk
- app-release-signed.aab
- BUILD-REPORT.txt
- PLAY-REPORT.txt
- SIGNING-INFO.txt
- SHA256SUMS.txt
- repo2play-upload.jks (NEW mode)

## Important
A signed AAB is not by itself a guarantee that Google Play will accept an app. Package identity, versioning, policy declarations, app content, dependencies, target SDK, Data Safety and Play App Signing/update-key continuity can still matter.
