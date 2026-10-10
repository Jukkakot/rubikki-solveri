# Proposal

## Why

After `scan-finish-unblock`, recording `web_20261010_102548` finishes right, but only at about
15.6 s, while its cube was clear from about 5.5 s. The user (2026-10-10): "optimise it as fast as you
possibly can." The finish rules and thresholds hold back a cube that is already clear. They are the
3:1 outvote ratio, the evidence that counts, how long tracks must settle, and `FINISH_MILLIS`.

## What Changes

- Tune the video scan's finish so it comes as soon after the cube becomes clear as safety allows.
  This covers the outvote ratio, using the cube's clearness (margin) to relax it, quicker settling
  of turns once the cube is clear, and any other finish condition the measurements point at.
- Hard constraint: no wrong finish on any fixture or recording, normal or robustness variant, or on
  the synthetic scrambles. No fixture may finish later than today.
- Measured target: a "clear → finish" delay per fixture and recording, reported before and after.

The measurements moved the finish past the spec's words: about a third of a second instead of half
a second, a misread outvoted by as many fitting readings (not "clearly outnumbered"), and a face whose
readings are counted in the evidence never holds a clear cube. So the change got a small delta of
`video-scan` after all (decided during implementation, autopilot).

## Capabilities

### Modified Capabilities

- `video-scan`: Finish the video scan (finish time, when a face reading against the clear cube holds it)

## Impact

- `cube`: `FaceTracks`, `VideoScan` finish logic and constants; a harness metric "clear → finish"
  for every fixture and recording.

## Implementation notes

Measured with `FinishDelayHarness` (FINISH_DELAY=1): per fixture (as recorded and robustness), recording
and 60 synthetic scrambles (each face alone, as `ScanNeverLockedTest`), the time the best cube first became
right and clear, the finish, the delay between them, what held it meanwhile, and over the whole video how
long a wrong cube was ever complete. Times in s; fixtures at 10 fps; recording times are its own clock.

### Before and after (clear → finish)

| run | clear | finish before | delay before | finish after | delay after |
|---|---|---|---|---|---|
| web_20261010_102548 | 5.0 | 15.6 | 10.5 (held by two misread white tracks) | 5.3 | 0.3 |
| web_181940 | 11.1 | 20.2 | 9.1 (turns not clear, 86 pictures) | 11.5 | 0.4 |
| 20261007_202403 | 11.9 | 14.3 | 2.4 (turns not clear) | 12.5 | 0.6 |
| 20261005_213850 robust | 16.0 | never | (misread held to the end) | 16.3 | 0.3 |
| every other run that finished | | | 0.5 | | 0.3 |
| web_20261009_100814 / 100824 | | | 0.5 | | 0.3 |
| synthetic, 60 seeds | | | 0.5 each | | 0.3 each |

Real runs: mean delay 1.33 → 0.32 s, longest 10.5 → 0.6 s. Wrong finishes 0 before and after (normal,
robustness, recordings, synthetic). No run finishes later; one more robustness run finishes. Longest time a
wrong cube was complete: 130 ms before (100824, after its right finish), 0 after. Acceptance bar holds
(ms per frame 0.8–5.2).

### What held the finish (1.2)

- `102548`: the white side's only tracks (14 and 9 readings) each misread two or more stickers, the cube clear
  from the other five; the 3:1 outvote needed 42 fitting readings but a track keeps at most 40, so only a second
  fitting white track at 15.6 s freed it.
- `web_181940`, `202403`: `turnsClear`, a pair of other turns within 2.0 of the best cube.

### Levers (one at a time, 2.1)

Kept:
1. A track whose readings are evidence (voting) does not hold the finish (`quietFor`): the cube is clear with
   them counted. Alone: `102548` 15.6 → 5.6 s, nothing else changed. Removing the quiet check entirely gave the
   same table, so it guards nothing on the data, but the rule for tracks outside the evidence stays.
2. `OUTVOTE` 3 → 1 (as many fitting readings): `web_181940` 20.2 → 11.7 s (an outvoted misread also leaves the
   evidence, so the turns become clear), `213850` robust finishes, the 130 ms wrong-complete of `100824` gone.
   2 and 1.5 gave none of this beyond `102548`.
3. `FINISH_TURN_MARGIN` 1.5 for `turnsClear` only (settling stays at 2.0): `202403` 14.3 → 12.7 s. 1.0 gave the
   same; 0.5 let `202058`'s wrong cube be complete 100 ms, so 1.5 keeps a margin.
4. `FINISH_MILLIS` 500 → 300: every delay 0.2 s shorter. Kept over 130 ms (longest wrong-complete seen before).

Rejected:
- `OUTVOTE` 0 (any misread outvoted, also out of the evidence): later finishes (`web_084657` 10.5 → 19.3 s,
  `132721` robust never).
- `TURN_MARGIN` 1.5 (settling turns): `web_121505:66-373` finished WRONG, `202403` never.
- Clearness 1.5 for the finish: wrong cubes complete for 3.6 s (`202318`) and 1.7 s (`100824`).
