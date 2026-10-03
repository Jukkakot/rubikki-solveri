## 1. Cube module

- [ ] 1.1 `ScanSession`: plausibility check (chroma ≥ 20 or L ≥ 55, at most one cell off) and `ScanEvent.NoCube` before the streak; `captureNow` unaffected; tests: dark pad and grey desk → `NoCube`, no capture; scrambled and solved-white faces still captured; capture button captures anyway (`./gradlew :cube:test`)
- [ ] 1.2 `FrameSampler.picture(frame, size)`: upright ARGB of the grid square for each rotation; test with a synthetic frame that the cell colours land in the right picture corners (`:cube:test`)

## 2. App

- [ ] 2.1 `CameraPreview` keeps the latest grid picture; `ScanContent` gets a `snapshot` provider; on every capture save the PNG (newest 12) and log `scan.capture` with picture name and readings; status text for `NoCube` (fi/en); verify `:app:compileDebugKotlin`
- [ ] 2.2 Log share sends log + pictures (`ACTION_SEND_MULTIPLE` when pictures exist); clear deletes them; Compose/unit tests: `NoCube` status shown, share intent lists the pictures (`:app:testDebugUnitTest`; AppTest.shareTheLog is known to fail locally on Windows only)

## 3. Docs

- [ ] 3.1 `docs/operations.md` "Tuning the camera scan": pictures, `scan.capture`, share to Google Drive for Claude; `docs/architecture.md` scan step 3 mentions the cube check; roadmap row 15 `scan-cube-check` done; verify by reading
