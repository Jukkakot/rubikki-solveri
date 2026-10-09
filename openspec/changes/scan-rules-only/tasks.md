# Tasks

## 1. One scanner in cube

- [ ] 1.1 Record the rules scanner's finish frame per fixture (current behaviour) for the acceptance bar and as a before/after check
- [ ] 1.2 Remove `ScanEngine` and the look path from `VideoScan.kt` (Reading, Group, piles, naming, consensus, rotations, `foundFace`, look constants, `bestTurn`/`agreeing`, look fields in `reset`); `centreLog` unconditional; keep the shared code (design Risks)
- [ ] 1.3 Merge `VideoScanTest` and `RulesVideoScanTest` into one RULES-only class, drop LOOK-only cases; drop the `engine =` argument in `FinderSizeHarness`, `ScanStateCodecTest`, `RulesTimeline`, `RulesScanTest`, `VideoScanHarness`
- [ ] 1.4 `ScanAcceptanceHarness`: stored per-fixture bar (1.1 × 1.2) instead of the look comparison
- [ ] 1.5 Run the cube JVM tests: same finishes on every fixture as in 1.1

## 2. Marks only on followed faces

- [ ] 2.1 `FoundFace.followed` (track size ≥ 2 after this reading), set in `rulesFound`; kept through `ScanStateCodec`
- [ ] 2.2 `ScanPaint.of` skips faces that are not followed
- [ ] 2.3 Tests: codec round trip carries `followed`; paint gives nothing for a one-reading face and veils once it is followed; measure the share of not-followed faces on the fixtures (design Risks)

## 3. Settings, screens, worker

- [ ] 3.1 Remove the scanner section from `SettingsScreen` and its strings (fi, en); `AppActions.scanEngine`/`onScanEngine` from the nav host
- [ ] 3.2 Remove the setting from Android `Settings` and `MainActivity`, from `BrowserStores` and `WebApp`
- [ ] 3.3 `VideoScanScreen`/`ScanLogger`: no engine parameter, no `engine=` in the log; remote reset without engine
- [ ] 3.4 `WebCamera` and `ScanWorker`: `reset` / `adopt:<resets>` without the engine part
- [ ] 3.5 Tests: merge `ScanPaintTest`/`RulesScanPaintTest`; adapt `VideoScanScreenTest`; remove the engine parts of `SettingsTest`; `BrowserStoresTest` checks that old JSON with `scanEngine` still loads
- [ ] 3.6 Build both apps (Android debug, web) and run all unit tests

## 4. Docs

- [ ] 4.1 `docs/architecture.md`: one scanner; rewrite the earlier-scanner paragraph so it keeps only the shared parts (pose, projection hold, `BestCube`, `Tone`, `CLEAR_MARGIN`); the acceptance harness's stored bar
- [ ] 4.2 Roadmap: row 71 `scan-rules-only` done
