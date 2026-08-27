# Repo2Play V11 Production Stage 1

Built on top of the working V10.1 signed engine.

## Deliberately preserved
- Android project detection
- Gradle recovery/build path
- APK build
- AAB build
- APK signing + verification
- AAB signing + verification
- NEW/UPDATE signing model

## Added
- UPDATE versionCode auto-increment
- versionName best-effort patch increment
- VERSION-INFO.json
- stronger Play Store technical preflight
- API 36 mobile submission baseline warning for 2026-08-31
- removal of internal detect/gradle env files from customer output

## Safety
`main` and tag `v10.1-golden` should remain untouched.
Develop and test this package only on `v11-production`.

## Important
Google Play approval cannot be guaranteed automatically. App content, permissions, data safety disclosures, SDK behavior, account policy, Play App Signing and other Play Console requirements can still matter.
