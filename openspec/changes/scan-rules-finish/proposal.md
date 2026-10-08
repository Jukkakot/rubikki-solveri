# Proposal

## Why

Third phone test (web, 1.0.301, 2026-10-08 09:10–09:15 UTC, the striped cube; log and screen
recording in `testdata/video/2026-10-08c/`): four rules scans and none finished. The first knew the
true cube in 20 s (09:11:17, margin 3.6) and then waited 30 s until "stuck". The look scanner finished
one of three.

- **Never finished with the cube known:** `complete` must hold for half a second (`FINISH_MILLIS`),
  and every new face track that is not settled yet (`unsureFaces`, `settledFor`) revokes it. A cube
  turned in the hand gives a new track every few frames, so the half second never comes; the log
  shows "every side done" three times in four seconds. The spec already says the scan shall never
  stay with everything read and nothing happening.
- **Slower and slower:** the rules scanner went from 14 to 5 pictures a second while about 30 face
  tracks piled up; tracks are never dropped and the per-frame work goes over all of them (pair
  costs, tables, rechecks). Slow reading makes the scan feel sticky and makes the first problem
  worse.

The user wants the hardest cube (striped) to scan well, and after this fix ideas for making the
algorithm faster.

## What Changes

- Once the cube is clear, a face newly in view does not hold the finish back unless it reads
  against the cube (more than one sticker otherwise in its best way); the half second keeps
  counting. A face that does read against it, or a drop in clearness, still revokes it.
- Face tracks that ended do not cost per-frame work forever: an ended short track that never
  settled is dropped, and the per-frame work runs over a bounded number of tracks per face (the
  rest kept only as the votes they already gave). The harness results must not get worse.
- The camera part of today's screen recording becomes a fixture (never wrong), plus a synthetic test
  for the finish: a clear cube followed by a new face track finishes.
- After the fix: measure where a frame's time goes and bring the user a short list of speed-up
  ideas (no code for them in this change).

## Capabilities

### Modified Capabilities
- `video-scan`: the finish is not held back by a new face that fits the clear cube; reading stays as
  quick late in a long scan as at its start.

## Impact

`cube/scan/FaceTracks.kt`, `VideoScan.rulesFrame`, possibly `Tracks.kt`; tests and fixtures in
`cube/src/jvmTest`.
