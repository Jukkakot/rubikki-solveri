# Proposal

## Why

Today the app reaches the phone only through Android Studio's Run ▶ with the phone on a cable or
wireless debugging. The `release` change built the pieces for a standalone APK, but nothing has
been published yet (no GitHub release exists), and its plan needs a new signing key, which would
force uninstalling the Run ▶-installed app and lose the solve history. The user wants a simple,
repeatable way to get the latest app onto the phone without a computer.

## What Changes

- Every push to `main` that passes CI publishes a signed release APK to one rolling GitHub release
  ("Uusin versio"), always at the same address:
  `https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk`.
- The signing key is the user's existing Android Studio debug key (the one Run ▶ already uses), so
  the downloaded APK installs as an update over the current app and keeps its history; Run ▶ and
  the download keep updating each other. The key goes to GitHub as one secret.
- Without that secret CI does not publish (an APK signed with a throw-away key could not update the
  installed app); it says so in the run summary.
- Settings → About gets a "Lataa uusin versio" button that opens the address in the phone's browser.
- The tagged `v*` release workflow attaches the same fixed-name APK.
- **Sharing with others, free:** the repository's front page gets a "Lataa / Download" section
  with the fixed link, a QR code and short install steps in Finnish and English, including
  Obtainium (a free app that installs and auto-updates apps straight from GitHub releases). The
  release notes carry the same steps. `docs/distribution.md` compares the free routes (GitHub
  link, Obtainium, the browser version, F-Droid, Samsung Galaxy Store) and what each would need.
- Docs: click-by-click setup (secret from the debug key, back it up), first install and every later
  update on the Galaxy S24 (allow installs from Chrome, Play Protect prompt, home-screen shortcut).

Touches the `app` module (About screen) and CI; not `cube`.

## Capabilities

### New Capabilities
- `app-delivery`: how a new build reaches the phone — the rolling release at a fixed address, the
  signing that lets it update the installed app, and when publishing is skipped.

### Modified Capabilities
- `about`: the about screen gains a button that opens the download of the latest version.

## Impact

- `.github/workflows/ci.yml` (new `publish` job), `.github/workflows/release.yml` (default debug-key
  passwords, fixed-name asset), `app/build.gradle.kts` unchanged (local release builds already fall
  back to the local debug key), `AboutScreen` + strings, `docs/operations.md`, `README.md`,
  `openspec/context/roadmap.md`.
- One GitHub secret (`RELEASE_KEYSTORE_BASE64`) added by the user; no money, no new accounts.
