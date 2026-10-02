# Design

## Context

Empty repo apart from docs and OpenSpec. Constraints: offline, 0 €, only the author's Galaxy S24
(Android 14+), minimum Android 12 (product.md). Claude builds and tests in a Linux container
without a phone or emulator, so as much as possible must be testable on the JVM.

## Goals / Non-Goals

**Goals:** a project that builds the same in the container, in CI and in Android Studio on
Windows; JVM-runnable tests for logic and key Compose interactions; a debugging story good enough
that a shared log file explains a failure.

**Non-Goals:** release signing (roadmap `release`), remote log upload (Axiom, later), instrumented
tests on a device or emulator, onboarding for other users.

## Decisions

- **Modules `cube` (Kotlin JVM library) and `app` (Android application).** Cube logic gets fast
  plain-JVM tests and cannot accidentally depend on Android. Alternative: one module with packages;
  rejected because nothing would stop Android imports leaking into the logic.
- **Versions** (current stable on 2026-10-02): AGP 9.4.1 with its built-in Kotlin, Kotlin 2.4.20,
  Gradle 9.8.0 (wrapper), Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 31, JDK 21
  toolchain. All versions in `gradle/libs.versions.toml`.
- **Application id `fi.jukkakot.rubikkisolveri`.** No store listing, so it can still change, but
  it stays stable so installs update in place.
- **Navigation: Navigation Compose with type-safe `@Serializable` routes.** Established, small,
  and testable with `TestNavHostController`. Alternative Navigation 3 is newer and would be fine,
  but has fewer examples for a beginner reading the code.
- **Language via AppCompat per-app locales** (`AppCompatDelegate.setApplicationLocales`, with
  `autoStoreLocales` so it also works on Android 12). On Android 13+ the choice also shows in the
  phone's per-app language settings. The default Finnish comes from `values/strings.xml` being
  Finnish and `values-en/` English; on first start the app sets `fi` explicitly so a phone in
  English still starts Finnish (product.md: Finnish default). The host activity is an
  `AppCompatActivity` for this reason.
- **Theme setting in DataStore Preferences** (`follow phone` / `light` / `dark`), read as a Flow
  by the activity. Dynamic colour on Android 12+ (always true at minSdk 31); static fallback
  schemes are still defined for previews and tests.
- **Logging: our own tiny `AppLog`** with a fixed `Evt` catalogue (`app.start`, `app.crash`,
  `nav.screen`, `settings.changed`, …). One line per event, NFR order: `ts level evt key=value …
  msg`. Writes to Logcat and appends to `files/logs/app.log`; when the file passes 512 kB it keeps
  the newest half. Writes go through a single-thread executor so the UI never waits on disk.
  Alternative Timber: adds little over Logcat for our needs and we need the file anyway.
- **Crash capture:** a default uncaught-exception handler writes `app.crash` with the stack
  flattened to one field, synchronously, then calls the previous handler. A marker file makes the
  next start show a snackbar "Sovellus kaatui viimeksi – näytä loki".
- **Sharing the log** through a `FileProvider` and `ACTION_SEND`.
- **Tests:** `cube` uses JUnit 4 + kotlin-test. `app` uses JVM unit tests with Robolectric for
  Compose interactions (`createComposeRule`), so CI needs no emulator. Robolectric runs at the
  newest SDK it supports (`robolectric.properties`), not necessarily 37.
- **Lint:** Android lint with `abortOnError` and `warningsAsErrors`, `GradleDependency`/
  `NewerVersionAvailable` disabled (dependencies are updated by hand). No formatter.
- **CI:** one job on ubuntu: JDK 21, `gradle/actions/setup-gradle`, `./gradlew test lint
  assembleDebug`, upload the debug APK as an artifact (7 days). The version name carries the
  short commit so a log line tells which build produced it.
- **Container setup:** `tools/setup-android-sdk.sh` installs the command-line tools, platform and
  build tools into `/opt/android-sdk`; a SessionStart hook in `.claude/settings.json` runs it in
  cloud sessions only, so future sessions can build at once.

## Risks / Trade-offs

- [Robolectric lags new SDKs] → pin its SDK in `robolectric.properties`; Compose behaviour is the
  same.
- [AGP 9 built-in Kotlin is new] → if it misbehaves, fall back to the classic Kotlin Android
  plugin; recorded here if it happens.
- [Dynamic colour cannot be checked without the phone] → listed under "How to check".

## Decisions made while building

- Robolectric needs `--add-opens java.base/java.io` and `--add-exports
  java.base/jdk.internal.access` on JDK 21 (set for all unit tests in `app/build.gradle.kts`).
- Robolectric does not apply per-app locales, so "Switch to English" is covered in two parts: a
  Compose test that picking English calls the language setter, and a test that the English
  resources render (`@Config(qualifiers = "en")`). Applying it for real is checked on the phone.
- Test default locale is Finnish (`robolectric.properties`), matching the app default.
