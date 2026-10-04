# Tasks

## 1. Reading and naming (cube)

- [ ] 1.1 `FrameSampler.sample`: middle 60 % of the cell, per-channel trimmed mean (25th–75th percentile)
- [ ] 1.2 Brightness-scaled comparison (gain capped at 6×) in `ColorClassifier`; live recognition in `ScanSession` uses it
- [ ] 1.3 `outcome()`: joint 6 × 6 centre naming, faces renamed (readings and `from` follow); next-best namings (at most 12) when no solvable cube; `renamed` in the outcome for the log
- [ ] 1.4 Tests: log fixture from `evidence/scan-log-2026-10-04.txt`; evening scan valid; daylight scans unchanged; 10:47 and 11:07 results reported; `072641` recognised as the blue face; trimmed mean ignores a highlight; worst-case outcome time measured

## 2. Check after every scan (shared)

- [ ] 2.1 `RubikkiNavHost.onResult`: always the colour check; confident scans without marks and with `check_note_ok` (fi/en); pictures follow renamed faces; `scan.done` logs `renamed`
- [ ] 2.2 Tests updated: scan flow ends in the check, "Näyttää oikealta" opens the solution

## 3. Check

- [ ] 3.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution`; roadmap row and backlog item (browser crash after a long background)
- [ ] 3.2 `docs/`: the scan's naming and the check after every scan, where the wiki describes them
