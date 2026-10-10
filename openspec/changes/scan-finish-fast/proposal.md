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

No specced behaviour changes: the spec already says a misread is outvoted when the fitting readings
"clearly outnumber it" and that the finish comes about half a second after the cube is clear. This is
tuning, so there is no spec delta.

## Capabilities

### Modified Capabilities

(none, tuning within the current spec)

## Impact

- `cube`: `FaceTracks`, `VideoScan` finish logic and constants; a harness metric "clear → finish"
  for every fixture and recording.
