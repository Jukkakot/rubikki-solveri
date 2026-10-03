# Design

## Context

No Google Play (product.md): the APK is installed directly. The author is new to Android signing,
so the steps must be click-by-click and the failure modes (lost key) explained.

## Goals / Non-Goals

**Goals:** a small signed APK that updates in place; a repeatable CI release.

**Non-Goals:** app bundles (AAB) and Play signing, automatic updates on the phone.

## Decisions

- **R8 full shrinking** (`isMinifyEnabled`, `isShrinkResources`) with the libraries' own rules;
  our rules only keep line numbers and the navigation route classes. Built and checked in the
  container; runtime behaviour of the minified APK is a phone check.
- **Signing sources in order**: `keystore.properties` (local, git-ignored) → `RELEASE_*`
  environment (CI, the keystore as a base64 secret) → the debug key. The debug fallback keeps CI and
  local release builds working before the key exists, but such APKs cannot update a key-signed
  install (Android checks the signature), so the docs say to use one key from the start.
- **Version**: `versionCode` = commit count (CI checks out full history), so every newer build
  updates an older one; `versionName` `1.0.<count>-<sha>` matches the log's `app.start`.
- **Licence text** shown in the app is a copy of the vendored `LICENSE` in `res/raw`; a test keeps
  them identical.
- **Creating the key and the secrets is the user's step** (an account/security decision): the
  docs give the exact clicks; nothing in the repo depends on it.

## Risks / Trade-offs

- [R8 strips something used by reflection] → libraries ship rules; checked on the phone ("How to
  check"); a crash would show in the log with line numbers.
- [Lost keystore] → the app cannot be updated with a new key; uninstall loses the local history.
  The docs say to back it up.
