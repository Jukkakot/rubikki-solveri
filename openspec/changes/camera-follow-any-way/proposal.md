# Proposal

## Why

Camera follow only works when the cube is held one fixed way (green to the camera, white on top)
with the front face inside a grid. Real solving does not go like that: the user turns the cube in
their hands, and follow then stalls on "hold the green centre towards the camera". The user chose
(backlog item "camera follow", option b) that follow keeps helping however the cube is held.

## What Changes

- Camera follow finds the cube anywhere in the picture and from any side, the way the video scan
  does; the fixed grid and the "hold green to the camera" rule go away.
- The move's arrow is drawn on the real cube where the turning layer actually is in the picture
  (on the side of the cube most towards the camera that shows the turn).
- The move is detected from every side in view (one to three), so a move counts whichever way the
  cube is held; a wrong turn is still named with its fix.
- A move that no side in view shows asks the user to turn the cube so the turning side shows (the
  done button still works).
- While the cube is held differently from the holding view, the move text names the turning side
  by its centre colour and points to the arrow; the small 3D guide cube turns to match how the real
  cube is held.
- The camera works as in the video scan: light metered on the cube, then locked; a torch button.
- **BREAKING (behaviour):** the grid and the fixed holding rule are removed from camera follow.

Modules: `cube` (follow tracking from found faces), `shared` (follow panel, drawing on the real
cube, camera pipeline shared with the video scan); `app` and `web` only through the shared camera
code already used by the video scan.

## Capabilities

### New Capabilities

### Modified Capabilities
- `camera-follow`: holding rule, grid, arrow placement, move detection, not-visible moves, cube in
  view and calibration change to work with the cube held any way.

## Impact

- `cube/follow/FollowTracker`, `FrontArrow` (replaced by an arrow on any side), reuse of
  `cube/scan/FaceFinder`, `Orientation`, `CubeProjection`, `ExposureControl`.
- `shared/ui/guide/FollowPanel`, `ui/solve/SolveScreen` (`DefaultFollowPanel` switches from grid
  samples to found faces), follow strings (fi/en).
- Browser: the face finder already runs in the Web Worker; follow uses the same path.
