## 1. Baseline and test

- [x] 1.1 Scratch replay (from `scan-speed-up-3`, now in this session's scratchpad) on today's code: per-picture lines, flip counts per fixture and kind, finish picture per fixture; summary table into `design.md` Findings
- [x] 1.2 `RulesScanTest`: a test that replays every fixture and fails on a track going A, B, A while its readings are unchanged; logs the flip count with readings (red before the fix)

## 2. One rule for settling (cube)

- [x] 2.1 `FaceTracks`: `recheck`'s test as one function, used by `recheck` and by `assignOpen` before it settles an option, a face or `none` (design 1). Done as a refactor (replay identical); using it in `assignOpen` was dropped, see 2.2
- [x] 2.2 Replay: flips and finish pictures against 1.1; all of `RulesScanTest` green. If finishes go more than five pictures later or one is lost, switch to the fallback (design 1, alternative A) and note it. Decision 1 lost two finishes and tripled the flips: switched to the hold (design 0)

## 3. Turns and leaning (cube)

- [x] 3.1 `assignOpen` keeps a face-only track's turn where `settleTurns` decides it (design 2); replay and tests as in 2.2. Tried, lost a finish: dropped
- [x] 3.2 Look at the `?X/Y` ↔ `X?X` flips still left (design 3); fix within these functions or record the cause. None left after the hold
- [x] 3.3 Test from 1.2 green; flips with readings under a tenth of 1.1; budget test still passes; before/after table in `design.md`

## 4. Docs and roadmap

- [x] 4.1 `docs/architecture.md`: one line on the settle rule if the map changes; roadmap item 69 `scan-track-settle` done, backlog item removed
