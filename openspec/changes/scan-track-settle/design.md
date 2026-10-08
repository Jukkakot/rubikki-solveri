# Design: scan-track-settle

## Context

See `proposal.md` (Why). In `FaceTracks.work` the steps that decide a track are `recheck` (settled →
open), `assignOpen` (open → assigned, face-only `X?Xk`, or settled `=Xk`/`=none`) and `settleTurns`
(face-only tracks' turns from the best cube). The track's state lives in `State`: `face`, `option`,
`assigned`, `otherFace`, `byCube`. `describe()` writes it as `=U0` (settled), `U?U2` (face only,
assigned turn), `?B1/L` (open, leaning). The measuring tool is the scratch replay from `scan-speed-up-3`
(one line per picture of every fixture, `describe()` at the end) plus a flip counter: a track whose
state goes A, B, A over three pictures.

Replay of today's code (2026-10-08), flip pictures per kind:

| kind | example | where |
|---|---|---|
| settled ↔ face only | `=B3` ↔ `B?B3`, `=R0` ↔ `R?R0` | `202403`, `web_121505:66-373` (#2, #18, #21: 11 each) |
| none ↔ face | `=none` ↔ `U?U2`, `?none/null` ↔ `D?D2` | `213929` #19 (20, no reading at all), `web_084657` #76 |
| turn of a face-only track | `B?B1` ↔ `B?B3`, `U?U2` ↔ `U?U3` | `web_084657` #92 (21), `web_121505:66-373` #9, #15 |
| leaning ↔ face | `?B1/L` ↔ `B?B0` | `web_084657` #60, #69, #78, #81, #84 (about 20 each) |

Totals: 269 in `web_084657`, 136 in `web_121505:66-373`, 49 in `web_121505`, under 25 elsewhere. The
shown stickers flip with it in `web_121505:66-373` (27 stickers, pictures 167–173) and in `web_084657`
after its finish (16 stickers, pictures 830–849; the app stops at the finish, the replay goes on).

Why the first two kinds happen (`213929` #19, which gets no reading over the 20 pictures):
`assignOpen` takes the track for no face with the margin of the joint assignment. Next picture,
`recheck` costs it alone against the settled tracks; a face is cheaper by more than `REOPEN_SLACK` = 0,
so it re-opens. `assignOpen` assigns `U2` and settles the face, not the turn. Next picture `recheck`
finds `none` cheaper than the face alone, and so on. The two checks use different costs (joint with
pairs and `STICKY`, against alone) and there is no gap between "settle" and "re-open" in the same
measure.

## Goals / Non-Goals

**Goals:**
- With no new reading of a track (nor a time limit crossed), its state does not change from one picture
  to the next. In the replay: no A, B, A flip of a track whose readings did not change.
- With new readings, flips are rare: the flip count over all fixtures drops to under a tenth (today
  about 480).
- Fixtures: everything in `RulesScanTest` still passes (each finishes as today or earlier, never wrong,
  times and budget). Each fixture's finish picture is listed before and after. A finish up to five
  pictures later (half a second) is accepted if the design notes it.
- Steady state also late in the long recordings: no-face pictures skip the work there (`steady`).

**Non-Goals:**
- No new thresholds tuned for one fixture. No change in what counts as evidence, in the best cube,
  the finish rule or the earlier scanner.
- Not making the long recordings `web_121505` and `PHONE_SCAN_2` finish (welcome if it happens).

## Decisions

1. **One rule: settle only what stays settled.** Whatever `assignOpen` settles (an option, a face, or
   `none`) must pass `recheck`'s own test at that moment, with the costs `recheck` would use (the track
   alone, against the settled tracks, its own votes out). A track that would fail stays open with its
   assignment, as an unsettled track does today. Then `recheck` in the next picture, given the same
   readings, finds nothing to re-open. It only re-opens what new evidence has turned against.
   The test is shared by the two callers in one function, so the two cannot drift apart again.
   Alternative A: a memory. A track that `recheck` re-opened is not settled again until it gets a new
   reading. This is simpler and certain to stop flips without readings, but it does not help with flips
   while readings come in (`web_121505:66-373`), and a track out of view would stay open for good.
   Kept as the fallback if decision 1 changes the finishes too much.
   Alternative B: `recheck` judges a track as it would be in the joint assignment (put back with the
   open ones). This is the same measure on both sides but costs a branch and bound per settled track per
   picture. Rejected for its cost.

2. **The turn of a face-only track is left to the cube.** For a track whose face is settled and whose
   turn `settleTurns` decides (no turn-settled track on that face), `assignOpen` keeps the track's
   assigned turn: the face's four turns cost the same there (their least), and `STICKY` then keeps the
   last turn. `settleTurns` alone moves it. Today the two pull the turn two ways (`B?B1` ↔ `B?B3`).
   If the replay shows a face's turn is better picked by the pair costs in some fixture (a finish later
   or lost), the measure is taken back and this goes into the findings.

3. **Leaning ↔ face** (`?B1/L` ↔ `B?B0`) is looked at after 1 and 2. If it is still there, it is
   the face-only part of decision 1 (a face set by `assignOpen` that `recheck` takes away). Otherwise
   the cause goes into the findings and is fixed here only if it stays inside these functions.

4. **Test.** A JVM test replays every fixture of the replay list and fails on any track whose state goes
   A, B, A over three pictures while its readings are unchanged (using `describe()`/`snapshot()`).
   The flip count with readings is written to the test's output and is not asserted. The scratch replay
   gives the before/after table of finish pictures in this file.

## Risks / Trade-offs

- [A track that should settle stays open longer (decision 1 is stricter), so a scan finishes later] →
  finish pictures before and after per fixture; the fallback (alternative A) is ready.
- [An early wrong settling is undone later than today] → it re-opens as before on new evidence; the
  "face read wrong at first" and look-alike fixtures cover it.
- [Turns left to the cube settle slower without the pair costs' pull] → decision 2 is taken back
  if the replay shows it.

## Findings

(filled in during apply)
