# Proposal

## Why

The video scan decides which face it sees by how the centre looks, and only at the end checks the
cube against what a real cube can be. When two centres look alike (red read as orange, blue in
shadow as white, a striped pattern), faces get mixed before any rule can act; the spike
`corner-scan-spike` measured it: with look-alike centres today's scan finished right in 1 of 11
recordings and once finished wrong. Rules brought in early (corner handedness, neighbours never
opposite colours) already finished two more, with no wrong cube.

The user's model (2026-10-07): **start from every cube that is possible and keep crossing out what
the observations rule out**, using every safe fact about a real cube, so the scan stays as easy as
turning the cube in the hand: no guidance, no asking the user about colours.

## What Changes

- **The scan keeps the set of cubes still possible.** Not as a list of cubes but per piece place:
  which pieces, turned which way, can still sit there; pieces used once, the corners' twist, the
  edges' flip and the permutation parity keep the places together as real cubes. Every reading
  narrows it; readings that disagree weigh against, a single one never crosses out alone.
- **Which face a reading shows is part of that set**, not decided first by its centre's look: a
  reading's face and turn stay open among those the rules allow, narrowed by
  - faces seen together: different faces, neighbours (never opposite colours), touching along the
    edges the picture shows, which also fixes their turns and a corner's handedness;
  - the same face over consecutive pictures (the cube does not jump);
  - the stickers it shows against the pieces still possible;
  - its centre's look, as soft evidence only.
- **The scan finishes when one cube is left clearly** (as now, about half a second), and never
  shows a sticker as known that a possible cube could still have otherwise.
- **Built beside today's scan**, measured on all recordings (also with look-alike centres) and
  switched over only when it is never wrong and finishes at least as often; then today's piling by
  look goes.
- What the user sees stays the same: progress ring, grey veils and dots, stall notice, finish.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Recognised by agreement" (faces known by rules and possible cubes, not by centre
  look first; never a cube that breaks a rule) and "Finish the video scan" (one possible cube left).

## Impact

- `cube`: a new scan core (possible pieces per place, face hypotheses per reading, the joint
  narrowing), `CornerReader` reused, `BestCube` reused or folded in; `VideoScan` keeps its public
  state so `shared` (screen, paint) does not change. `CornerScanHarness` grows into the acceptance
  harness.
- Performance: one frame's narrowing must fit the phone and the browser (Web Worker) next to the
  finder (~40 ms a frame today).
- No new dependencies.
