# Design

## Context

`camera-scan` gives the camera preview, the grid and per-cell readings; `move-guide` gives the
stepper and the 3D guide. Only the front face is visible to the camera, and the user holds the
cube in the standard way (green front, white top), so the expected front face before and after
each move is known from the cube model.

## Goals / Non-Goals

**Goals:** hands-free advance for every move that changes the front face; clear handling of back
turns and mistakes.

**Non-Goals:** tracking the cube anywhere in the frame or in 3D, reading more than one face at a
time (stretch goal in product.md, not now).

## Decisions

- **Expectations from the model**: `front(cube)` = the nine colours of F. For the current move m
  from state S: `before = front(S)`, `after = front(S·m)`. `visible = before != after`.
- **Matching with tolerance**: a frame matches a front if at least 8 of 9 cells agree (one misread
  allowed), and it must agree better with `after` than with `before`. Stable for 3 frames → done.
  For mistakes, the candidates are all 18 face turns from S: a stable unique best match to
  `front(S·x)` with x ≠ m (and ≠ no move) reports `WrongMove(x)`; the fix is x⁻¹.
- **Centre guard**: the centre must read as the front centre's colour, otherwise "keep green
  towards the camera".
- **Calibration** (`LiveCalibration`): references start at the default palette; whenever a stable
  frame matches `before` or `after` with ≥ 8 cells, each matching cell's Lab moves the reference of
  its known colour 30 % towards it. Reading = nearest calibrated reference.
- **Front arrows** (`FrontArrow.of(move)`, pure): grid coordinates 0..1 (x right, y down) seen on
  the front. U row 0 left (U' right); D row 2 right (D' left); R column 2 up (R' down); L column 0
  down (L' up); wide turns like their face; M column 1 like L, E row 1 like D; F/f a round arc
  clockwise or counter-clockwise; B, S and rotations: none. Half turns: same arrow, `double = true`.
- **UI**: camera mode is a switch in the solution screen's top bar (saved across rotation),
  sharing `StepperState`. The preview is the scan's `CameraPreview`; the overlay draws the grid,
  live dots, and the arrow (amber, dark outline as in the 3D guide); the 3D `GuideCube` sits in
  the bottom-right corner at 30 % width. The camera permission flow is shared with the scan
  (`CameraPermissionGate`).
- **After done** the tracker resets its streaks; a manual "done" also works in camera mode.

## Risks / Trade-offs

- [Misreads advance too early] → 8/9 cells, better than `before`, 3 stable frames; worst case the
  user taps previous.
- [Turning hands cover the grid] → frames without the right centre are ignored.

## Decisions made while building

- Calibration learns from every cell of a frame that matches a known front with at least 7 of 9
  cells (not only the agreeing ones), otherwise a colour misread from the start could never be
  corrected. A move changes at least three front cells, so such a frame is unambiguous.
- `follow.event` log lines record each non-waiting event with the move, for tuning.
- The corner guide cube is 30 % wide so it does not cover the grid.
