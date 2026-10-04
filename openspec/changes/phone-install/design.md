# Design

## Context

- `release` (archived 2026-10-03) set up R8, signing from `keystore.properties` → `RELEASE_*` env →
  debug key, `versionCode` = commit count and a `v*`-tag workflow. No tag has been pushed, so no
  release exists; the repo is public.
- The phone's current install comes from Android Studio's Run ▶ on the user's Windows PC, i.e.
  signed with `%USERPROFILE%\.android\debug.keystore` (store and key password `android`, alias
  `androiddebugkey`). Android only updates an app with an APK signed by the same key; any other key
  means uninstall, which deletes `progress.db` (backup is off).

## Goals / Non-Goals

**Goals:** a newest-build download at one fixed address after every green push; install over the
existing app without losing data; one-time setup the user can do in a few minutes.

**Non-Goals:** an update check or auto-update inside the app (it would need network access, against
"no network" in product.md); Google Play; app bundles.

## Decisions

- **Sign with the user's Android Studio debug key** (not a new release key). It is the only key
  that updates the installed app in place, and Run ▶ builds keep updating the downloaded one.
  Alternatives: a new `rubikki.jks` (the old docs) forces one uninstall and losing the history;
  exporting/importing the history first would be a whole feature. Trade-off: the debug key's
  password is the well-known `android`, so the file itself is the secret; it lives only on the PC
  and in a GitHub secret (repo secrets are not readable by forks or logs). Acceptable for a
  personal app without store distribution. The docs tell the user to back the file up, since
  reinstalling Android Studio on a new PC would otherwise create a different one.
- **Secrets:** only `RELEASE_KEYSTORE_BASE64` is required; `RELEASE_STORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` default in the workflows to `android` /
  `androiddebugkey` / `android` when unset (an existing release-key setup still works by setting
  them).
- **Rolling release with the fixed tag `latest-build`:** a `publish` job in `ci.yml`, after the
  `check` job, on `push` to `main` only. It builds `assembleRelease` (tests and lint already ran in
  `check`), copies the APK to `rubikki-solveri.apk`, then with the runner's `gh`:
  `gh release delete latest-build --cleanup-tag --yes || true` and
  `gh release create latest-build rubikki-solveri.apk --target $GITHUB_SHA --latest
  --title "Rubikki Solveri <versionName>" --notes "<commit subject>"`. Recreating instead of editing
  moves the tag to the new commit and keeps exactly one rolling release. GitHub's
  `/releases/latest/download/<asset>` redirect then always serves the newest file.
  `permissions: contents: write` on that job only; `concurrency` group `publish` with
  `cancel-in-progress: true` so a newer push wins. The version name is read from Gradle
  (`./gradlew -q :app:printVersionName`, a tiny task added to `app/build.gradle.kts`) so the title
  matches Settings → About.
  Alternatives: a release per commit (clutters the releases page); Actions artifacts (expire,
  need a GitHub login and unzip on the phone); GitHub Pages hosting of the APK (fine, but the
  release page also shows the version and notes for free).
- **Skip without the key:** the job's first step checks the secret; if empty it writes a line to
  `$GITHUB_STEP_SUMMARY` ("Publishing skipped: add the RELEASE_KEYSTORE_BASE64 secret, see
  docs/operations.md") and ends successfully, so CI stays green before setup.
- **Tagged releases (`release.yml`)** keep working and also attach `rubikki-solveri.apk`; they use
  the same password defaults. A `v*` release created after the rolling one becomes "latest"
  until the next push, and it serves the same fixed asset name, so the address never breaks.
- **About button** uses Compose's `LocalUriHandler.openUri(DOWNLOAD_URL)` (no Android intent code,
  so it also works unchanged in the planned browser version). The address is one constant
  `AppLinks.LATEST_APK` in the app. Label: "Lataa uusin versio" / "Download the latest version",
  as a tonal button above the licences button.
- **No `INTERNET` permission** is added: the browser does the download and Android's package
  installer does the install.

## Risks / Trade-offs

- [The user's PC key differs from the one that signed the phone's current install, e.g. Studio was
  reinstalled] → the docs' first-install step says: if Android says "App not installed" /
  "conflicts with an existing package", the keys differ; then either install from that PC with Run
  ▶ once more after setting up the secret from the same PC, or accept one uninstall.
- [Local commits not yet pushed give Run ▶ a higher version code than the download] → Android
  refuses the older download ("downgrade"); the docs say to push first or just use Run ▶.
- [Play Protect warns about an unknown developer] → docs: "More details → Install anyway".
- [Debug-key-signed release is `debuggable=false` but uses a debug certificate] → fine for
  sideloading; would have to change before any store release (product.md already says so for the
  name).

## Migration Plan

User steps (click-by-click in `docs/operations.md`), once: (1) PowerShell one-liner turns
`%USERPROFILE%\.android\debug.keystore` into base64 on the clipboard; (2) GitHub → Settings →
Secrets and variables → Actions → New secret `RELEASE_KEYSTORE_BASE64`; (3) back up the keystore
file; (4) push anything (or re-run the latest CI) → the release appears; (5) on the phone open the
address in Chrome, allow Chrome to install apps, Update; (6) optional: Chrome ⋮ → Add to home
screen for a one-tap "update" icon. Rollback: delete the secret; publishing stops, the last release
stays.
