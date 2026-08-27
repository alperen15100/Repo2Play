REPO2PLAY V12 RELEASE

This package is designed to be copied into the existing Repo2Play repository while checked out on v12-release.

What it changes/adds:
- android-app/ : production Repo2Play Android app
- .github/workflows/build-app.yml : signed APK + Play Store AAB for Repo2Play itself
- .github/workflows/repo2play.yml : V12 engine workflow with per-project UPDATE keystore input
- store/ and docs/ : Play Store listing, privacy policy and Data Safety draft

It does NOT replace scripts/*.sh. The tested V11 engine scripts remain intact.

After copying to the repository:
git add -A
git commit -m "Repo2Play V12 release"
git push origin v12-release

Then run:
Actions -> Repo2Play V12 Store Release -> Run workflow

Expected artifact:
Repo2Play-V12-Store-Release/
  Repo2Play-v12-release.apk
  Repo2Play-v12-playstore.aab
  SHA256SUMS.txt
