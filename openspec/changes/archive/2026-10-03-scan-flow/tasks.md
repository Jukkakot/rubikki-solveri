## 1. Scan session (cube)

- [x] 1.1 `ScanSession`: replace `WrongFace` with `PreviousFace` (last accepted face still in view, per-cell Lab distance), add `Holding.centreLooksLike` and `reviewCentreLooksLike`, steadiness by `STEADY_DISTANCE`; verify with `ScanTest` cases `otherCentreIsOnlyAHint`, `previousFaceStillInView`, `warmRed` and the hold/accept tests (`./gradlew :cube:test`)
- [x] 1.2 Remove tap-to-fix from the session (`cycle`, `reviewColors`, corrections, corrections in references and `outcome`); drop or keep `classify`'s `fixed` parameter per design; verify `:cube:test` green with the old tap-to-fix tests removed

## 2. Scan screen (app)

- [x] 2.1 Restore `Progress` and `PermissionScaffold` in `ScanScreen.kt` from `HEAD`; live dots and review tiles draw raw camera colours; review tiles not clickable; status texts for the centre hint and "turn the cube"; review centre note; verify `./gradlew :app:compileDebugKotlin`
- [x] 2.2 Strings fi/en (`scan_review`, `scan_review_centre`, `scan_status_centre`, `scan_status_turn`, `scan_accept`, `scan_retake`; remove `scan_status_wrong`, `scan_tile`); verify lint has no unused-resource errors
- [x] 2.3 Compose tests: `otherCentreIsAHint`, `previousFaceStillInView`, `heldStill`, `scanAgain` with the new texts; review screenshot updated; verify `./gradlew :app:testDebugUnitTest`
- [x] 2.4 Diagnostics log line per face: drop `live`/`fixed`, add `centreLooksLike`; verify by reading the screen code and a passing build

## 3. Home screen version (already built)

- [x] 3.1 Confirm 41e2488 meets the app-shell "Version on the home screen" requirement (version + install time, version alone on failure); add a `ShellTest` check that the version text is shown if missing; verify `:app:testDebugUnitTest`

## 4. Docs and roadmap

- [x] 4.1 `docs/architecture.md` camera-scan pipeline step 3 (hint, previous-face stop, distance steadiness, raw colours, no tap-to-fix); `openspec/context/product.md` input line (no tap to fix during scan; fix in the editor); roadmap row 13 `scan-flow` done; verify by reading the pages
- [x] 4.2 Full check chain (`./gradlew build` or the project's usual check task) green before commit
