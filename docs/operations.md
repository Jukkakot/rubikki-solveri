# Operations

There is no server: "operations" means getting the app onto the phone and finding out what it did.

## Builds — Implemented

- Every push to `main` runs CI (GitHub Actions → CI): tests, lint, debug APK. The APK is attached
  to the run as the artifact `rubikki-solveri-debug-<commit>` for 7 days.
- The `publish` job (a signed release APK as the release "latest-build", see Release below) is
  **disabled for now** (`if: false`, user 2026-10-04); it also needs the signing key secret.
- The version name is `1.0.<commit count>-<short commit>`; the log's `app.start` line and
  Settings → About show it.
- CI also builds the browser version and runs its smoke test (`web/smoke/smoke.mjs`: the page
  starts without errors, the solver's speed via `?selftest`); on a push to `main` its `pages` job
  publishes that same build (see Browser version). `pages.yml` is only a manual rebuild (Run
  workflow). Pages is set to "GitHub Actions" as its source (done 2026-10-04 with
  `gh api -X POST repos/Jukkakot/rubikki-solveri/pages -f build_type=workflow`).
- Pushes that only change `openspec/`, `docs/` or `*.md` run no workflow (`paths-ignore`, decided
  2026-10-04: over half of the pushes were spec and doc commits).

## Installing on the phone — Implemented

Three ways:

1. **The download link** (everyday): open
   https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk on the
   phone, or in the app Settings → About → **Lataa uusin versio**. Steps below under Release → 4.
2. **From Android Studio** (during development): phone connected over Wi-Fi (wireless
   debugging), pick the phone in the device menu, press Run ▶ (steps below). Both ways use the same key, so they update each other.
3. **From a CI artifact:** download the debug APK zip from a CI run, unzip, copy it to the phone.
   It is signed with that runner's own debug key, so it does **not** install over the others
   without an uninstall (which deletes the history) — avoid.

### Android Studio and the Galaxy S24 over Wi-Fi

Assumes Android Studio is installed and the phone is already paired with it over Wi-Fi (wireless
debugging).

1. Start Android Studio and open the project (welcome screen → **rubikki-solveri**, or
   **File → Open Recent**). Wait until the Gradle sync at the bottom finishes.
2. Get the newest code: **Git → Pull…** → **Pull** (or the blue down arrow in the toolbar).
3. Phone: same Wi-Fi as the computer, screen unlocked. If the device menu in the toolbar does not
   show "Samsung SM-S921…": on the phone **Settings → Developer options → Wireless debugging** on
   (it switches off by itself after a while or on another network), then in Android Studio's
   device menu pick the phone again (or **Pair Devices Using Wi-Fi** and scan the QR code if it
   asks to pair anew).
4. The run configuration next to the device menu says **app**. Press the green **Run ▶**. The app
   installs as an update (the history stays) and opens on the phone.
5. Later updates: steps 2–4 again.

If Wi-Fi gives trouble, a USB-C cable also works: **Developer options → USB debugging** on, plug
in, allow "Allow USB debugging?" on the phone, then Run ▶.

## Logs and debugging on the phone — Implemented

- Settings → Troubleshooting → Log shows the newest lines first, with the phone's local time
  (only the time for today) and errors in red, warnings in amber; Share sends the file (e.g. to
  yourself by email, or paste into a Claude session); Clear empties it. The shared file keeps the
  UTC ISO times.
- The file keeps about the last 512 kB.
- With the phone connected, Android Studio's Logcat shows the same lines live (`tag:Rubikki`).

## Tuning the camera scan — Implemented

The scan logs the readings and keeps a small picture of the grid for every capture:

- `scan.capture face=F picture=20261003-104512-123-F.png rgb=…,…` — every capture (automatic or
  the button, retakes too): the face the centre was recognised as, the picture's file name and
  the nine readings as seen (hex RGB, row by row).
- `scan.face face=F rgb=…,… recognised=F` — an accepted face, provisionally named by its centre
  (the final names are decided at the end, see `renamed`).
- `scan.lock lock=true` — exposure and white balance locked when the first face is captured
  (`lock=false` when no face is done again).
- `scan.stall where=camera|ui ms=…` — the camera frames or the screen stopped for that long; a
  freeze the user saw should show up here.
- `scan.done valid=… validity=… uncertain=N cube=… rotations=U1R0F3D2L0B0 renamed=RL` — the result
  (`cube` is the 54 colour letters, `.` for unknown; `rotations` the quarter turns clockwise each
  face's capture was turned; `renamed` the faces whose capture ended up elsewhere at the end, e.g. `B>U,U>B`).
- `scan.check verdict=solvable|impossible validity=… faces=R,U marked=… cube=…` — the check's
  verdict after the last "Looks right" (`faces` to look at again, `marked` the likely misreads);
  `scan.check rescan=U rotation=k colors=…` — a face rescanned on its own replaced that face.

Pictures (120×120 PNG of the grid area) live in `files/logs/scan/` on the phone, newest 12 kept;
Clear in the log screen deletes them. They leave the phone only when the log is shared.

If the scan misbehaves: scan once, Settings → Log → Share → **Drive** (the log and the pictures go
together), then tell Claude; Claude fetches them with the Google Drive connector. The readings
replay in a unit test to tune `ColorClassifier`, the "looks like a cube" thresholds
(`FrameSampler.MIN_GAP_CONTRAST`, `MIN_STICKER_CELLS`, `MIN_COLOUR_CELLS`, `MAX_STICKER_SPREAD`) or `STEADY_DISTANCE`.

With the phone on wireless debugging Claude can also pull them directly:
`adb exec-out run-as fi.jukkakot.rubikkisolveri cat files/logs/scan/<name>.png > <name>.png`
(debug builds only). Pictures worth keeping go to `cube/src/test/resources/scan/` as test fixtures.

## Data on the phone — Implemented

- Settings: DataStore `settings` (theme, notation). Language: the system's per-app language.
- Solves and practice: Room database `progress.db` in the app's private storage. Uninstalling the
  app deletes it; Android backup is off (`allowBackup=false`), so it is not copied anywhere.
- Schema changes need a Room migration (schemas are exported to `app/schemas`).

## Browser version — Implemented

- Address: https://jukkakot.github.io/rubikki-solveri/ (published by the Pages workflow, see
  Builds). Same features as the phone app; Karkki colours instead of Material You.
- **Add to the S24's home screen (Chrome):** open the address → ⋮ (top right) → **Add to home
  screen** → **Install** (Finnish Chrome: **Lisää aloitusnäytölle** → **Asenna**). The icon
  "Rubikki" opens the app full screen. On an iPhone (Safari): Share → **Add to Home Screen**.
- **Offline:** after one complete visit the app opens without a network (service worker
  `sw.js`, files listed in `precache.json`). A new build is used at the latest on the second
  start after it was published; the home screen's version line ("Koottu …") tells which build runs.
- **Data** stays in that browser (localStorage keys `rubikki.*`): settings, solves, practice,
  the log and the newest 12 scan pictures. It is separate from the phone app and from other
  browsers. Clearing the site's data (Chrome: ⋮ → Settings → Site settings → All sites →
  jukkakot.github.io → **Delete data**) deletes it.
