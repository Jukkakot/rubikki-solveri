# Design

## Context

See proposal.md (Why). Today `GuideCube` (`ui/guide/MoveGuide.kt`) animates its `CubeViewState`
to `CubeScene.guideView(move)` whenever the presented move's face changes: front-right for U/F/R,
yawed for L, 148° round for B, tilted from below for D. `MoveWords` builds "Turn the X clockwise
(as seen from Y)". `SolveScreen` knows the method (`SolveMethod.FAST` / `LEARN`) and shows
`GuideCube(state)` in the guide and, through `FollowPanel`, in camera follow. The timer's guided
scramble and the practice screen also run through `SolveScreen` (scramble: fast; practice: learn).
Fast-solve solutions contain no whole-cube turns.

## Goals / Non-Goals

**Goals:** in the fast method the on-screen cube never changes orientation by itself; hidden sides
partly visible through a mirror; words that fit the holding view; a way back after dragging.

**Non-Goals:** any change to the learn method (view, words, whole-cube turns, practice) or to
lessons; changing the highlight; checking the real cube's orientation with the camera (backlog,
after the user has tried camera follow).

## Decisions

### 1. `GuideCube(state, steady: Boolean, mirror: Boolean)`

`SolveScreen` passes `steady = method == FAST`; the guide passes `mirror = steady`, `FollowPanel`
`mirror = false`. Steady: the view starts at `CubeScene.DEFAULT_VIEW` (the current U/F/R guide
view: yaw −32°, pitch 24°) and nothing animates it except the reset button. Not steady: today's
`guideView` behaviour, unchanged.

### 2. Mirror

A second `Cube3D` in the guide cube's box, bottom-start corner, about 34 % of the box width, on a
small rounded `surfaceContainerHigh` card with the label "Peili" / "Mirror" (`mirror_label`). Same
colours, `move`, `progress`, `highlight` and `arrow` as the main cube; `draggable = false`; fixed
view from behind and the left, a little from above (yaw 148° + the holding tilt, pitch 24°: back,
left and top visible), drawn with `graphicsLayer { scaleX = -1f }` so it reads like a mirror
(constant `MIRROR_VIEW`, tunable after the user tries it). The main cube keeps its full size; the
mirror overlaps the box's empty corner (the cube's projection leaves the corners free).

### 3. Words for the holding view (fast method only)

A pure `MoveWords.steadyParts(move)` gives the sentence key and words; `moveDescription(move,
after, steady)` picks it when `steady`. New strings in both languages:
`move_top_left` / `move_top_right` ("Käännä yläkerrosta vasemmalle" / "Turn the top layer to the
left"), `move_bottom_left` / `move_bottom_right`, `move_right_up` / `move_right_down`,
`move_left_up` / `move_left_down`, `move_front_cw` / `move_front_ccw`, `move_back_left` /
`move_back_right` ("Käännä takapuolta niin, että sen ylärivi liikkuu vasemmalle" / "Turn the back
side so its top row moves to the left"), and half turns through the existing `move_half`
("Käännä yläkerrosta puoli kierrosta"). Mapping: U → left, U' → right, D → right, D' → left,
R → up, R' → down, L → down, L' → up, B → left, B' → right, F → clockwise. A cube-model test
follows a front or top sticker through each move to confirm the direction. The learn method keeps
the current strings.

### 4. Reset button

`CubeViewState.isAt(target)` (angle between the quaternions under 2°). In steady mode a 40 dp
round tonal icon button with a 48 dp touch target sits in the box's top-end corner while the view
is off the holding view; icon: a curved "rotate back" arrow drawn as a vector resource in
`composeResources/drawable` (material-icons-core has no suitable one); content description
"Palauta asento" / "Reset the view" (`reset_view`). Tap → `animateTo(DEFAULT_VIEW)`.

## Risks / Trade-offs

- [Mirror flips left/right, which can confuse] → labelled "Peili"; the main cube and the words carry
  the instruction; angle and size tuned after the user's try.
- [Two wordings (fast, learn)] → each fits its own view (the learn view still swings to the side it
  names); documented in docs/architecture.md.
- [Fast-method guide screenshots change] → expected; checked by eye once.
