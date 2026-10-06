# Tasks

## 1. Exposure controller (cube)

- [x] 1.1 `ExposureControl` per design 1–2 (searching, metering with steps, locked, relock when washed out for 2 s, torch change, point following), replacing `LightSettle`. Verify: unit tests for each transition.

## 2. Cameras (shared, web, app)

- [x] 2.1 `CameraPreview` takes the point, the steps and the lock; the video scan screen feeds them from `ExposureControl` and skips frames while metering. Verify: Compose test (the preview gets the point and the lock); existing video scan screen tests pass.
- [x] 2.2 Android: metering/focus point, exposure-compensation steps, lock (design 3); pictures every 66 ms (design 5). Verify: builds; unit test of the point conversion; phone check listed for the user.
- [x] 2.3 Browser: `pointsOfInterest`, `focusMode`, `exposureCompensation` where supported (design 4), video scan copy every 66 ms. Verify: web build runs in desktop Chromium with a fake camera without errors; phone check listed for the user.
- [x] 2.4 Log: `scan.camera` line on both platforms, snapshot `fps`, `torch`, `darker`, `kind=lock` line (design 6). Verify: `VideoScanLogTest` covers the new fields.

## 3. Browser worker (webworker, web)

- [x] 3.1 `webworker` module: Kotlin/Wasm worker running `FaceFinder` on transferred pictures, faces back as numbers (design 8); copied into the web distribution and precache. Verify: unit test of the faces' encoding round trip (JVM, shared code in `cube`); web build contains the worker.
- [x] 3.2 Page side: pictures as `ImageBitmap` to the worker, newest only, faces back into the video scan; fallback to the page's thread; `scan.worker` line, `worker` in the snapshot (design 8–9). Verify: desktop Chromium with a fake camera: the video scan runs with `worker=true`, no page errors; with the worker blocked it falls back; phone check listed for the user.

## 4. Docs

- [x] 4.1 `docs/architecture.md` video scan camera control and the browser worker (and `development.md` if the build changes); roadmap row `camera-exposure` done.
