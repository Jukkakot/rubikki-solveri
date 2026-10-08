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

0. **As built (2026-10-08): a track does not go back without a new reading of its own** (`hold()` at the
   end of the deciding steps, after `settleTurns`). Each track keeps the decisions it ended the last picture
   with (face, option, assigned turn, other face, by-cube) and the ones it had before its last change, with
   its newest reading's number at that change. When the work would take it back to those earlier decisions
   and the track has had no new reading since, it keeps the current ones (voting is then worked out again).
   A turn settled by the cube and one settled by a picture count as the same way back. Decisions 1 and 2
   below were tried first and dropped (Findings); this is design 1's alternative A widened from "re-opened"
   to any decision. It needs no new threshold, and its risk (a track out of view kept as it is) cost no
   finish on the fixtures.

1. **(Tried, dropped) One rule: settle only what stays settled.** Whatever `assignOpen` settles (an option, a face, or
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

2. **(Tried, dropped) The turn of a face-only track is left to the cube.** For a track whose face is settled and whose
   turn `settleTurns` decides (no turn-settled track on that face), `assignOpen` keeps the track's
   assigned turn: the face's four turns cost the same there (their least), and `STICKY` then keeps the
   last turn. `settleTurns` alone moves it. Today the two pull the turn two ways (`B?B1` ↔ `B?B3`).
   If the replay shows a face's turn is better picked by the pair costs in some fixture (a finish later
   or lost), the measure is taken back and this goes into the findings.

3. **(Covered by 0) Leaning ↔ face** (`?B1/L` ↔ `B?B0`) is looked at after 1 and 2. If it is still there, it is
   the face-only part of decision 1 (a face set by `assignOpen` that `recheck` takes away). Otherwise
   the cause goes into the findings and is fixed here only if it stays inside these functions.

4. **Test.** A JVM test replays every fixture of the replay list and fails on any track whose state goes
   A, B, A over three pictures while its readings are unchanged (using `describe()`/`snapshot()`).
   The flip count with readings is written to the test's output and is not asserted. The scratch replay
   gives the before/after table of finish pictures in this file.

## Risks / Trade-offs

- [A track out of view is held in a state that the rest of the cube would now change back (decision 0)] →
  it moves on with its next reading; no finish on the fixtures came later. If a phone scan stalls on it, a
  time escape (go back after about a second) is the next step.
- [An early wrong settling is undone later than today] → it re-opens as before on new evidence; the
  "face read wrong at first" and look-alike fixtures cover it.
- [Turns left to the cube settle slower without the pair costs' pull] → decision 2 is taken back
  if the replay shows it.

## Findings

Baseline (1.1, 1.2, today's code): the new test counts **493** A, B, A flips without a new reading and
**31** with new readings over the 22 fixtures (its own count, per track and picture; the replay's flip
counter above counted about 480 by `describe()` and the reading count). So nearly all flips happen
with nothing new read: the cause is the work itself, not the readings.

Finish picture per fixture (unchanged by `TrackInfo`'s two new fields): `151828` 228, `151903` 119,
`213729` 124, `213817` 125, `213850` 168, `213929` 192, `132721` 158, `152753` 73, `202156` 159,
`202318` 170, `202403` 144, `web_084657` 178, `web_181940` 202, `web_084657:66-455` 105, `blueFirst`
188; `132049`, `202058`, `20261007_web`, `web_121505` and the other slices do not finish.

Where the flips come from (2.x, `Diag` in the scratchpad: a track's describe and `costsOf` per picture):

- `213929` #19 (`=none` ↔ `U?U2`): alone, `U` costs 2.7 and no face 6.0, so `recheck` re-opens `=none`;
  `assignOpen` takes it for `U`; then `recheck`'s rule for two tracks of one face that read it otherwise
  in every turn re-opens it and the settled `#0 =U0`, and assigned together the clash cost sends #19 to
  `none` again. The joint assignment never sees a clash with a *settled* track of the same face.
- `web_084657` #92/#93 (`U?U2` ↔ `U?U3`): two face-only tracks of one face whose costs alternate with each
  other's turn (coupled).
- By kind over all fixtures (by `describe()`): turn of a face-only track 155, face ↔ open 115, face ↔
  settled 99, the rest under 35 each.

Tried and dropped (replay of all fixtures, flips without readings by the test / finishes):

| step | flips | finishes |
|---|---|---|
| baseline | 493 | 15 |
| decision 1 (assign keeps recheck's tests, also the same-face clash) | 1728 | 13 (`202403`, `web_084657` lost) |
| pair costs with the settled tracks in each track's own cost (both checks alike) | 496 | 14 (`213729` lost; `202318` 140, `202403` 195) |
| decision 2 (face-only turn left to the cube) | about 585 by kind | 14 (`202403` lost) |
| decision 0 (hold) | 5 (all A by picture, B, A by cube) | 15 |
| decision 0, by-cube and by-picture alike | **0** (8 with new readings, 31 before) | 15, `202403` 143 (144) |

The steps judge a track in three ways and fixing one pair of them moved the flips elsewhere; the hold
stops the back-and-forth whatever its cause. Shown stickers flipping A, B, A: `web_121505:66-373` 7 → 0,
`web_084657` 31 → 4 (after its finish), `web_121505` 4 → 3, `web_121505:448-751` 0 → 2, `202403` 0 → 1.
`RulesScanTest` and every cube test pass, the budget test included.
