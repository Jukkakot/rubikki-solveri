# Proposal

## Why

Nothing runs yet. Every later change (cube model, 3D view, solver, camera) needs a buildable
Android project, a pure Kotlin module for cube logic, a theme and navigation shell to hang screens
on, a way to see what went wrong on the phone, and CI that keeps `main` green.

## What Changes

- Gradle project with two modules: `cube` (pure Kotlin/JVM, no Android imports) and `app`
  (Android, Jetpack Compose, Material 3).
- App shell: home screen with the planned entry points (scan, manual input, both "coming soon"
  until their changes land), a settings screen, navigation between them.
- Theme: Material 3 with Material You dynamic colour, light and dark; follows the phone unless the
  user picks one in settings.
- Language: Finnish by default, English selectable in settings; all UI text from string resources.
- Diagnostics: a local log (Logcat plus a size-capped file on the phone), uncaught crashes logged,
  an in-app log viewer with share, and a friendly crash notice on the next start.
- Generic project setup from the game-kit template, minus everything game/web-specific: docs wiki
  (README, architecture, development, operations), `openspec/context/nfr.md` for an offline Android
  app, `.claude/settings.json` with allowed Gradle commands, `.editorconfig`/`.gitattributes`.
- GitHub Actions CI: unit tests, Android lint and a debug APK build (APK uploaded as an artifact).

## Capabilities

### New Capabilities
- `app-shell`: home screen, navigation, settings for language and theme, how the app looks and
  which language it speaks.
- `diagnostics`: what the app records about its own behaviour and errors, and how the user can see
  and share it on the phone.

### Modified Capabilities
(none)

## Impact

- Modules: creates `cube` (empty apart from a smoke test) and `app`.
- New dependencies: AndroidX (Compose BOM, Material 3, Navigation, DataStore, AppCompat for
  per-app language), JUnit, Robolectric for JVM Compose tests.
- Repository: Gradle wrapper, CI workflow, docs pages, nfr.md, Claude settings.
