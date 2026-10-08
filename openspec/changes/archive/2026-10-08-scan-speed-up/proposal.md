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
- no committed timing harness: the measuring test was temporary

## Decisions
- **Finder, measured on 100 phone stills of `web_121505` (360×600, JVM):** blobs 14.8 ms and lattices 8.7 ms
  before, 8.3 and 3.1 ms after (23.5 → 11.5 ms a frame).
  - Blobs: the flood fill visits neighbours in a loop, not a local function, and the median colour sorts packed
    primitives instead of boxed lists.
  - Lattices: about a thousand fits a frame, 97 % of them failing. A fit compares squared distances (`hypot`
    was the cost) and stops when three edges are missing. Only blobs within reach of the centre are tried.
  - The regenerated fixtures are byte for byte the same.
- **Rules scanner, per frame, looped ×3 with the same results (hash of every frame's stickers and flags):**
  `202058` 7.2 → 6.0 ms, `web_084657` second scan 7.5 → 6.5 ms. Each face's votes are worked out once per
  turn, in the same order as before, so the sums are exactly equal.
  - "Skip when no new readings" was left out: it could change answers.
- **Log:** besides the paint, the scan's own time per picture went in too (`scanMs`, `paintMs`): with the
  finder's it shows the whole frame. The paint is timed while it is worked out and drawn; the GPU's part is
  not visible to the app.
