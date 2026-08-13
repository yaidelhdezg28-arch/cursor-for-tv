# TV Cursor

A tiny Android TV app that lets you control an on-screen mouse cursor using
only the D-pad on your standard TV remote — no touchpad, air-mouse, or phone
app required.

## How it works

- It's an **Accessibility Service**, which is the only way a non-touch
  Android TV app can inject synthetic taps at arbitrary screen coordinates.
- **Long-press BACK** on the remote toggles "cursor mode" on/off.
- While cursor mode is on: D-pad **up/down/left/right** moves the cursor,
  **center/OK** clicks at the cursor's position.
- While cursor mode is off: the remote behaves exactly as normal (short
  BACK press still works normally too).

## Build it — no install, entirely in the browser (easiest)

This repo includes a GitHub Actions workflow (`.github/workflows/build.yml`)
that compiles the APK in the cloud using the real Android build tools —
no "APK converter" website needed, and nothing to install locally.

1. Go to [github.com](https://github.com) and create a new **public or
   private repository** (e.g. `tv-cursor`).
2. On the repo page, click **"uploading an existing file"** (or **Add
   file > Upload files**) and drag in the entire contents of this
   `TVCursor` folder (keep the folder structure — `app/`, `.github/`,
   `build.gradle`, `settings.gradle`, etc. all need to be at the repo root).
3. Commit the upload to the `main` branch.
4. Click the **Actions** tab at the top of the repo — a workflow called
   "Build APK" will already be running (it auto-triggers on push). Wait
   for the green checkmark (a couple of minutes).
5. Click into that workflow run, scroll to **Artifacts**, and download
   **TVCursor-debug-apk** — that's a zip containing `app-debug.apk`.
6. Copy that APK to your TV (via `adb install app-debug.apk`, a USB
   stick with a file manager app, or a "Send files to TV" app) and
   install it.

This uses only GitHub's own infrastructure to compile your code — it's
the same Gradle/Android SDK toolchain Android Studio uses locally, just
running on GitHub's servers instead of your machine.

## Build it locally with Android Studio

1. Open the `TVCursor` folder in **Android Studio** (Giraffe or newer).
   Let Gradle sync — it will download the Android Gradle Plugin, Kotlin
   plugin, and AndroidX/Material libraries automatically.
2. Connect your Android TV device (or an Android TV emulator) with
   `adb connect <device-ip>:5555`, or plug in a TV that has USB debugging
   enabled.
3. Click **Run** ▶ in Android Studio, or build an APK via
   **Build > Build Bundle(s)/APK(s) > Build APK(s)** and sideload it with:
   ```
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Install/enable on the TV

1. Launch **TV Cursor** from your TV's app list.
2. Press the "Open Accessibility Settings" button (or navigate to
   **Settings > Accessibility** manually).
3. Find **TV Cursor Control** in the list and turn it on.
4. Exit settings. From anywhere in Android TV, **long-press BACK** on the
   remote to show the cursor, use the D-pad to move it, press **OK** to
   click, and long-press **BACK** again to hide it.

## Customizing

Everything lives in two files:

- `MainActivity.kt` — the onboarding screen.
- `CursorAccessibilityService.kt` — all the actual cursor/remote logic:
  - `longPressThresholdMs` — how long BACK must be held to toggle (default 550ms).
  - `baseStep` in `moveCursor()` — how far the cursor moves per D-pad press,
    with built-in acceleration the longer you hold a direction.
  - The toggle key is BACK by default because it's present on every Android
    TV remote; swap `KeyEvent.KEYCODE_BACK` for another key code if you'd
    rather use e.g. long-press on the Home button (note: Home is reserved
    by the system on most devices and can't always be intercepted).

## Notes / limitations

- Works on Android TV / Google TV, API 24+.
- Uses `TYPE_ACCESSIBILITY_OVERLAY`, so no extra "draw over other apps"
  permission dialog is needed — enabling the Accessibility Service is
  the only setup step.
- Clicking is done via `dispatchGesture`, which synthesizes a real tap, so
  it works inside almost any app, not just system UI.
- If your remote lacks a distinct "long press" (rare), you can rebind the
  toggle to a double-press pattern instead — ask and I can adjust the logic.
