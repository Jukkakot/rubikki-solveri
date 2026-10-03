## 1. Layout

- [ ] 1.1 `ScanContent`: actions and manual link in the Scaffold bottom bar; no vertical scroll; title row with face progress; camera box fills the remaining height at 3:4; status + progress panel on the camera's bottom edge; "X read" chip on the camera; verify `./gradlew :app:compileDebugKotlin`
- [ ] 1.2 Review overlay: short `scan_review` line, `scan_review_note`, centre note under the tiles; strings fi/en; verify lint
- [ ] 1.3 Tests: scan Compose tests pass with the new layout; review screenshot at phone size shows the buttons; verify `./gradlew :app:testDebugUnitTest` (AppTest.shareTheLog fails locally on Windows only, CI green)

## 2. Docs

- [ ] 2.1 `docs/architecture.md` scan route line mentions the one-screen layout; roadmap row 14 `scan-layout` done; verify by reading
