# Proposal

## Why

Browser recording `web_20261009_100824` (2026-10-09, committed in `cube/src/jvmTest/resources/recordings/`)
never finished, although every followed face's colours matched the real cube (8/8). The scanner
settled each face's turn wrong in the first pictures. It then judged every face against the other
settled faces, so the wrong turns propped each other up. One face alone could never turn right,
because its right turn looked wrong against its wrong neighbours. All of them would have had to
turn together, so the scan stayed "almost a possible cube" and stalled. A morning video showed the
same lock (the white face 180° wrong).

The user's principle: every interpretation can be wrong, nothing may lock, and wrong observations
must be found as fast as possible.

## What Changes

- Whenever the cube is not clear and a new face reading arrives, the scanner rechecks everything
  together from the start. It takes the faces' own readings and tries every combination of turns
  for the faces named by their centres (at most 4⁶ = 4096), and every naming of the faces still in
  doubt. When a combination makes a clearly better cube, it replaces the current one at once, even
  for faces that were settled or confirmed with a tick.
- (Dropped in implementation: a disagreement score per followed face, weighing down the faces that
  disagree most. The recheck alone fixed the recording and no fixture needed it; kept in the backlog.)
- The correction is silent. Stickers and ticks just change to the better cube, and a tick may go
  and come back.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: new requirement "Nothing locked": interpretations are rechecked together, and a
  better whole wins over settled parts.

## Impact

- `cube`: `FaceTracks` (settling and re-opening, a whole-cube recheck reusing the turn search from
  `RotationSearch`/`BestCube`, a disagreement score per track). `VideoScan` only consumes it.
- Tests: the stuck recording must finish with the true cube. Every existing fixture and the
  acceptance harness must keep their finishes (no wrong cube, no slower finish beyond the stored bar).
  A budget test keeps the per-picture time in check.
