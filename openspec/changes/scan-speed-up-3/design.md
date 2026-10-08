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
   averages at most 5 ms a picture (3 ms at first; raised by the user on 2026-10-08 after A+B+C, see Measurements), and pictures without faces from picture 200 on at most 0.5 ms.
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

## Measurements

JVM, ms a picture per 100-picture window (budget test, then the scratch bench for pictures without faces
from picture 200 on).

| step | `web_121505` windows | `PHONE_SCAN_2` windows | no-face late |
|---|---|---|---|
| before (1.2) | 2.0, 13.4, 11.5, 10.7, 10.8, 11.0, 9.4, 7.8 | 1.4, 4.6, 6.6, 7.3, 7.2 | ~10 / ~7 |
| A (2.1) | 1.6, 9.6, 9.4, 7.1, 7.1, 6.9, 7.0, 3.5 | 0.9, 3.8, 5.7, 5.9, 5.1 | ~2 / ~2.5 |

Found in 2.1 (decision 1 amended):

- The per-picture work is not a fixed point: a track can flip between two states picture after picture
  (`web_121505`, #19 `=none` ↔ `U?U2`). Skipping on "no reading, no limit" alone shifted those flips and
  changed the shown state in 875 pictures of the long recordings. So the skip also needs the last
  picture's work to have changed nothing (`steady`: each track's face, option, assignment, other face,
  open-since and by-cube, the voting tracks and the references compared before and after). Then the skip
  is exact: the replay matches line for line. A state that keeps flipping is worked out every picture,
  so no-face pictures late in these recordings still cost ~2 ms; B and C make that work cheaper.
- `LIVE_MILLIS` (a track's double weight in `updateVoting`) is a third time limit, listed with
  `GAP_MILLIS` and `RETIRE_MILLIS` in `FaceTracks.LIMITS`.
| B (3.1, 3.2) | 1.4, 8.5, 6.8, 4.3, 4.5, 4.1, 3.5, 1.9 | 0.9, 3.8, 5.6, 5.8, 5.2 | ~1 / ~2.5 |

Found in 3.x: a track's version is its newest reading's number with the references' generation
(`FaceTracks.version`): readings are only added and the oldest dropped, so that number tells the readings,
and so the votes and leading colours. The pair tables brought `assignOpen` from about half of the late
work to ~11 %; `unary`'s pair-rule bound was ~1.5 %, so it is not cached (left out on the measurement). The
replay still matches line for line after `recheck`'s subtraction. The rest is now the best-cube searches:
`BestCube.solve` ~36 %, `settleTurns` ~26 %, `turnsClear` (called by `VideoScan` each picture) 9–14 %.
| C (4.1) + `lookCost` per face + `Search` bound sums | 1.0, 4.2, 4.6, 2.5, 2.5, 1.6, 2.0, 0.9 | 0.5, 2.2, 2.4, 2.8, 2.1 | ~0.4 / ~0.45 |

Found in 4.1 (decision 3 amended): the memo is keyed by the evidence's votes, not by the turn combination:
equal votes give an equal best cube, so it is exact by construction and also covers the trials that repeat
from one picture to the next, `turnsClear` and `BestCube.solve` itself (kept while the evidence is the
same). A key by turn combination is not exact once a face's pick is not 0 (the tracks the cube settled are
then read at their settled turn, the others at the new one). Two more exact cuts went in: `unary` works
`lookCost` out once per face, not per option (it was ~14 %), and `Search` keeps the bound's pair sums per
depth. A row-minimum pre-check before each of Murty's assignments in `PieceSearch` gained ~3 % and was
left out.

Still over the budget: `web_121505` pictures 100–499 (open tracks piling up, often two faces in view);
`BestCube.solve` is ~35 % there, the turn trials ~22 %. The time-based rate limit cannot help the replay
(one picture per 100 ms already).

Budget decision (user, 2026-10-08): the window budget is 5 ms on the JVM (about 15 ms in the browser), not 3;
the no-face budget stays 0.5 ms. Still catches the old growth (11–13 ms windows). No rate limit, no further
best-cube work; the results stay exactly the same as before the change. A track that flips state picture
after picture is left for its own change (roadmap).
The no-face budget is checked on the median of those pictures (they mostly skip the work; the few with a state still moving are counted in the windows): the average sat on 0.5 ms in a Gradle run.
The warm-up also replays the two timed recordings once (decision 4 said another fixture): with only the other one, the window 100–199 swung 5.1–6.0 ms between Gradle runs; warmed on them it passes steadily.
The windows are timed by the thread's CPU time (a parallel `./gradlew check` pushed the clock-timed windows to 20 ms); the no-face median stays on the clock, as the CPU time ticks in 15.6 ms steps on Windows.
