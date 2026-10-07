# Proposal

## Why

At the start of a video scan nothing happens for about five seconds although the cube is clearly
in the picture (user's screen recording and log, 2026-10-07, Samsung Internet). The log shows the
camera and the worker ready within 0.6 s; the wait is the exposure lock: no picture is read until
the camera is locked, and the lock waits for the metering point to stay put for 0.6 s. The point
follows the largest face, which jumps between the two faces in view as the cube turns in the hand,
so every jump restarts the wait. In that scan no darkening was even needed (`darker=0`).

The user's goal: the scan as fast as possible, everything in real time, as little waiting as
possible.

## What Changes

- **No picture is held back for the camera.** Pictures are read from the first face on, also while
  the camera meters, steps darker or re-meters after a torch change; washed-out readings already
  count little and uncertain ones leave the decision to the rest of the cube.
- The camera is still locked, in the background, as soon as metering settles: the metering point
  follows the cube as a whole (the middle of all faces found), not the largest face, and the lock
  comes at the latest about a second after the first face, however the cube moves. Darkening steps
  for washed-out stickers still run (only when the stickers read washed out).
- While a face is found but no sticker has been read yet, a small spinner shows on the picture (no
  text). The "show the cube" text stays for when no cube is in view.
- The phone app gets the same fix (the logic is shared); the browser was where it was noticed.

Decisions (light lane, no design.md):
- Why lock at all (user asked): free auto exposure and white balance change with whichever face
  fills the picture (a white face darkens it, a blue one brightens it), so the same red would read
  differently on different faces; the scan compares stickers across the whole cube. The lock keeps
  one colour scale; re-metering when stickers wash out (and on torch changes) keeps it adaptive.
- Readings taken before the lock count like any other; the risk (slightly different exposure in
  the first second) is covered by soft votes and the cube deciding, and checked against the
  existing regression videos, the dim ones included.
- Time limit about 1 s from the first face; a constant in the cube module, tuned on the phone if
  needed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Camera set for the cube" (read during metering, cube-wide point, time limit,
  spinner) and "Reading in different light" (a torch change no longer pauses reading).

## Impact

- Modules: `cube` (exposure control: frames always read, metering point and time limit) and
  `shared` (spinner on the video scan). No change in `app` or `web`.
- Docs: none beyond the roadmap row (the camera-exposure notes point to the code).
