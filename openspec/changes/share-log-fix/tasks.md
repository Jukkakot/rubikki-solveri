# Tasks

## 1. Fallback

- [x] 1.1 `shareOrDownload` in `platform.mjs`: share with pictures → on refusal (not `AbortError`) the log alone → on refusal download; report the outcome and the error through a callback
- [x] 1.2 `Js.kt` / `Share.kt` / `WebApp.kt`: pass the callback, log `log.shared` with `outcome` and `error`

## 2. Check

- [x] 2.1 `./gradlew :web:wasmJsBrowserDistribution` and the smoke test; the user tries the share button in Samsung Internet
