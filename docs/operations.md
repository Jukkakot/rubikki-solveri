# Operations

There is no server: "operations" means getting the app onto the phone and finding out what it did.

## Builds — Implemented

- Every push to `main` runs CI (GitHub Actions → CI): tests, lint, debug APK. The APK is attached
  to the run as the artifact `rubikki-solveri-debug-<commit>` for 7 days.
- The version name is `0.1.0-<short commit>`; the log's `app.start` line shows it.

## Installing on the phone — Implemented

Two ways:

1. **From Android Studio** (normal during development): phone connected with USB debugging on,
   pick the phone in the device menu, press Run ▶.
2. **From a CI artifact:** download the APK zip from the CI run, unzip, copy the APK to the phone,
   open it and allow "install unknown apps" for the file manager. A debug APK from CI is signed
   with that runner's debug key, so installing it over a Studio-installed build needs an
   uninstall first.
3. **A signed release** for everyday use: see Release below.

### First time: Android Studio on Windows and the Galaxy S24

1. Download Android Studio from https://developer.android.com/studio → "Download Android Studio",
   accept the terms, run the installer with the defaults (keep "Android Virtual Device" ticked; it
   does no harm).
2. Start Android Studio → the setup wizard → **Standard** → accept every licence (click each
   licence on the left, then "Accept") → Finish. It downloads the SDK (a few GB).
3. Get the code: on the welcome screen **Clone Repository** → URL
   `https://github.com/Jukkakot/rubikki-solveri.git` → pick a folder → Clone → "Trust Project".
   Wait until the Gradle sync at the bottom finishes (first time several minutes).
4. Phone, once: **Settings → About phone → Software information**, tap **Build number** seven
   times (enter your PIN) → "Developer mode has been turned on".
5. Phone: **Settings → Developer options** → turn on **USB debugging**.
6. Connect the phone with a USB-C cable. On the phone allow "Allow USB debugging?" (tick "Always
   allow from this computer"). If Windows asks for a driver, install Samsung's USB driver from
   https://developer.samsung.com/android-usb-driver.
7. In Android Studio's toolbar the device menu now shows "Samsung SM-S921…"; the run
   configuration next to it says **app**. Press the green **Run ▶**. The app opens on the phone.
8. Later updates: **Git → Pull** (or the blue arrow), then Run ▶ again.

Wireless instead of the cable (optional): Developer options → **Wireless debugging** on, then in
Android Studio device menu → **Pair Devices Using Wi-Fi** and scan the QR code with the phone.

## Logs and debugging on the phone — Implemented

- Settings → Troubleshooting → Log shows the newest lines first; Share sends the file (e.g. to
  yourself by email, or paste into a Claude session); Clear empties it.
- The file keeps about the last 512 kB.
- With the phone connected, Android Studio's Logcat shows the same lines live (`tag:Rubikki`).

## Tuning the camera scan — Implemented

The scan logs the readings and keeps a small picture of the grid for every capture:

- `scan.capture face=F picture=20261003-104512-123-F.png rgb=…,…` — every capture (automatic or
  the button, retakes too): the picture's file name and the nine readings (hex RGB, row by row).
- `scan.face face=F rgb=…,… centreLooksLike=R` — an accepted face; `centreLooksLike` when the
  centre read as another colour.
- `scan.lock lock=true` — exposure and white balance locked after the first face.
- `scan.done valid=… validity=… uncertain=N cube=…` — the result (`cube` is the 54 colour letters,
  `.` for unknown).

Pictures (120×120 PNG of the grid area) live in `files/logs/scan/` on the phone, newest 12 kept;
Clear in the log screen deletes them. They leave the phone only when the log is shared.

If the scan misbehaves: scan once, Settings → Log → Share → **Drive** (the log and the pictures go
together), then tell Claude; Claude fetches them with the Google Drive connector. The readings
replay in a unit test to tune `ColorClassifier`, the "looks like a cube" thresholds
(`ScanSession.MIN_CHROMA`, `MIN_WHITE_LIGHTNESS`) or `STEADY_DISTANCE`.

## Data on the phone — Implemented

- Settings: DataStore `settings` (theme, notation). Language: the system's per-app language.
- Solves and practice: Room database `progress.db` in the app's private storage. Uninstalling the
  app deletes it; Android backup is off (`allowBackup=false`), so it is not copied anywhere.
- Schema changes need a Room migration (schemas are exported to `app/schemas`).

## Release — Implemented

The release APK is shrunk with R8 (about 4.5 MB) and signed with your own key. Versions: build
number = number of commits, name `1.0.<count>-<commit>` (shown in Settings → About and in the
log's `app.start`). Android only installs an update over an app signed with the **same key**, so
create the key once and keep it.

### 1. Create the signing key (once, in Android Studio)

1. Android Studio → **Build → Generate Signed App Bundle or APK…** → choose **APK** → Next.
2. Module **app**. Under *Key store path* click **Create new…**.
3. Key store path: a folder outside the project, e.g. `C:\Users\<you>\keys\rubikki.jks`.
   Choose a password and confirm it. Alias: `rubikki`, its password (may be the same), validity
   25 years, your name under *First and Last Name*. **OK**.
4. Back in the dialog press **Cancel** (the build below uses the Gradle setup instead).
5. **Back up `rubikki.jks` and the passwords** (e.g. a password manager and a USB stick). If the
   key is lost, updates are impossible: the app must be uninstalled, which deletes its history.

### 2. Build a release APK on your computer

1. In the project folder (next to `settings.gradle.kts`) create `keystore.properties`
   (it is git-ignored, never commit it):
   ```
   storeFile=C:/Users/<you>/keys/rubikki.jks
   storePassword=<key store password>
   keyAlias=rubikki
   keyPassword=<key password>
   ```
2. Android Studio → **Build → Select Build Variant…** → set *app* to **release**, then
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The APK is in
   `app/build/outputs/apk/release/app-release.apk` (the "locate" link in the pop-up opens it).
   Alternatively Run ▶ with the release variant installs it on the connected phone directly.

### 3. Optional: releases from GitHub

1. GitHub → the repo → **Settings → Secrets and variables → Actions → New repository secret**,
   four secrets:
   - `RELEASE_KEYSTORE_BASE64`: the key file as base64 — in PowerShell
     `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Users\<you>\keys\rubikki.jks")) | Set-Clipboard`,
     then paste.
   - `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` (`rubikki`), `RELEASE_KEY_PASSWORD`.
2. Tag a version: Android Studio → **Git → New Tag…** → `v1.0` → push with tags (or ask Claude to
   do it). The **Release** workflow builds, tests and attaches `rubikki-solveri-v1.0.apk` to a
   GitHub release. Without the secrets it still runs, but signs with a throw-away debug key.

### 4. Install the APK on the Galaxy S24

1. Copy the APK to the phone (USB cable → *Phone/Download*, or download it on the phone from the
   GitHub release).
2. On the phone open **My Files → Downloads** and tap the APK. The first time Android asks to
   allow installs from that app: **Settings → Allow from this source** → back → **Install**.
3. A build signed with a different key than the installed one does not install over it ("App not
   installed"): uninstall the old one first (this deletes its history) — or always use the same key.
