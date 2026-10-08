# Design: scan-speed-up-3

## Context

See `proposal.md` (Why). `FaceTracks.onFrame` runs, every picture: `Tracker.onFrame`, `updateRefs`,
`votesOf` for live tracks, `recheck`, `assignOpen`, `updateVoting`, `settleTurns`, `evidenceOf`,
`BestCube.solve`, `retire`. Only `Tracker.onFrame` and `votesOf` depend on the picture itself; the
rest is global work over all tracks. A track's votes change only when it gets a reading, or when the
references change (`updateRefs` names every reading again). Measuring tool from explore: a replay of
the long fixtures timing `VideoScan.onFrame` per 100-picture window (kept in the test, task 1).

## Goals / Non-Goals

- Goal: late in a long scan, a picture costs about what it costs once the faces are first known;
  a picture without faces costs almost nothing.
- Goal: the same results. A and B are exact (same numbers); C's memo is exact.
- Non-goal: changing what the scan decides, its thresholds, or `MAX_OPEN`/`MAX_FACE_READINGS`;
  retiring unsettled tracks earlier (decided against before: lost the striped U2 cube, `202403`).
- Non-goal: the finder (`FaceFinder`) and the page's paint; this is the scan logic only.

## Decisions

1. **A: a change flag with a deadline.** `FaceTracks` keeps `dirty`: set when the picture gives any
   track a reading, when the references change, and when `now` passes the next time limit, the
   earliest of a track's `lastAt + GAP_MILLIS` (it stops being live, `stale()` and `assignment()` then
   change) and, for a track taken for no face, `lastAt + RETIRE_MILLIS`. When not dirty, `onFrame`
   runs only `Tracker.onFrame` (to know nothing was added) and returns; `picture`, `evidence`, `best`
   and the tracks' states stay as they were. `undecided(now)` and other time-read queries already
   take `now` at the call, so they stay right.
   Alternative: skip only pictures with no faces. Simpler, but misses a picture whose only face is
   left out and the time limits; the deadline costs little more.

2. **B: per-track versions, pair caches keyed by them.** Each track's state gets a `version`, bumped
   when `votesOf` changes its votes; a global `generation` is bumped when the references change.
   `assignOpen`'s pair cost table for (a, b) is kept with the two versions, the generation and the
   pair rules' answer for the pair (`PairRules` gets a version per pair, or a global one bumped when
   `observe` changes a ban), and built again only when one of them moved. With up to 8 open tracks and
   1–2 live, that turns ~28 tables a picture into ~7. The `bound` list in `unary` (pair rules against
   settled tracks) is cached the same way. `unary`'s content cost is not cached: it reads the global
   likelihoods, which change with any vote, and is cheap.
   `recheck` builds `tables()` once and, per track, a copy with that track's votes taken out
   (`add(..., -1.0)`), instead of summing all tracks again for each (O(n) instead of O(n²)).
   Alternative: incremental likelihoods. Not worth it: the profile puts the cost in pairs and cube
   searches.

3. **C: memo in `settleTurns`, searches only on change.** Within one picture `settleTurns` keeps
   `BestCube.cost` per turn combination (the 6 extra turns packed into an int); its second round and
   the settle check repeat combinations already costed. It already starts from the last picture's turns
   (they are folded into the tracks' assignments). With A in place `settleTurns` and `BestCube.solve`
   run only on dirty pictures.
   A time-based rate limit (best cube at most every ~100 ms) is held back: added only if A+B+C miss
   the budget (Decision 4), and then only if `aClearCubeFinishesWhileNewFacesKeepComingIntoView`
   (finish within `FINISH_MILLIS + 100`) still passes; the choice goes into this file.

4. **Budget test.** `aLongScanReadsAsQuicklyLateAsEarly` is replaced: after a warm-up on another
   fixture, `web_121505` and `PHONE_SCAN_2` are replayed (100 ms a picture); every 100-picture window
   averages at most 3 ms a picture, and pictures without faces from picture 200 on at most 0.5 ms.
   On CI (`CI` set) both budgets are tripled: the shared GitHub runners are slower and noisy, and the
   test should catch the 5× growth, not machine speed. The bound on tracks worked through stays.
   Alternative: compare late with early in the same run. Rejected: the start is cheap because nothing
   is known yet, so the ratio cannot be held and the old version of the ratio missed the growth.

5. **Same results check.** Before the code changes, a scratch replay records each picture's state
   (stickers, complete, finished, clearness) for every fixture used in the scan tests; after each step
   the same replay must give the same lines. Tiny float differences from decision 2's subtraction
   could flip a tie; any difference is looked at, and accepted only if the scan tests still pass.

## Risks / Trade-offs

- [The change flag misses a cause of change, so a stale state is shown] → the same-results check
  over all fixtures (decision 5) catches it; the deadline list is kept next to the flag in code.
- [Caches hold old numbers after the references change] → one global generation in every cache key.
- [Budget test flaky on a busy machine] → window averages, warm-up, tripled budget on CI.
- [The browser gains less than the JVM] → `scanMs` in the next browser log shows it; the user checks.

## Migration Plan

None: in-memory per scan; nothing stored.
