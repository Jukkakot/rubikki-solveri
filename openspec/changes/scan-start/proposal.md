# Proposal

## Why

At the start of a video scan nothing happens for about five seconds although the cube is clearly
in the picture (user's screen recording and log, 2026-10-07, Samsung Internet). The log shows the
camera and the worker ready within 0.6 s; the wait is the exposure lock: no picture is read until
the camera is locked, and the lock waits for the metering point to stay put for 0.6 s. The point
follows the largest face, which jumps between the two faces in view as the cube turns in the hand,
so every jump restarts the wait. In that scan no darkening was even needed (`darker=0`).

## What Changes

- The metering point follows the cube as a whole (the middle of all faces found), not the largest
  face, so it no longer jumps between faces.
- Metering has a time limit: the camera is locked at the latest about a second after the first
  face is found, however the cube moves. Darkening steps for washed-out stickers still run as before
  (they only happen when the stickers read washed out).
- While the camera adjusts, the status on the picture says so ("Säädän kameraa…") instead of
  "Näytä kuutio kameralle", so the user knows the cube was seen.
- The phone app gets the same fix (the logic is shared); the browser was where it was noticed.

Decisions (light lane, no design.md):
- Pictures are still not read while the camera adjusts; the time limit alone brings the first
  reading to about a second, and reading during adjustment would mix exposures in the votes.
- Time limit about 1 s from the first face (600 ms settle plus slack); a constant in the cube
  module, tuned on the phone if needed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Camera set for the cube" gets the time limit, the cube-wide metering point and the
  adjusting status.

## Impact

- Modules: `cube` (exposure control: metering point and time limit) and `shared` (status text
  fi/en on the video scan). No change in `app` or `web`.
- Docs: none beyond the roadmap row (the camera-exposure notes point to the code).
