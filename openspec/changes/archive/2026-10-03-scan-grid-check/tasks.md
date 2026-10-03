## 1. Cube module

- [x] 1.1 Copy the seven phone pictures into `cube/src/test/resources/scan/`; `FrameSampler.gapContrast(picture, size)` and `looksLikeCube(picture, size)` (≥ 15 in ≥ 6 cells); test: desk picture rejected, the six cube faces accepted (`./gradlew :cube:test`)
- [x] 1.2 `ScanSession.onFrame(..., looksLikeCube: Boolean = true)` replaces the colour rule (remove `MIN_CHROMA`, `MIN_WHITE_LIGHTNESS`, `looksLikeCube(labs)`); update the `noCubeInTheGrid` / `cubeFacesLookLikeACube` tests; verify `:cube:test`
- [x] 1.3 Regression test: the six `scan.face` readings from the 2026-10-03 08:09 phone scan classify to `WWWWWWWWWRRRRRRYRYGOGGGGGYBRGRBYYBYGOGOYOOYOYBBBBBOOBO`, valid, 0 uncertain; verify `:cube:test`

## 2. App

- [x] 2.1 `CameraPreview` delivers the picture before the readings; `ScanScreen` passes `looksLikeCube` from the latest picture; `ScanContent` takes `looksLikeCube: () -> Boolean`; Compose test `noCubeInTheGrid` drives it; verify `:app:testDebugUnitTest` (AppTest.shareTheLog known to fail locally only) and lint

## 3. Docs

- [x] 3.1 `docs/architecture.md` scan step 3 and `docs/operations.md` thresholds (`FrameSampler` gap contrast instead of `MIN_CHROMA`/`MIN_WHITE_LIGHTNESS`; pictures can be pulled with adb `run-as`); roadmap row 16; verify by reading
