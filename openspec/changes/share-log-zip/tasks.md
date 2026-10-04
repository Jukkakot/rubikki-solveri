# Tasks

## 1. Zip fallback

- [x] 1.1 Share the log and the newest 9 pictures (Chromium's 10-file limit); npm `fflate` in `web/build.gradle.kts`; `shareOrDownload`: share → on refusal (not `AbortError`) or no file sharing, download `rubikki-log-<stamp>.zip` (log + pictures, stored); report the first refusal

## 2. Check

- [x] 2.1 `./gradlew :web:wasmJsBrowserDistribution` and the smoke test; the user tries the share button in Samsung Internet and sends the zip
