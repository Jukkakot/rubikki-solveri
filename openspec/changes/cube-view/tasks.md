# Tasks

## 1. Cube-side helpers

- [x] 1.1 `FaceView` hold orientations (face order, rotation, centre and top colours) in `cube`; verify tests that each hold brings the face to the front with the net's top side up
- [x] 1.2 `CubeEditor` (nullable stickers, fixed centres, counts, complete, toCube, encode/decode); verify tests "Paint a sticker", "Centres are fixed", "Clear", "Too many of a colour"

## 2. 3D scene

- [x] 2.1 Quaternion/vector math and `CubeScene` (quads, move rotation, projection, culling, painter's order); verify tests "Default view" (exactly the 27 U/F/R stickers visible), animation midpoint rotates only the turned layer, rotation direction of R
- [x] 2.2 Hit test; verify test "Tap a sticker" (front centre) and that hidden stickers are never hit
- [x] 2.3 `Cube3D` composable (drawing, drag, tap, marked outline) and `CubeAnimator` (queue, durations, animator scale); verify Robolectric tests "Queued moves" and "Animated turn" end state

## 3. Screens

- [x] 3.1 Manual input screen (face grid, palette, hint, mini map, next/previous, 3D preview, counts, check with messages, clear/fill solved); verify Compose tests for painting, next face hint, unfinished, invalid and valid check
- [x] 3.2 Free cube screen (face buttons with prime toggle, scramble, undo, reset); verify a Compose test that tapping R then undo returns to solved
- [x] 3.3 Home entries and navigation; verify test "Open manual input"; update docs (architecture: 3D view, screens) and mark roadmap item 2 done
