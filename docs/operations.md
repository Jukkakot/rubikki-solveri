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

The scan logs numbers, never pictures:

- `scan.face face=F rgb=…,…` — the nine captured readings of a face (hex RGB, row by row as seen),
  and `live=` the quick per-cell reading.
- `scan.done valid=… validity=… uncertain=N cube=…` — the result (`cube` is the 54 colour letters,
  `.` for unknown).

If the scan misreads colours: scan once, Settings → Log → Share, and give the log to Claude. The
readings are enough to replay the classification in a unit test and adjust
`ColorClassifier.DEFAULT_PALETTE` (live dots and the centre check) or `UNCERTAIN_BELOW`.

## Data on the phone — Implemented

- Settings: DataStore `settings` (theme, notation). Language: the system's per-app language.
- Solves and practice: Room database `progress.db` in the app's private storage. Uninstalling the
  app deletes it; Android backup is off (`allowBackup=false`), so it is not copied anywhere.
- Schema changes need a Room migration (schemas are exported to `app/schemas`).

## Release — Planned (`release`)

A signed release APK installed directly on the phone; no Google Play.
