# Proposal

## Why

The app is meant to live on the author's phone, not only run from Android Studio. That needs a
small, optimised, signed release APK that installs and updates in place, without Google Play
(product.md). Third-party licences must be shown in the app (nfr).

## What Changes

- Release build shrunk and optimised with R8 (about 4.5 MB instead of 35 MB), keeping line numbers
  for crash logs.
- Signing: a keystore described in `keystore.properties` (never committed) or, in CI, the
  `RELEASE_*` secrets; without them the release is signed with the debug key.
- Version: build number = commit count (every build updates the previous), name `1.0.<count>-<commit>`.
- GitHub Actions "Release": a `v*` tag builds, tests and attaches the APK to a GitHub release.
- Settings → About: version, what the app does with data, open-source parts and min2phase's MIT
  licence text.
- Docs: how to create the keystore in Android Studio, keep it safe, add the secrets, and install
  the release APK on the Galaxy S24.

## Capabilities

### New Capabilities
- `about`: information about the app and its open-source licences.

### Modified Capabilities
(none)

## Impact

- `app/build.gradle.kts`, `app/proguard-rules.pro`, `.github/workflows/release.yml`, CI full
  history checkout, `AboutScreen`, docs.
