# Tasks

## 1. Exposure controller (cube)

- [ ] 1.1 `ExposureControl` per design 1–2 (searching, metering with steps, locked, relock when washed out for 2 s, torch change, point following), replacing `LightSettle`. Verify: unit tests for each transition.

## 2. Cameras (shared, web, app)

- [ ] 2.1 `CameraPreview` takes the point, the steps and the lock; the video scan screen feeds them from `ExposureControl` and skips frames while metering. Verify: Compose test (the preview gets the point and the lock); existing video scan screen tests pass.
- [ ] 2.2 Android: metering/focus point, exposure-compensation steps, lock (design 3); pictures every 66 ms (design 5). Verify: builds; unit test of the point conversion; phone check listed for the user.
- [ ] 2.3 Browser: `pointsOfInterest`, `focusMode`, `exposureCompensation` where supported (design 4), video scan copy every 66 ms. Verify: web build runs in desktop Chromium with a fake camera without errors; phone check listed for the user.
- [ ] 2.4 Log: `scan.camera` line on both platforms, snapshot `fps`, `torch`, `darker`, `kind=lock` line (design 6). Verify: `VideoScanLogTest` covers the new fields.

## 3. Docs

- [ ] 3.1 `docs/architecture.md` video scan camera control; roadmap row `camera-exposure` done and `web-scan-worker` planned.
