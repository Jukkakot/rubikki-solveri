# Tasks

## 1. Acceptance harness

- [x] 1.1 Grow `CornerScanHarness` into the acceptance harness: every fixture as recorded and in the robustness variant, per mode (old, rules) frames to finish, right / WRONG, ms per frame; a JVM test (gated, as now) that fails when the rules path breaks the bar of design 6; verify it runs and prints today's numbers for the old mode.
- [x] 1.2 Move the synthetic corner/edge view builder from `CornerReaderTest` into a shared test helper (any cube, any corner or edge view, roll, reading turns, centre overrides); verify `CornerReaderTest` still passes using it.

## 2. Tracks

- [x] 2.1 Add tracks (design 2): continuation by position, step vectors, in-plane turn and sticker agreement; partial readings continue only; JVM tests with synthetic sequences (a face moving and turning slowly stays one track; a jump to another face starts a new one; a partial reading continues); verify `./gradlew :cube:jvmTest --tests "*Track*"` passes.

## 3. Pair rules

- [x] 3.1 Derive the hard pair rules from one picture (design 3): distinct faces for any two tracks; for touching faces (edge view by `sideTowards`, three-face view by the common corner) the allowed (face, turn) pairs; JVM tests with the synthetic views for every edge and corner (the true pair allowed, the opposite colours and the mirrored turns not); verify the tests pass.

## 4. Face solver

- [x] 4.1 Add the option costs (content against the previous best cube, soft centre look relative to settled centres) and the joint branch-and-bound with settling and re-opening (design 3–4); JVM tests: look-alike red/orange centres shown apart finish right; white and pale-yellow side by side never both white and yellow; the striped cube; blue first, white later never shows a wrong sticker; a face read wrong at first is put right; verify the tests pass.
- [x] 4.2 Add the finish rule and known stickers (design 5) and the "two ways to tell the faces" case (does not finish until settled); verify a test with two interchangeable faces never seen with a common neighbour does not finish and raises the turn-the-cube flag after about two seconds.

## 5. Integration

- [x] 5.1 Give `VideoScan` the mode (design 6), building the same `VideoScanState` from the solver (stickers, leading, found faces with names and known colours, confirmed sides, projection and pose from settled tracks, stall); run the existing `VideoScanTest` and `ScanPaintTest` against both modes; verify both pass (scenarios that only described the old piling are rewritten to the new spec).
- [x] 5.2 Run the acceptance harness; study every wrong or unfinished fixture, starting with the robustness wrong cube of `20261005_151828`; add any further safe rule it shows (recorded in design.md); verify the rules path meets the bar.
- [ ] 5.3 Show the turn-the-cube flag on the status line ("Käännä kuutiota" / "Turn the cube", fi + en strings in `shared`; a stall notice still takes its place); one light test that the line shows it when the state says so; verify it passes.

## 6. Both scanners, switch and wrap-up

- [ ] 6.1 Add the scanner choice to Settings (new / earlier, remembered, default new) and `engine=rules|look` on the scan log lines; remove the spike's `rules` flag from the old path; a light test that the choice changes the scanner the scan uses and survives a restart; verify the test passes.
- [ ] 6.2 Make the rules path the default once 5.2 meets the bar; verify `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` passes.
- [ ] 6.3 Update `docs/architecture.md` (the scan pipeline map: both scanners, where the choice lives) and the roadmap entry; list for the user what to try on the phone (both scanners in look-alike light, the striped cube, a normal scramble) and the recordings to add as fixtures (striped cube, dim light); verify the docs describe both scanners.
