# Google Play Data Safety draft

Review these answers in Play Console before publishing.

## Data handled
Authentication information: GitHub personal access token.
User content / developer content: repository identifier, branch name, build inputs, release artifacts and signing key.

## Processing
- GitHub token is encrypted on the device with Android Keystore.
- Token is transmitted to GitHub only over HTTPS for GitHub API requests.
- Repository/build inputs are sent to GitHub / GitHub Actions to perform the user-requested build.
- Per-project JKS is encrypted in the app's local vault and is also included in the user's downloaded release package.
- Ecrin Labs does not receive these values through a separate application backend in this version.

## Suggested Play Console framing
Data is processed to provide app functionality.
No advertising SDK is included.
No analytics SDK is included.
No sale of user data.
No independent Ecrin Labs server-side collection in this version.

The final Data Safety answers must match the exact production distribution and any services added later.
