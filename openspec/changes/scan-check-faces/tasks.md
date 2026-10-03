## 1. Cube module

- [x] 1.1 `ScanOutcome.samples` (54 raw readings) and `ScanSession(only = FaceView)` one-face mode (starts at that face, accept ends it, no previous-face check, redo does nothing); unit tests for both (`./gradlew :cube:test`)
- [x] 1.2 `MisreadSearch.swaps(cube, readings?)`: valid single swaps ranked by reading cost; tests: a solved/scrambled cube with two stickers swapped finds that swap first, a twisted corner gives none, ranking follows the readings (`:cube:test`)
- [x] 1.3 `ColorClassifier.classifyFace` against the other 45 labelled readings with the centre offset; tests: the same face read again under a uniform brightness shift gets its colours back, a doubtful sticker gets low confidence (`:cube:test`)
- [x] 1.4 `ScanCheck` state: initial checked faces from the marks, `lookRight`, `paint` (clears that face's marks, keeps checked), `replaceFace`, `verdict()` (valid / suspect faces + marked swap / fallbacks), `encode`/`decode`; tests for each spec scenario of the face-by-face check and the verdict (`:cube:test`)

## 2. App

- [x] 2.1 `LastScan` (pictures + readings) replaces `LastScanPictures`; `ScanRoute(face)` one-face mode in `ScanContent` (one progress dot, redo hidden) returning face + readings through the previous entry's saved state; Compose test: one-face scan accepts and calls back once (`:app:testDebugUnitTest`)
- [x] 2.2 Check mode in `ManualInputScreen` on `ScanCheck`: checked marks in the face map, "N faces left" line, `[Kuvaa tämä puoli uudelleen] [Näyttää oikealta]`, verdict line and solution on the last "Looks right", whole-cube rescan in the menu, rescan hidden without readings; consumes a returned face; `scan.check` log line; strings fi/en; Compose tests: looks right moves to the next unchecked face, an impossible cube names the faces, a valid one calls `onValid` (`:app:testDebugUnitTest`, lint)
- [x] 2.3 Screenshot tests: check with faces left, check with a verdict, one-face scan; existing `scan-check` updated; one-screen fit verified in the screenshots; refresh the gallery after the push

## 3. Docs

- [x] 3.1 `docs/architecture.md` (face-by-face check, misread search, one-face scan), `docs/operations.md` (`scan.check` line), roadmap row 21 `scan-check-faces` done; verify by reading
