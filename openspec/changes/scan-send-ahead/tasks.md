# Tasks

## 1. Timings first (web, shared)

- [ ] 1.1 `platform.mjs`: per picture `prepMs` (bitmap asked → ready) and `idleMs` (finder's previous answer → this post) passed to `onFaces`; `WebCamera.kt` puts them in `FoundFaces`; verify: `node web/smoke/video.mjs` prints snapshots with both fields
- [ ] 1.2 `VideoScanScreen` / `VideoScanLog`: snapshot averages `prepMs`, `idleMs` (null on the phone and the page fallback); verify: a `VideoScanLogTest` case with and without them, `./gradlew :app:testDebugUnitTest` (or the shared tests) green
- [ ] 1.3 Baseline: desktop Chromium trace of the current build with `web_181940` (fps, finder idle, prepMs) into `design.md` Findings

## 2. Send ahead (web)

- [ ] 2.1 `platform.mjs`: crop set from Kotlin (`scanWorkerCrop`), sending triggered by the frame watcher and by the finder's answer; one picture made or waiting at most; a camera frame sent once; pool 6; verify: the smoke test finishes the scan with the worker and with `--no-worker`
- [ ] 2.2 Fallback without `requestVideoFrameCallback`: Kotlin loop sends each animation frame when nothing is being made or waits, guarded by `currentTime`; verify: smoke run with the watcher disabled in an init script still finishes
- [ ] 2.3 Same trace after: pictures a second ≤ camera rate, finder idle down, page animation frame work unchanged; numbers into Findings; `./gradlew :web:wasmJsBrowserDistribution` and the project checks green

## 3. Docs and roadmap

- [ ] 3.1 `docs/architecture.md` worker paragraph (send ahead, frame read once, `prepMs`/`idleMs`); roadmap item 79 `scan-send-ahead` done with the phone check (fps near 30, `idleMs` small, what `prepMs` shows)
