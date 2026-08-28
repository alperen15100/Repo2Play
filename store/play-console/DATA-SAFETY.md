# Repo2Play V12.1 — Data Safety

Developer: Ecrin Labs

Repo2Play uses the user's own GitHub authentication
for user-requested GitHub operations.

GitHub credentials:
- entered by the user
- protected locally using Android security mechanisms
- used for GitHub API communication over HTTPS
- not intentionally sent to an Ecrin Labs analytics or advertising server

Repository information:
- repository owner/name
- branch
- build mode

This information is used for core build functionality.

Signing material:
Repo2Play can store project signing material in its
local encrypted Signing Vault so UPDATE releases can
retain the original Android signing identity.

The user can remove locally stored credentials and
Signing Vault data from within the application.

Repo2Play V12.1 contains no Ecrin Labs advertising SDK.

IMPORTANT:
Re-check this declaration against the exact final AAB
before submitting Google Play Data Safety.
