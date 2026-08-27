
# Repo2Play — Google Play A–Z Submission Pack

Developer: Ecrin Labs
Package: com.ecrinlabs.repo2play
Target SDK: 36
Min SDK: 26
Version: 12.0.0 (12)

## 1. Production artifact
Use only:
- Repo2Play-v12-playstore.aab for Google Play
- Repo2Play-v12-release.apk for direct testing

The Store Release workflow fails if either file is missing.

## 2. App access
The app requires the user's own GitHub Personal Access Token.
In Play Console > App content > App access, explain that no Ecrin Labs account is required.
If Google review needs to exercise authenticated functionality, provide a dedicated reviewer GitHub token/account with the minimum permissions required for the review project. Never provide your personal production token.

## 3. Privacy policy
Google Play requires an active public non-PDF privacy-policy URL and a privacy-policy link/text inside the app.
The repository includes docs/privacy.html.
Enable GitHub Pages on the repository and verify this URL works before submission:
https://alperen15100.github.io/Repo2Play/privacy.html

The app contains a Privacy Policy button pointing to this address.

## 4. Data Safety draft
Complete the Play Console Data Safety form consistently with store/DATA-SAFETY.md.
Current code:
- encrypts GitHub token on device with Android Keystore
- encrypts per-project signing-key vault on device
- transmits build/API requests to GitHub via HTTPS
- does not include ad or analytics SDKs
- does not operate an Ecrin Labs data backend

Do not claim "no data collected" without considering Google's definition of collection and the GitHub processing flow. Answer the console questionnaire based on the exact production behavior and Google definitions.

## 5. Permissions
Current app requests:
- INTERNET
It does not request MANAGE_EXTERNAL_STORAGE, contacts, location, microphone, camera, phone, SMS, accessibility or VPN permissions.
POST_NOTIFICATIONS is not required by current behavior and should not be requested unless notifications are added.

## 6. Content rating
Complete the IARC questionnaire truthfully.
Repo2Play is a developer utility and has no user-generated public feed, gambling, violence, sexual content or social communication feature in the current build.

## 7. Target audience
Choose the real intended audience. This is a developer utility and is not designed for children.
Do not select children unless the product is actually designed for them and Families requirements are met.

## 8. Ads
Current build contains no ad SDK.
Declare "No" for ads unless an ad SDK is added later.

## 9. Store listing
App name: Repo2Play
Developer attribution: Ecrin Labs

Short description:
Build signed Android APK and Play Store AAB releases from GitHub on your phone.

Full description is in PLAY-STORE-LISTING.md.

## 10. Required graphic assets
Play Console app icon:
- 512 x 512
- 32-bit PNG with alpha
- <= 1024 KB

Feature graphic:
- 1024 x 500
- JPEG or 24-bit PNG, no alpha

Phone screenshots:
- at least 2
- JPEG or 24-bit PNG, no alpha
- 320–3840 px
- maximum dimension no more than 2x the minimum dimension

Recommended screenshots:
1. Secure GitHub connection
2. Repository + NEW/UPDATE
3. Build in progress
4. Release ready: APK + AAB + JKS
5. Recent Builds + Signing Vault

## 11. Store review test
Before production submission:
- install release APK on a clean phone
- connect a reviewer/test GitHub token
- perform NEW build
- download ZIP
- verify signed APK installs
- verify AAB exists
- verify JKS exists
- perform UPDATE using the stored JKS
- verify same signing identity
- clear app data and confirm token/vault disappear
- verify Privacy Policy URL opens from app
- verify no secrets appear in GitHub logs

## 12. Signing
Keep the Repo2Play app's own Play upload key safe.
For target projects built by Repo2Play, NEW release ZIP contains the generated JKS backup.
UPDATE is blocked if the matching key is unavailable.

## 13. Play App Signing
For Repo2Play itself, enroll/use Google Play App Signing as required by Play Console.
The AAB uploaded to Play is signed with the configured upload key.

## 14. Before pressing "Send for review"
Verify:
- Store listing complete
- App icon uploaded
- Feature graphic uploaded
- minimum 2 screenshots uploaded
- privacy URL live
- Data Safety completed
- App access completed
- Ads declaration completed
- Content rating completed
- Target audience completed
- AAB uploaded successfully
- pre-launch report reviewed
- no unresolved policy warnings in App content
