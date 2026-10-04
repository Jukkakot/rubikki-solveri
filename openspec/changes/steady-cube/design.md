# Design

## Context

See proposal.md (Why). Today `GuideCube` (`ui/guide/MoveGuide.kt`) animates its `CubeViewState`
to `CubeScene.guideView(move)` whenever the presented move's face changes: front-right for U/F/R,
yawed for L, 148° round for B, tilted from below for D. `MoveWords` builds "Turn the X clockwise
(as seen from Y)". In learn mode `BeginnerSolver` prefixes most algorithms with `y`, `y2` or `y'`
(white corners, middle layer, yellow edges and corners); `MacroSearch.find` returns the first
depth with any hit, shortest by move count, so a `y` + trigger beats a `D` + trigger + `D'`-style
placement whenever it is shorter. Fast-solve solutions (min2phase) contain no rotations.

## Goals / Non-Goals

**Goals:** the on-screen cube never changes orientation by itself; hidden-side moves stay readable;
fewer real whole-cube turns in learn mode; every move's words readable from the front.

**Non-Goals:** changing the beginner method (white cross on top, z2 turn over stay); camera-based
orientation check in follow mode (later idea); avoiding the method's fixed turns.

## Decisions

### 1. One holding view

`CubeScene.guideView` is removed; `GuideCube` starts from `CubeScene.DEFAULT_VIEW` (the current
U/F/R guide view: yaw −32°, pitch 24°, so top, front and right show) and never calls `animateTo`
on its own. Camera follow's small guide cube uses the same `GuideCube`, so it follows.

### 2. Mirror cube

A second `Cube3D` inside the guide cube's box, about 34 % of its width, in the bottom corner on the
mirrored side's side (left for L, right for B and D), on a rounded surface-container card with a
small label ("Peili: takapuoli" / "Mirror: back"). It gets the same colours, `move`, `progress`,
`highlight` and `arrow` as the main cube, `draggable = false`, and a fixed view looking at that
side (L: yaw +90°, B: yaw 180°, D: pitch −90°, each with a slight tilt so two neighbours show),
drawn with `graphicsLayer { scaleX = -1f }` so it reads like a mirror. Shown while the presented
or animating move's face is L, B or D. Rejected: an arrow drawn along the hidden layer's visible
edge on the main cube (needs new arrow geometry; the mirror shows the real face and its arrow
with code that exists).

### 3. Words from the front

New strings per layer and direction (positional args unchanged in form):
`move_u_left` / `move_u_right` ("Käännä yläkerrosta vasemmalle" / "Turn the top layer to the left"),
`move_d_left` / `move_d_right`, `move_r_up` / `move_r_down`, `move_l_up` / `move_l_down`,
`move_f_cw` / `move_f_ccw`, `move_b_left` / `move_b_right` ("Käännä takapuolta niin, että sen
ylärivi liikkuu vasemmalle" / "Turn the back side so its top row moves to the left"), and
`move_half_<layer>` via the existing `move_half` with the side name. The mapping (U → left,
U' → right, D → right, R → up, L → down, B → left, F → clockwise) lives in a pure
`MoveWords.direction(move)` and is checked by a cube-model test: the sticker that moves is followed
through the move. Old `move_cw` / `move_ccw` / `from_*` strings are removed. Whole-cube turns keep
`move_whole_cube`.

### 4. Reset button

`CubeViewState` gets `isAt(target, tolerance)`; `GuideCube` shows a small round icon button
(`Icons.Filled.Refresh`-style rotate icon from material-icons-core, content description
"Palauta asento" / "Reset view") in the box's top-end corner when the view is off the holding view
by more than ~2°; tap → `animateTo(DEFAULT_VIEW)`. 48 dp touch target.

### 5. Rotation cost in the beginner search

`MacroSearch.find(start, macros, maxDepth, goal, cost)` explores all depths up to `maxDepth` and
returns the hit with the lowest `cost`, ties by length then by order. Cost = `8 ×` quarter
whole-cube turns (y = 1, y2 = 2) `+` face moves, so a rotation is used only when it saves more
than about eight face moves. The search space stays small (corners: 80 macros → 6 400 sequences
at depth 2; measured beginner solve today 16 ms in the browser). The test records the average
quarter rotations over 200 seeded random cubes before the change (constant in the test, measured
once on the old code) and asserts ≤ 60 % after, plus that every solution solves.

### 6. Whole-cube turn step

When `state.current` is a rotation, `MoveWordsText` shows two 28 dp colour dots (front, top
centres of `cubeAt(index + 1)`) with small labels "edessä" / "ylhäällä" next to the sentence.

## Risks / Trade-offs

- [Mirror flips left/right, which some find confusing] → it is labelled "Peili", and the main cube
  still highlights the layer; the words describe the turn from the front without needing the mirror.
- [Cost weight 8 chooses longer solutions] → learn-mode solutions get a few moves longer; the user
  prefers fewer regrips. Constant `ROTATION_COST` for tuning.
- [Screenshots of guide screens change] → expected; the gallery is not refreshed per change.
