# Design

## Context

`cube-view` draws the cube; `fast-solve`'s stepper drives it. This change adds presentation
layers to `Cube3D` and wraps them in a `MoveGuide` composable reused later by the beginner solver
and lessons.

## Goals / Non-Goals

**Goals:** a beginner can tell layer and direction at a glance, for every face including back,
left and bottom.

**Non-Goals:** following the real cube's physical orientation with sensors or the camera
(`camera-follow`); slice/rotation arrows beyond a simple ring (the solvers produce face turns).

## Decisions

- **Highlight**: each quad remembers its home cubie; when a highlight move is set, quads of cubies
  outside the move's layer are mixed 55 % towards a neutral grey and darkened. Plastic stays dark.
- **Arrow geometry** (plain Kotlin, tested): an arc in the plane of the turning face, centred on
  the axis 1.56 out from the cube centre (just above the stickers), radius 1.05; the arc's middle
  points towards the camera's projection onto the face plane so it sits on the visible part of the
  face; sweep = the move's full angle (±90° or 180°), traced in the move's rotation direction, so
  clockwise from the axis tip looks clockwise on screen. Projected with the same camera; drawn last
  with a dark outline and an amber (#FFB300) stroke and a triangular head. Hidden while
  `progress > 0`.
- **View targets** keep the hold and only change the camera: U/F/R and slices: yaw −32°, pitch 24°
  (default); L: yaw +32°; B: yaw 148° (behind, from the left); D: pitch −24°. Implemented as
  `CubeScene.guideView(move)`; slerp 450 ms.
- **Demo**: on a new step, wait 500 ms, play the move, wait 700 ms, snap back (shared code with
  "show"). Skipped when the phone's animations are off.
- **Haptics**: `HapticFeedbackType.Confirm` on done, `SegmentTick` at the end of a demo.
- **Notation setting**: DataStore boolean `show_notation`, default false; settings switch;
  passed to `MoveGuide`.

## Risks / Trade-offs

- [The arrow could hide stickers the user needs] → thin enough (5 % of the canvas) and only
  over the turning face; checked on screenshots and on the phone.

## Decisions made while building

- The stepper's logic moved into `StepperState` (testable with the Compose test clock); the
  solution screen only lays it out.
- Dimming is 60 % towards grey (screenshots: the highlighted layer reads clearly in light and dark).
