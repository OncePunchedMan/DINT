# Release APKs and In-App Updates

This project builds debug APKs in GitHub Actions and creates a GitHub Release APK whenever `master` receives a push or merge.

## Triggers

- Every branch push builds a debug APK artifact for testing branch-specific changes.
- Every pull request builds the same debug APK artifact.
- Every push to `master` also creates a GitHub Release containing `Dint-debug.apk`.
- The workflow can also be run manually from GitHub Actions.

## Signing

Android only allows an installed app to update to an APK signed with the same key. For reliable updates, add these repository secrets:

- `DINT_KEYSTORE_BASE64`: base64-encoded keystore file.
- `DINT_KEYSTORE_PASSWORD`: keystore password.
- `DINT_KEY_ALIAS`: key alias.
- `DINT_KEY_PASSWORD`: key password.

If these secrets are missing, the workflow still builds APKs, but the runner's generated debug key may change and Android may reject updates over an existing install.

## In-App Update Flow

The app checks `https://api.github.com/repos/OncePunchedMan/DINT/releases/latest`, finds the first `.apk` asset, downloads it into app cache, and opens Android's package installer. Android still shows the install/update prompt; the app does not silently update itself.
