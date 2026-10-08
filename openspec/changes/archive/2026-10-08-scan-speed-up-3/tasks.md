# Tasks

## 1. Measuring first (cube)

- [x] 1.1 Same-results replay (design 5): a scratch replay records each picture's state (stickers, complete, finished, clearness) for every fixture the scan tests use, before any code change; kept outside the repo, rerun after each later task and must match (any difference looked at, accepted only with the scan tests passing)
- [x] 1.2 Budget test (design 4) replacing `aLongScanReadsAsQuicklyLateAsEarly` in `RulesScanTest`: warm-up, `web_121505` and `PHONE_SCAN_2` replayed, every 100-picture window ≤ 5 ms a picture (first 3 ms), pictures without faces from picture 200 on ≤ 0.5 ms, budgets ×3 when `CI` is set, the bound on tracks kept; it fails on the current code (record the numbers in `design.md`)

## 2. A: nothing changed, nothing redone (cube)

- [x] 2.1 Change flag with deadline in `FaceTracks.onFrame` (design 1): dirty on a new reading, a reference change, or `now` past the next `GAP_MILLIS`/`RETIRE_MILLIS` limit; otherwise only `Tracker.onFrame` runs. JVM test: a track left alone settles, stops being live and retires at the same pictures as before; same-results replay matches; numbers into `design.md`

## 3. B: work kept between pictures (cube)

- [x] 3.1 Track versions and the references' generation; `assignOpen` pair cost tables and `unary`'s pair-rule bound cached by them (design 2), `PairRules` reporting when a pair's ban changes. Same-results replay matches; scan tests pass
- [x] 3.2 `recheck` from one full table minus each track (design 2). Same-results replay matches (or differences accepted as in 1.1); numbers into `design.md`

## 4. C: fewer best-cube searches (cube)

- [x] 4.1 `settleTurns` memo of `BestCube.cost` per turn combination within a picture (design 3). Same-results replay matches; budget test passes, else the rate limit of design 3 is tried with the finish test as the guard and the choice recorded in `design.md`

## 5. Wrap-up

- [x] 5.1 `./gradlew check`, web build, browser smoke tests
- [x] 5.2 Docs: a line in `docs/architecture.md` on the scan's per-picture caching and the change flag (where it lives); roadmap entry
- [x] 5.3 List for the user to try: `scanMs` and `fps` in the browser and phone logs early and late in a long scan (about the same), the scan still finishes as before
