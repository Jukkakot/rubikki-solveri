# Design

## Context

Camera follow today (`cube/follow/FollowTracker`, `ui/guide/FollowPanel`) reads nine samples from
a fixed grid in the middle of the picture and compares them with `front(S)` / `front(S·m)` of the
known cube; the arrow (`FrontArrow`) is drawn in grid coordinates. The video scan already has
everything needed to see the cube from any side: `FaceFinder` (full and partial faces anywhere,
off the main thread / in the Web Worker), `Orientation` (cube turn from a face's steps, two faces
disambiguate), `CubeProjection` (every sticker's place in the picture), `ExposureControl`, the
torch. Unlike the scan, follow knows the whole cube state, which makes placing a seen face easy.

## Goals / Non-Goals

**Goals:** follow works with the cube held any way; the arrow sits on the real turning layer; one
camera pipeline for scan and follow.

**Non-Goals:** full 3D tracking between frames (each frame is placed on its own, with the last
placement only as a tie-breaker); slice moves or wide moves beyond what the solvers produce today;
any change to the on-screen guide (steady view stays).

## Decisions

1. **Frames = found faces.** `DefaultFollowPanel` switches from `CameraPreview(onSamples, onPicture)`
   to the video scan's `onImage` → `FaceFinder` path (worker in the browser), and the same
   `ExposureControl` + torch. The shared part of `VideoScanScreen`'s camera pipeline moves to one
   composable both screens use. *Alternative:* keep the grid and add a pose guess; rejected, the
   grid is the problem.

2. **Placing a seen face (cube module, `FollowView`).** For each found face (full, or partial with
   ≥ 7 stickers): the centre colour names the cube side (centres never move in face turns); its
   rotation in the picture is the one of four (0/90/180/270) whose stickers best match that side in
   the state *before or after* the move (or a wrong-move candidate, below). Two faces in one frame
   fix the rotation by geometry (`sideTowards`, as the scan's corner views), which also resolves
   evenly coloured faces. A single face whose best rotation is not unique (uniform or symmetric
   side) takes the rotation closest to the last placed one, if that is < 1 s old and the face's
   steps turned by < 45°; else the frame is "cannot place" → ask to tilt (spec "Cube in view").
   From the placed faces, `Orientation.candidates` + `CubeProjection` give the cube's turn and the
   sticker positions, as in the scan.

3. **Detection over all placed faces.** Score = matching stickers summed over the placed faces
   (centres left out), against `S`, `S·m`, and `S·x` for the 17 other face turns x. Done: the
   after-state wins, at most one mismatch per placed face, and at least one placed face is changed by
   m; stable for 3 frames (as today). Wrong move: a unique best `S·x` with the same rule → `WrongMove(x, x⁻¹)`.
   No placed face changed by m → `NotInView`. Classification uses `LiveCalibration` (kept), learning
   from every placed face that clearly matches a known state. *Alternative:* soft votes with
   `ColorClassifier.shares` as in the scan; kept for later if dim light proves a problem, since
   follow compares against a known answer and hard labels have worked so far.

4. **Arrow on any side (`SideArrow`, replaces `FrontArrow`).** Pick, among sides facing the camera
   (`CubeProjection.facing`), the one most towards the camera that m changes visibly: the turning
   side itself → round arrow about its centre; a neighbouring side → straight arrow along the row
   or column of stickers in the turning layer, from its first to its last sticker in the direction
   they move. Defined in cube coordinates (sticker indices + direction), projected each frame, so it
   rides on the cube. "2×" label at the head as today. The facing side gets the preference over a
   steep side when both show the turn (steep sides are where the weak-perspective projection
   drifts).

5. **Wording and guide cube.** "Held the holding-view way" = the placed orientation's front and up
   centres are the holding view's (`CubeScene.DEFAULT_VIEW`'s green / white). Otherwise the text is
   "Käännä <väri> puolta nuolen suuntaan" (+ "puoli kierrosta" for a half turn), colour from the
   turning layer's centre (existing colour names). The corner guide cube's view rotation is set from
   the placed orientation (snapped to the nearest of the 24 cube turns so it does not wobble), eased
   over ~200 ms; back to the holding view after 1 s without a cube.

6. **Whole-cube turns.** The solvers' current output has none, but `Move.isRotation` exists: in
   camera mode such a step advances by itself (holding is free; the next moves are placed by
   colour anyway).

7. **Logging.** `follow.event` gains the placed sides (`view=F,U,R`) and the hold (`usual|other`);
   a `follow.place` line when a frame cannot be placed, at most once a second.

## Risks / Trade-offs

- [Single uniform side late in the solve (most sides solved) is ambiguous] → continuity from the
  last placement, else ask to tilt; two sides in view are the normal case when holding a cube.
- [Projection drifts on steep sides, arrow slightly off] → prefer the most facing side; the arrow
  is drawn on the found face's own lattice when that face is the one drawn on.
- [More CPU than reading nine samples] → the same finder already runs ~15 fps in the scan on the
  user's phone and in the browser worker.
- [Partial faces under the fingers more often while turning] → partial faces (7–8) count; frames
  during the turn simply don't match anything and are "waiting".

## Migration Plan

None (no stored data). The grid strings `follow_hold`, `follow_bring_cube` are replaced.
