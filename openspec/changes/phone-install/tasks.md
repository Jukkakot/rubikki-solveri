# Tasks

## 1. Publishing from CI

- [x] 1.1 `app/build.gradle.kts`: add a `printVersionName` task that prints `versionName` (configuration-cache safe: a provider captured in a plain `doLast`). Verify: `./gradlew -q :app:printVersionName` prints `1.0.<count>-<sha>`
- [x] 1.2 `.github/workflows/ci.yml`: add the `publish` job (needs `check`; only `push` on `refs/heads/main`; `permissions: contents: write`; `concurrency: publish` cancel-in-progress; full-history checkout, JDK 21, gradle setup): skip-with-summary when `RELEASE_KEYSTORE_BASE64` is empty; otherwise decode the keystore to `$RUNNER_TEMP`, export `RELEASE_STORE_FILE` and the three passwords with the `android`/`androiddebugkey`/`android` defaults, `./gradlew assembleRelease`, copy to `rubikki-solveri.apk`, delete and recreate the `latest-build` release with `gh` (`GH_TOKEN: ${{ github.token }}`), title from `printVersionName`, notes from `git log -1 --format=%s`. Verify: `actionlint` (download the binary to the scratchpad) reports no errors on both workflows, and the job's shell is checked by running its script locally with `gh` stubbed (prints the intended commands, skip path when the secret is empty)
- [x] 1.3 `.github/workflows/release.yml`: same password defaults; also copy the APK to `rubikki-solveri.apk` and attach both files. Verify: `actionlint` clean

## 2. About screen

- [x] 2.1 Add `AppLinks.LATEST_APK` (the fixed address) and the "download the latest version" tonal button on `AboutScreen` above the licences button, opening the address with `LocalUriHandler`; strings `about_download_latest` in Finnish ("Lataa uusin versio") and English. Verify: a new Compose test `AboutTest` taps the button with a fake `LocalUriHandler` and asserts the exact URL; `StringsTest` passes; the about screenshot still fits without scrolling
- [x] 2.2 Run `./gradlew test lint assembleDebug assembleRelease`. Verify: all green

## 3. Sharing with others

- [x] 3.1 `scripts/download-qr.py` → committed `docs/img/download-qr.png` for the fixed address. Verify: decoding the PNG (e.g. `pip install pyzbar` or opencv `QRCodeDetector`) gives exactly the address
- [x] 3.2 Release notes text: the publish job writes the commit subject plus the short Finnish/English install and Obtainium steps (kept in `.github/release-notes.md`, the subject prepended). Verify: `actionlint` clean; running the notes step locally prints the expected body
- [x] 3.3 `README.md`: "Lataa / Download" section near the top (link, QR image, fi + en steps, Obtainium); `docs/distribution.md` with the routes table and what each later route needs; linked from `docs/README.md`. Verify: all links and the image path resolve

## 4. Docs and roadmap

- [x] 4.1 `docs/operations.md`: rewrite "Release" and "Installing on the phone": the fixed address first; one-time setup (PowerShell `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore")) | Set-Clipboard`, the GitHub secret clicks, back up the keystore); first install on the Galaxy S24 (Chrome download → open → "Allow from this source" for Chrome → Update; Play Protect "More details → Install anyway"); later updates (About → "Lataa uusin versio", or the home-screen shortcut); troubleshooting ("App not installed" = different key or older version). Remove the "create a new rubikki.jks" path (keep a short note that a dedicated key is possible via the four secrets). Verify: every step names the exact menu labels; links resolve
- [x] 4.2 `docs/architecture.md` "Release build": the rolling release and the signing decision. Mark roadmap item 26 `phone-install` done. Verify: `openspec validate phone-install` passes
