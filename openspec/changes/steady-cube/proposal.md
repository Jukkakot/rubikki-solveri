# Proposal

## Why

Following the shortest solution on the real cube, the user loses time after every move of the
left, back or bottom side: the guide's 3D view swings round to show that side (the back almost
half a turn), which looks exactly like the cube being turned, so the user has to work out the
orientation again each time. The user's top priority: in the shortest-solution guide the cube on
screen never changes orientation by itself.

## What Changes

In the shortest-solution guide (the "fast" method, including the timer's guided scramble):

- **Steady view:** the guide cube always stays in the holding view (white on top, green in front,
  seen from the front a little from the right and above). It no longer swings for left, back or
  bottom moves. The turning layer stays highlighted as today.
- **Mirror:** a small mirror cube, always shown next to the guide cube, shows the cube from behind
  and the left as a mirror would, so part of the back is visible too. A nice-to-have aid; its
  angle and size may be tuned after the user has tried it.
- **Words for the steady view:** moves are described as seen in the holding view ("Turn the top
  layer to the left", "Turn the right side up", "Turn the back side so its top row moves to the
  left"); front turns keep clockwise / counter-clockwise.
- **Reset orientation:** the user can still drag the cube freely; a button then appears that turns
  it back to the holding view. Nothing turns the view automatically.
- **Camera follow** in the fast method: its small guide cube also keeps the steady view, without a
  mirror.

Unchanged: the learn method (its view, words, whole-cube turns), lessons, the highlight.

Modules: `shared` (guide view, mirror, words, reset button, strings), tests in `app`.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `move-guide`: in the fast method the view stays in the holding position, with a mirror and a
  reset button; the learn method keeps the view that follows the move.
- `fast-solve`: moves in words are described as seen in the holding view.

## Impact

- `shared/.../ui/guide/MoveGuide.kt` (`GuideCube`, `MoveWordsText`), `ui/guide/FollowPanel.kt`,
  `ui/solve/SolveScreen.kt` (passes the method), `ui/cube3d/Cube3D.kt` (`CubeViewState`),
  `ui/common/MoveWords.kt`, strings in both languages.
- `app` tests: new Compose tests; `MoveWordsTest` keeps the learn-method wording, new tests for the
  steady-view wording; guide screenshots of the fast method change.
