# Development

## Setup — Implemented

- **Windows (the author):** Android Studio (it brings its own JDK and the Android SDK). Open the
  repo folder; Studio syncs Gradle by itself. See [operations.md](operations.md) for running on
  the phone.
- **Linux / Claude's cloud container:** JDK 21, then `tools/setup-android-sdk.sh` (installs the
  command-line tools, platforms and build tools into `/opt/android-sdk` and writes
  `local.properties`). Cloud sessions run it automatically from the SessionStart hook.
- Versions live in `gradle/libs.versions.toml`; the Gradle wrapper pins Gradle itself.

## Checks — Implemented

Run before every commit (CI runs the same):

```
./gradlew test lint assembleDebug
```

- `test`: `cube` JVM tests and `app` unit tests. App tests run on the JVM with Robolectric
  (Compose UI tests included); the default test locale is Finnish (`robolectric.properties`), a
  test can switch with `@Config(qualifiers = "en")`.
- `lint`: Android lint, warnings are errors. Dependency-version checks are off (updated by hand).
- The debug APK lands in `app/build/outputs/apk/debug/`.

## Testing approach

| Level | Tools | Status |
|---|---|---|
| Cube logic | JUnit 4 + kotlin-test in `cube`; test names follow spec scenarios | Implemented |
| App logic | JUnit in `app/src/test` (plain JVM where possible) | Implemented |
| Screens | Compose UI tests on Robolectric: key interactions only | Implemented |
| On the phone | Manual, listed under "How to check" in each change summary | — |

## Screenshots without a phone — Implemented

`./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'` renders key screens with
Robolectric's native graphics into `app/build/screenshots/*.png` (Galaxy S24-sized, light and
dark). It only fails when rendering crashes; look at the images to check layout and the 3D cube.

## Debugging — Implemented

- **Log:** every event is one line in Logcat (tag `Rubikki`) and in the app's log file. In
  Android Studio: Logcat, filter `tag:Rubikki`. On the phone: Settings → Log (share it to send).
- **Crash:** the stack is in the `app.crash` line; the next start offers the log.
- Adding an event: add it to `Evt`, log it with `AppLog.logger.info(Evt.X, msg, "key" to value)`.

## Conventions — Implemented

- English for code and identifiers; UI text from string resources: Finnish in `values/`, English
  in `values-en/` (a test checks every key exists in both).
- Prefer established libraries over hand-written plumbing; cube logic is our own code except the
  vendored two-phase solver.
- Conventional commits, directly on `main`.
- Planning: OpenSpec (`/opsx:*`). Each change updates the wiki pages it affects.
