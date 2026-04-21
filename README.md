# Do I Need To?

Android MVP for interrupting habitual phone unlocks.

## What it does

- Lets the user choose how much friction to add before continuing after unlock.
- Uses an accessibility service to detect the post-unlock flow and launch an intervention screen.
- Asks the user why they opened the phone, with curated suggestions plus an editable custom reason.
- Logs unlock reasons locally on-device and shows recent history and daily unlock counts.
- Includes free scheduling so nudges only run during the user's chosen daily routine window.
- Includes free hard mode, lock-first answer options, and a manual lock button.
- If device admin is enabled, can lock the phone again when the user decides to stop.

## Important platform limits

- Android does not let a normal third-party app replace the real lock screen.
- Launching an activity right after unlock is sensitive to OEM behavior and Android background launch policies.
- Because of that, the most realistic MVP is an accessibility-based intervention rather than a true lock-screen replacement.

## Setup

1. Open the project in Android Studio.
2. Let Android Studio create or repair the Gradle wrapper if needed.
3. Install the app on a device.
4. In the app, enable the accessibility service.
5. Optionally enable device admin if you want the app to re-lock the phone.

## Debug release automation

This repo includes a GitHub Actions workflow at [.github/workflows/android-debug.yml](.github/workflows/android-debug.yml).

- Pushes to `main` build `app-debug.apk` and upload it as a workflow artifact.
- Pull requests also build the debug APK for quick verification.
- Pushing a tag like `v0.0.1-debug` builds the same debug APK and publishes it as a GitHub Release asset.

Useful commands:

```bash
git tag v0.0.1-debug
git push origin main --tags
```

## Main behavior

- `UnlockAccessibilityService` listens for `ACTION_USER_PRESENT` while running.
- After unlock, the next window transition triggers `InterventionActivity`.
- The friction slider controls how long the user must wait before the “continue” button becomes active.
- A daily schedule can limit nudges to a chosen start and end time, including overnight windows.
- The unlock sheet requires a reason before continuing and stores the answer with the outcome.
- Hard mode blocks easy dismissal and requires the user to explicitly continue or lock the device.
- Some curated answers are treated as “keep locked” intents and immediately re-lock the device.
- “Keep it locked” calls `DevicePolicyManager.lockNow()` when the app has device admin rights.
