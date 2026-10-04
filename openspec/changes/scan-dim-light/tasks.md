# Tasks

## 1. Reading and naming (cube)

- [ ] 1.1 `FrameSampler.sample`: middle 60 % of the cell, per-channel trimmed mean (25th–75th percentile)
- [ ] 1.2 Brightness-scaled comparison (gain capped at 6×) in `ColorClassifier`; live recognition in `ScanSession` uses it
- [ ] 1.3 `outcome()`: joint 6 × 6 centre naming, faces renamed (readings and `from` follow); next-best namings (at most 12) when no solvable cube; `renamed` in the outcome for the log
- [ ] 1.4 Tests: log fixture from `evidence/scan-log-2026-10-04.txt`; evening scan valid; daylight scans unchanged; 10:47 and 11:07 results reported; `072641` recognised as the blue face; trimmed mean ignores a highlight; worst-case outcome time measured

## 2. Check after every scan (shared)

- [ ] 2.1 `RubikkiNavHost.onResult`: always the colour check; confident scans without marks, with `check_note_ok` (fi/en), a 5 s automatic continue shown on "Näyttää oikealta", "Skannaa koko kuutio uudelleen" at hand, any touch stops it; pictures follow renamed faces; `scan.done` logs `renamed`
- [ ] 2.2 Scan screen without face names (design 7): live status, review name and face choice, "X luettu", the grid's centre hint go; done marks filled with the centre as seen; single-face rescan unchanged; strings removed/replaced in fi/en
- [ ] 2.3 Tests updated: scan flow ends in the check and opens the solution by itself when confident; a touch stops it; scan again from the check; no face name shown while scanning

## 3. Check

- [ ] 3.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution`; roadmap row and backlog item (browser crash after a long background)
- [ ] 3.2 `docs/`: the scan's naming and the check after every scan, where the wiki describes them
