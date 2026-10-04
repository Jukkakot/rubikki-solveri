# Proposal

## Why

Following a solve on the real cube, the user loses time after every move of the left, back or
bottom side: the guide's 3D view swings round to show that side (the back almost half a turn),
which looks exactly like the cube being turned, so the user has to work out the orientation again
each time. In learn mode the solver also asks for many real whole-cube turns (a y before most
algorithms), with the same cost. The user's top priority: the cube on screen never changes
orientation by itself, and the hands turn the whole cube as rarely as possible.

## What Changes

- **Steady view:** the guide cube always stays in the holding view (front, a little from the
  right and above), in both solve modes and in camera follow. The view no longer swings for left,
  back or bottom moves.
- **Mirror:** while a move of a side the view cannot show (left, back, bottom) is presented, a
  small mirror cube next to the main cube shows that side as a mirror would, with its highlight
  and arrow. The main cube still highlights the turning layer.
- **Words from the user's side:** moves are described as seen from the front ("Turn the top layer
  to the left", "Turn the right side up", "Turn the back side so its top row moves left"); only
  front turns keep clockwise/counter-clockwise.
- **Reset orientation:** the user can still drag the cube freely; a button then appears that
  turns it back to the holding view. Nothing turns the view automatically.
- **Fewer whole-cube turns in learn mode:** the beginner solver weighs every whole-cube turn as
  costly when it picks the next algorithm placement, so pieces are taken from where the cube
  already is (turning the top or bottom layer instead) and the hands turn the cube far less often.
  The method itself (white cross on top, turning over for the middle layer) stays.
- **Clearer whole-cube turn:** when one is needed, the step shows the new front and top centre
  colours as big colour dots next to the words.

Modules: `cube` (beginner search cost), `shared` (guide view, mirror, words, reset button, turn
step), tests in `cube` and `app`.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `move-guide`: the view stays in the holding position (replaces "View follows the move"); mirror
  for hidden sides; reset button after dragging; whole-cube turns shown with centre colours.
- `fast-solve`: moves in words are described from the user's side instead of "as seen from".
- `beginner-solver`: whole-cube turns are kept to a minimum.

## Impact

- `cube/.../beginner/BeginnerSolver.kt` (`MacroSearch` cost), its tests.
- `shared/.../ui/guide/MoveGuide.kt`, `ui/cube3d/CubeScene.kt` (`guideView` goes), `ui/common/
  MoveWords.kt`, strings in both languages, `FollowPanel` (uses the guide cube).
- `app` tests: `MoveWordsTest` / `BeginnerTextsTest` expectations, guide screenshots change.
