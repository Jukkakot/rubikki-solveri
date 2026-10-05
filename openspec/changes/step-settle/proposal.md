# Proposal

## Why

Phone testing (2026-10-05): after tapping "Tein sen" the cube first turns the move the user just
made, and only then shows the next one. It reads as a new instruction. The automatic demo returns
the cube to before the move, so "done" has to play the move again. The arrow in the mirror is also
one thing too many.

## What Changes

- The demo ends where the move ends: after the automatic demo (and after "show") the cube stays in
  the state after the move, which is how the user's real cube looks once they have turned it.
  "Show" starts again from before the move.
- "Done" no longer animates the move. The cube is already in the new state (or jumps there if the
  demo has not finished), gives a small nod (tilts a few degrees and back, about 0.3 s), and the
  next move appears and demos as before. The same applies when camera follow detects the move.
- "Back" goes to the previous move with the same nod: the cube jumps to before that move and its
  demo plays again (instead of animating the undo).
- The mirror shows the cube, highlight and turning, but no arrow.
- The nod is skipped when the animations are off (reduced motion).
- Mockups the user chose from: https://claude.ai/artifact/8FU4Jx3GEZE5VmgaYWJRKZ (option A with the
  nod from option C).

## Capabilities

### New Capabilities

### Modified Capabilities
- `move-guide`: demo ends after the move; the nod on a step change; no arrow in the mirror.
- `fast-solve`: stepping no longer animates done or back.

## Impact

`shared` only: `ui/guide/StepperState.kt` (done, back, demo), `ui/cube3d/Cube3D.kt` (mirror arrow,
nod), the solve screen's tests. `cube`, `app` and `web` are untouched.
