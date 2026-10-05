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
- "Back" works as now: it animates the undo of the previous move (from the state before the
  current move; after a demo the cube first jumps back to it), then that move demos again. No nod
  (user, 2026-10-05).
- The last "done" ends in a small victory instead of a nod: the cube hops and spins once (~1 s)
  while confetti in the six sticker colours bursts from it, with a success vibration (user's
  choice c, 2026-10-05). No motion with animations off.
- The arrow is shown only before the move: not after the demo, when the cube already shows the
  result (user, 2026-10-05).
- The mirror never shows the arrow. When the turning face is hidden from the view (the back, and
  in some views the bottom or left), the arrow goes around the outside of that layer, beside the
  cube, instead of being drawn over the front (user, 2026-10-05).
- The nod is skipped when the animations are off (reduced motion).
- Mockups the user chose from: https://claude.ai/artifact/8FU4Jx3GEZE5VmgaYWJRKZ (option A with the
  nod from option C).

## Capabilities

### New Capabilities

### Modified Capabilities
- `move-guide`: demo ends after the move; the nod on a step change; no arrow in the mirror.
- `fast-solve`: done no longer replays the turn; back unchanged; the solved celebration.

## Impact

`shared` only: `ui/guide/StepperState.kt` (done, back, demo), `ui/cube3d/Cube3D.kt` (mirror arrow,
nod), `ui/cube3d/CubeScene.kt` (arrow placement), the solve screen (celebration) and its tests. `cube`, `app` and `web` are untouched.
