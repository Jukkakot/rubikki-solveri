# Proposal

## Why

Looking back and forth between the phone and the cube is the hardest part of following a
solution. With the cube in front of the camera, the app can draw the arrow on the real cube,
notice when the move is done and move on by itself — the user's eyes stay on the cube.

## What Changes

- A camera mode in the solution screen: the live camera with the same 3×3 grid as the scan; the
  user keeps the green centre facing the camera and white on top.
- The current move is drawn on the real front face as a 2D arrow (rows move left/right, columns
  up/down, the front face round), with "2×" for half turns; a small 3D guide cube in the corner
  shows the whole move, including back turns that cannot be seen from the front.
- The app reads the front face's colours and advances when they match the cube after the move
  (stable for a moment), with a vibration. A turn the other way or of another side is noticed and
  named, with the turn that undoes it.
- Moves that do not change the front face (back turns, or rare cases where the colours happen to
  stay the same) wait for the "done" button, and say so.
- Colour reading calibrates itself on the known cube colours while following.

## Capabilities

### New Capabilities
- `camera-follow`: following the solution with the cube in front of the camera.

### Modified Capabilities
(none)

## Impact

- `cube`: new `follow` package (front-face expectations, move detection, front arrows, live
  calibration) — pure Kotlin.
- `app`: shared camera permission gate (scan and follow), camera mode in `SolveScreen`, overlay
  drawing.
