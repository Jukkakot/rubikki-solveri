# Proposal

## Why

Browser scans of the striped cube on 2026-10-12 (version 1.0.410, the user's own phone):

- 10:01:43: stuck for 29 s, then **finished with a wrong cube**: every side half round (`GRB…` for `BRG…`), the
  striped cube's mirror image. The same cube scanned right just before and after. A wrong cube sends the solution
  astray, which is worse than a stall. Its recording was not shared (only the newest three are kept).
- 10:02:33: stuck for 43 s, left by the user (recording `web_20261012_100233`). The cube was never changed during
  the scan, yet the finder kept reporting faces that the cube does not have: a 3×3 lattice with one row from the
  next face across the cube's edge (`WWW|GRB|.RB`, rows of the white top over a side). Its steps are uneven (20 and
  28 px in one lattice). Followed as faces, they gave the scan pieces no real cube has, and it never got clear.

The striped cube only exposes these: a side whose rows all look alike makes a lattice shifted by one row as
believable as the real one. Both weaknesses are general (user 2026-10-12: fix them for every cube, no
pattern-specific check). Offline, the half-round mirror also happened before: `202156` and `202058` finished as
their mirror in some reference modes (`scan-warm-references` Findings), and the fake-camera runs of `web_181940`
did so in 3 of 16 runs (`scan-send-ahead` Findings).

## What Changes

- **Face finder:** a lattice that bends over the cube's edge is not a face. The nine stickers of a face lie on
  one plane, so their steps change only gently across the lattice; a lattice whose row or column steps or sticker
  shapes jump between its inner and outer row is rejected. Today's `minSpan` check (a row of the next face seen
  flatter) stays.
- **Finish:** the scan does not finish with a cube that every side turned half round (or another way of turning
  the faces) fits nearly as well, until two faces seen together in one picture tell them apart. First the mirror
  finish is reproduced offline and its cause found; the fix follows the cause (design 2).
- Test material: the recording `web_20261012_100233`; the striped and plain stills and recordings already in the
  repo; every source checked so that nothing finishes later or wrong where it did not.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: *Scan from video* says a lattice across a cube's edge is not a face; *Finish the video scan* adds
  a scenario: two cubes alike but for every side's half turn do not finish until a view tells them apart.

## Impact

`cube` only: `FaceFinder` (commonMain), `FaceTracks` finish checks (commonMain), tests and harnesses (jvmTest),
recording fixture. `app`, `shared` and `web` use the shared code unchanged. A phone check with the striped cube is
listed for the user.