- **Logs:** Settings → Log → share button: on the phone the share sheet with the log and the scan
  pictures; on a computer the log is downloaded as `rubikki-log-<time>.txt`. The browser console
  shows the same lines.
- **Speed check:** open the address with `?selftest` at the end: the console prints
  `SELFTEST ok warmup=… solve=…` (milliseconds) and the app log keeps the line.
- Locally: `./gradlew :web:wasmJsBrowserDistribution`, then
  `node web/smoke/serve.mjs web/build/dist/wasmJs/productionExecutable` and open
  http://127.0.0.1:8080/ (camera needs `localhost`/`127.0.0.1` or https).

## Release — Implemented

The release APK is shrunk with R8 (about 4.5 MB). Versions: build number = number of commits, name
`1.0.<count>-<commit>` (Settings → About, the log's `app.start`). Android only installs an update
over an app signed with the **same key**. The app's key is your **Android Studio debug key**
(`C:\Users\<you>\.android\debug.keystore`, created by Android Studio, passwords `android`): it
already signed the app Run ▶ put on the phone, so the downloaded APK updates that install and keeps
its history. Local release builds without `keystore.properties` use the same key automatically.

### 1. Once: give the key to GitHub

1. On the PC open **PowerShell** (Start → type `powershell` → Enter) and paste:
   ```
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore")) | Set-Clipboard
   ```
   Nothing is printed; the key is now on the clipboard as text.
2. In the browser: https://github.com/Jukkakot/rubikki-solveri → **Settings** (top bar) →
   left menu **Secrets and variables → Actions** → **New repository secret**.
   Name: `RELEASE_KEYSTORE_BASE64`, Secret: paste (Ctrl+V) → **Add secret**.
3. **Back up the key file**: copy `C:\Users\<you>\.android\debug.keystore` to a USB stick or
   a cloud drive. A new PC or a reinstalled Android Studio makes a different key; then copy this
   file back to the same place before pressing Run ▶, or updates stop working.
4. Start a build: **Actions** → **CI** → the newest run → **Re-run all jobs** (or push anything).
   When it is green, **Code** page → right side **Releases** shows "Rubikki Solveri 1.0.…".

A dedicated key instead (e.g. for a store later): set `RELEASE_KEYSTORE_BASE64` to that key file
and add `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`; every existing
install then needs one uninstall. See [distribution.md](distribution.md).

### 2. Tagged versions (optional)

A tag like `v1.0` (Android Studio → **Git → New Tag…**, then push with tags) runs the **Release**
workflow, which makes a permanent release "v1.0" with the same APK. The fixed download link
serves whichever release is newest.

### 3. Build a release APK on the computer (optional)

Android Studio → **Build → Select Build Variant…** → *app* = **release**, then Run ▶ (installs on
the phone) or **Build → Build App Bundle(s) / APK(s) → Build APK(s)** (file in
`app/build/outputs/apk/release/`).

### 4. Install or update on the Galaxy S24

1. On the phone open the link above in **Chrome** (or Settings → About → **Lataa uusin versio** in
   the app). Chrome may ask "Download file anyway?" → **Download**.
2. Tap **Open** in the download notice (or **My Files → Downloads → rubikki-solveri.apk**).
3. First time only: "For your security, your phone is not allowed to install unknown apps from this
   source" → **Settings** → turn on **Allow from this source** → back.
4. **Update** (or **Install** on a phone without the app). If Play Protect shows "Unsafe app
   blocked" or asks to scan: **More details → Install anyway**.
5. Optional, for one-tap updates: in Chrome open the link page, **⋮ → Add to home screen**.

If it says **"App not installed"**: either the phone's app was signed with another key (e.g. Run ▶
from a different PC — install the key file there, see 1.3) or the download is older than what is
installed (Run ▶ from unpushed commits — push first, or keep using Run ▶).

For other people (QR code, Obtainium automatic updates): the front page `README.md` and
[distribution.md](distribution.md).
