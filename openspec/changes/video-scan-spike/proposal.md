# Proposal

## Why

The user wants to scan by just turning the cube in front of the camera, the app working out the
state bit by bit from the video, with no separate captures (the current guided scan stays as an
option) and a visual sign of what is recognised already. That needs finding the cube's faces
anywhere in a frame, also at an angle, which the app cannot do today (it reads a fixed grid). This
spike finds out offline, on the user's own videos, whether that works well enough before anything
is built into the app.

## What Changes

- A face finder in pure Kotlin (`cube` module, so it can later run on the phone and in the
  browser): finds 3×3 sticker lattices anywhere in a frame, also seen at an angle, and reads their
  nine colours.
- An offline harness (JVM test, not part of the app): runs the finder on frames extracted from the
  test videos, assembles the cube from what it saw, and reports numbers against the true state.
- A short findings note in the change (`findings.md`) with the numbers and a go / no-go proposal
  for the live feature. **No change to the app's behaviour.**

Test material (2026-10-05, `testdata/video/2026-10-05/`): `20261005_151828.mp4` (25 s, free
angles, 2–3 faces at a time, fingers in view), `20261005_151903.mp4` (19 s, faces straight on, cube
on a table); true state from the scan right after, in `scan-log.txt` (12:19:57Z):
`YWRBWWGYRWGYWRGBOWWRBGGBWBYGROOYYRGBOYROORGBOBRGYBWOOY`.

## Capabilities

### New Capabilities

### Modified Capabilities

None: a spike, no requirement changes (`skip_specs`).

## Impact

New code in `cube` (face finder) and a JVM test harness; frames extracted with ffmpeg into
`testdata/video/<date>/frames/` (git-ignored). `app`, `shared`, `web` untouched.
