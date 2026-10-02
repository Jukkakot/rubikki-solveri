# Tasks

## 1. Project skeleton

- [x] 1.1 Gradle wrapper 9.8.0, `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.gitignore`, `.editorconfig`, `.gitattributes`; verify `./gradlew help` runs
- [x] 1.2 `cube` module (Kotlin JVM, JDK 21) with a smoke test; verify `./gradlew :cube:test` passes
- [x] 1.3 `app` module (AGP 9.4.1, Compose, minSdk 31, target/compile 37, id `fi.jukkakot.rubikkisolveri`) with an empty activity; verify `./gradlew :app:assembleDebug` builds an APK
- [x] 1.4 `tools/setup-android-sdk.sh` and `.claude/settings.json` (allowed Gradle/OpenSpec commands, SessionStart hook for cloud sessions); verify the script is idempotent when run twice

## 2. Theme and language

- [x] 2.1 Material 3 theme with dynamic colour, light/dark fallbacks; theme preference in DataStore; verify unit test for preference round trip
- [x] 2.2 Finnish `values/strings.xml`, English `values-en/strings.xml`, AppCompat per-app locales with Finnish set on first start; verify a test that every Finnish string key has an English one and lint passes

## 3. Shell and navigation

- [x] 3.1 Home screen (app name, disabled "coming soon" entries for scan and manual input, settings button); verify Robolectric Compose test "App starts on home" and "Unbuilt feature"
- [x] 3.2 Settings screen (language, theme, link to the log) and navigation with back; verify Compose tests "Open and leave settings", "Switch to English", "Forced light"

## 4. Diagnostics

- [x] 4.1 `AppLog` with event catalogue, line format, Logcat + capped file; verify unit tests "Event recorded" and "Log stays small"
- [x] 4.2 Crash handler and next-start notice; verify unit test that a thrown error becomes one `app.crash` line and sets the marker
- [x] 4.3 Log screen (newest first, share via FileProvider, clear); verify Compose test "Clear the log" and a test that share builds an `ACTION_SEND` intent with the file

## 5. Project setup, CI and docs

- [x] 5.1 `openspec/context/nfr.md` adapted for an offline Android app; verify it covers logging, testing, error UX, workflow
- [x] 5.2 `.github/workflows/ci.yml` (test, lint, assembleDebug, APK artifact); verify the same command passes locally
- [x] 5.3 Docs wiki: `docs/README.md`, `architecture.md`, `development.md`, `operations.md`; mark roadmap item 0 done; verify links resolve
