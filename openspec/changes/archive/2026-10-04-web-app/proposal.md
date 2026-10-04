# Proposal

## Why

The user wants the same app in a web browser too: open it from a link on any phone or computer,
without installing an APK. Doing it as a second, separate code base would double every future
change; the cube logic is already pure Kotlin and the screens are Compose, so both platforms can
share one code base with Kotlin Multiplatform and Compose Multiplatform (Kotlin/Wasm in the
browser). A spike on 2026-10-04 confirmed the toolchain works with the project's versions.

## What Changes

- **Browser version** at `https://jukkakot.github.io/rubikki-solveri/`, rebuilt and published on
  every green push to `main`: every feature of the phone app (scan with the camera and the colour
  check, manual input, free cube, shortest solution and the move guide, camera follow, beginner
  method, lessons and practice, timer, history and statistics, settings, log, about).
- Installable as a home-screen app (PWA) and works offline after the first visit; data (settings,
  solves, log) stays in that browser. No server, no accounts, still 0 €.
- **One code base:** the `cube` module becomes multiplatform (JVM + browser); the vendored
  min2phase Java solver is ported to Kotlin (checked move-for-move against the Java original);
  the screens, navigation, theme, texts, fonts and pictures move from `app` into a new
  multiplatform `shared` module used by both the Android app and the new `web` module. Platform
  parts stay per platform: camera, storage, log files, language switching, sharing.
- The Android app keeps behaving exactly as before (same screens, same data, same database); this
  is checked with the existing tests and a before/after comparison of every screenshot.
- Differences in the browser, by necessity: fixed Karkki colours instead of Material You; language
  change reloads the page; the camera's exposure lock and torch only where the browser supports
  them; vibration only where supported; on wide screens the app is a phone-width column.

Modules: `cube` (multiplatform conversion, min2phase port), `app` (becomes the Android shell),
new `shared`, new `web`; CI workflows.

## Capabilities

### New Capabilities
- `web-app`: the browser version — where it lives and how it updates, browser support, install
  and offline use, data kept in the browser, and how camera, language, theme, sharing, vibration,
  keyboard and wide screens work there. Everything else follows the existing capabilities.

### Modified Capabilities
(none — the existing requirements apply unchanged to both platforms; browser-specific rules are in
`web-app`)

## Impact

- Gradle: Kotlin Multiplatform, Compose Multiplatform 1.12.1, the Android KMP library plugin,
  JetBrains navigation/lifecycle, kotlinx-datetime, kotlinx-browser; `kotlin-js-store` lock file.
- `cube` source sets move (`src/main` → `src/commonMain`/`jvmMain`, tests → `jvmTest`).
- Most of `app/src/main/kotlin/.../ui` and the string, font, drawable and raw resources move to
  `shared`; Room, DataStore, the file log, CameraX and AppCompat language stay Android-side.
- New `web` module, `.github/workflows/pages.yml`, CI builds and smoke-tests the browser build.
- User step once: GitHub → Settings → Pages → Source "GitHub Actions".
- Depends on nothing in `phone-install`, but both edit CI; apply `phone-install` first.
