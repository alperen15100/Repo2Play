REPO2PLAY FINAL — MODEL B

1) Build this Android app with .github/workflows/build-app.yml.
2) Install the APK.
3) In the app, paste the GitHub PAT of the account that has access to the engine repository.
4) Engine repository: owner/Repo2Play
5) Target repository: owner/android-project
6) Branch: main
7) Choose NEW or UPDATE and tap BUILD APP.
8) The app dispatches repo2play.yml on ref v11-production, polls the exact returned workflow_run_id, then downloads the workflow artifact ZIP.

Required token access for the engine repository:
- Actions: write (dispatch)
- Actions: read (run status + artifact)
The token also needs whatever repository access your engine workflow requires for the target repository.

IMPORTANT ENGINE REQUIREMENT:
The engine repository must contain the already-tested V11 workflow at:
.github/workflows/repo2play.yml
and ref:
v11-production

This package does NOT replace or modify the tested V11 engine.
