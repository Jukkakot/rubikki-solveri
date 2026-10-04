# Proposal

## Why

In the phone's browser the solution screen is taller than the screen (about 560 dp of height
under the browser's bars): the user has to scroll down to press "Tein sen" after every move. The
phone app has more height but the same problem on shorter phones and in landscape.

## What Changes

- **Solution screen** (both methods, also the timer's guided scramble and practice): the step
  text, the move in words and the button row are always visible; the guide cube (with the mirror)
  takes the height that is left and shrinks to fit. The camera-follow panel does the same with
  its camera view.
- **Free cube screen:** the turn buttons and "Ratkaise" stay visible; the cube shrinks.
- On a screen so short that the cube would get very small (landscape phone), the screen scrolls as
  today instead of shrinking the cube further (minimum cube height, see Decisions).
- Settings, About, the timer, lessons and the home screen keep their current layout.

Decisions (light lane, no design.md):
- Layout: the screen becomes a fixed column (no scroll); the cube area gets the leftover height
  (`weight(1f)`) and the cube is drawn as large as fits in it, centred. Under 160 dp of leftover
  height the old scrolling layout is used (cube at full width).
- The learn method's stage card stays above the cube; the goal card (stage start) is unchanged.

Modules: `shared` (SolveScreen stepper, MoveGuide, FollowPanel, FreeCubeScreen); tests in `app`.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `app-shell`: new requirement that screens with a main action fit the screen without scrolling.

## Impact

- `shared/.../ui/solve/SolveScreen.kt` (Stepper), `ui/guide/MoveGuide.kt` (`GuideCube` sizing),
  `ui/guide/FollowPanel.kt`, `ui/free/FreeCubeScreen.kt`.
- Screenshots of the solve screen change (smaller cube on short screens).
