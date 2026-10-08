# Proposal

## Why

The third phone test ran at about 11 pictures a second. The face finder took 46 ms of each picture. Late in a long
scan, the rules scanner's own work took about 9 ms (JVM), 5 of it in `settleTurns`. The user asked for those
speed-up ideas of `scan-rules-finish` that pay most for their effort (2026-10-08).

## What Changes

- **Face finder:** faster, with exactly the same result (same faces, same partial faces).
  - First measure where its time goes (blobs or lattices), then speed up that part.
  - The committed fixtures, regenerated from the stills, stay byte for byte the same.
- **Rules scanner:** `settleTurns`, `turnsClear` and `leadingOf` build the evidence for a trial turn from per-face
  vote sums computed once (one sum per face and extra turn), not from every reading per trial. The answers stay
  the same.
- **Log:** the paint's time per frame goes into the log's snapshots beside the finder's, so it can be judged next.
- **Left out, kept as ideas:**
  - Reusing `BestCube.solve` between frames: saves about 2 ms, but risks a stale cube.
  - Searching only near the last faces: changes what is found, so it needs phone tuning.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. The change is about speed only and no behaviour changes (`skip_specs`). The log's extra field is a tuning aid.

## Impact

- `cube/scan/FaceFinder.kt`
- `cube/scan/FaceTracks.kt`
- the video scan screen's log (`shared/.../ui/scan/VideoScanScreen.kt`, `VideoScanLog`)
- a timing harness in `cube/src/jvmTest`

## Decisions
