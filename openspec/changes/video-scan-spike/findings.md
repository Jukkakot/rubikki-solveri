# Findings: video-scan-spike (2026-10-05)

Face finder `cube/.../scan/FaceFinder.kt`, harness `cube/src/jvmTest/.../VideoScanHarness.kt`
(run with `VIDEO_HARNESS=1 ./gradlew :cube:jvmTest --tests "*VideoScanHarness*"`; frames from
task 1.1, git-ignored). Frames 360×640 at 10 fps. Colours of each face found are named with the
cube's own centres as references; "exact" means all nine match a true face in some rotation.

## Numbers (final thresholds)

| | 151828: free angles, fingers | 151903: straight on, table |
|---|---|---|
| frames | 250 | 186 |
| frames with ≥1 full face | 79 % | 77 % |
| full faces per frame | 1.03 | 0.80 |
| frames with 2 / 3 faces | 58 / 1 | 5 / 0 |
| faces missing 1–2 stickers (in frames) | 223 (180) | 54 (50) |
| faces read exactly right | 232 / 258 (90 %) | 141 / 149 (95 %) |
| … one sticker wrong | 10 | 1 |
| … wrong lattice (≤ 6 right) | 15 | 6 |
| stickers right, all faces | 96 % | 97 % |
| true faces seen exactly | 6 / 6 | 6 / 6 |
| cube assembled = true state | **yes** | **yes** |
| JVM ms per frame (desktop) | 17 | 13 |

A wrong reading never repeats for long: the same wrong nine colours appear in at most 2 frames for
a wrong lattice and 5 frames for a one-sticker misread, while right readings repeat up to 36
frames. Voting over frames therefore separates them cleanly.

## What made the difference (tuned once, on both videos together)

- **Blob growth bounded by the blob's mean colour** (`meanWithin`), not only by the neighbour
  step: before it, stickers on the cube's far edge leaked through a thin lit rim into the wall and
  were dropped, so no frame ever had two full faces (0 → 58).
- **Lattice checks across the cube's edge:** a lattice that takes one row from the next face is
  rejected by how much of a step neighbouring stickers span (`minSpan`), and among lattices that
  share stickers the most compact one wins (`compactness`, catches lattices sheared along a
  diagonal). Wrong lattices on the angled video 26 → 15.
- **Perspective:** the edge places allow a longer step along the axis (`stretch`) than sideways.
- Tried and dropped: shrinking the light areas before growth (worse), a size-ratio rule for
  neighbours (lost real faces). Thresholds near the chosen ones change the numbers little; only
  `darkBelow` ≥ 80 breaks dark blue/red stickers.

## Where it fails (frames under `frames/overlay/<video>/`)

- **Wrong lattice on a steep third face** (151828 f0185–f0189): the narrow left face plus part of
  its neighbour read as a face; 1–2 frames each.
- **Lattice sheared onto the next face** (151828 f0051, f0060, f0069): a column of one face and a
  diagonal step onto the top face.
- **One sticker misread** (151828, the U face 5 frames): a colour at the edge of the light.
- **Face not found:** a finger over a sticker (the 7–8 partial faces), steep views where the far row
  is very thin, motion blur during fast turns.

## Proposal: **go**

Both videos give the true cube from the finder's output alone, every face is read exactly in many
frames, and corner views show two faces in about a quarter of the angled video's frames (enough
for the faces' rotations and the pose). For `video-scan`:

- Feed full lattices only (as planned); a reading counts once the same colours (in any rotation)
  come in **≥ 3 frames**; wrong readings above never lasted longer than 2 (5 for one sticker),
  so per-sticker votes with a clear margin are safe.
- Partial faces (223 on the angled video) are plentiful: worth adding later for the sticker-by-
  sticker input, not needed for the first version.
- Speed: ~15 ms per frame on the desktop JVM; a phone is a few times slower, still well within
  10 fps. Measure in the browser before deciding on a Web Worker.
- Hints should steer towards corner views (two faces) and away from very steep angles.

The user decides the next step (`video-scan` or more test material first).
