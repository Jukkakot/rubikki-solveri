# Design: scan-feedback

## Context

See proposal.md – Why. The paint (`ScanPaint.of`) veils every sticker of a found face that is not
`recognised` and dots the known ones; `recognised` comes from the best cube, so under the rules
scanner a face read many times stays grey until its track settles. `FaceTracks` already holds each
counting track's votes and leading colours (`TrackInfo.leading`); `VideoScanState.leading` holds the
leading colour of stickers not yet known on the projection. In the browser the video element shows
the live camera; the worker gets a 360 px `ImageBitmap` of the cover crop and answers ~45–60 ms later;
the marks are drawn by Compose over a hole in the canvas, with `Glide` (τ 60 ms) and `MotionFade`.

## Goals / Non-Goals

**Goals:** a face held to the camera visibly "takes" within about half a second; the ring tells
which side is missing; in the browser the marks lie on the cube in the picture shown.

**Non-Goals:** changes to how the scanner decides (rules, costs, finish); the Android picture path;
GPU work; arrows on the real cube; a computed "best next view" (the demo shows a side to show or
a corner view, not which link between faces the scanner lacks).

## Decisions

1. **"Read" is track-level.** A found face's sticker is read when its track counts (≥ `MIN_READINGS`
   pictures) and that sticker's votes have a leading colour; its mark (decision 6) shows that colour. The
   found face carries it (`FoundFace.read`, reading order) next to `known`. Known wins over read where
   both exist (a confirmed colour corrects an early misread). On projection sides not found in the
   frame, a sticker shows a dot when known or when `state.leading` has a colour; otherwise a veil.
   Alternative: the frame's own names (`names`) — rejected, they flicker frame to frame and would show
   one bad frame as a colour.
   The earlier look-based scanner has no tracks: its `read` is its `known` (behaviour unchanged).

2. **Read sides for the ring.** `VideoScanState.readSides: Set<CubeColor>` — the centre colours of
   tracks that have counted at any time in this scan (remembered after the track ends, cleared on
   restart). Segment state per colour: faint (not in `readSides`), lit (in it), full (its side in
   `confirmed`, or the scan `complete`). The centre colour is only evidence to the scanner, but for
   this checklist it is right in practice; a red/orange mix-up lights the wrong segment until the
   cube confirms the side, which is acceptable for a hint.

3. **Ring drawing.** Six arcs round the existing small ring's circle with small gaps, order W R G Y O
   B (opposites across); faint = the colour at low alpha as an outline, lit = the colour at medium
   alpha filled to half thickness, full = solid at full thickness. The accessibility description
   names the sides still unread instead of the sticker count.

4. **Status line once every side is read.** When `readSides` has all six colours and the cube is
   not complete, the line shows the existing "Käännä kuutiota" (`video_status_turn`), not "show the
   grey parts" (nothing is grey then). Order: done > no cube > turn (undecided or all read) > grey.
   The earlier idea of a corner text was dropped for the small demo cube (decision 5; user,
   2026-10-08).

5. **Turn demo on a small cube.** A `Cube3D` about 64 dp next to the status line, not draggable,
   all stickers grey except the six centres (user, 2026-10-08), so its orientation reads at a glance.
   Shown when no new sticker became known and no new side was read for `DEMO_IDLE_MILLIS` (2 s) and
   the scan is not complete; hidden at once on either. Its movement, a loop of about 2.5 s (turn,
   hold, jump back):
   - unread side and `orientation`/`pose` known: start from the real cube's orientation as the
     camera sees it, end with the first unread side (in W R G Y O B order) facing the camera,
     by the shortest whole-cube rotation;
   - every side read, or no pose: from one side facing the camera to the standard corner view
     (three sides showing).
   The target only changes when the side to show changes, so the loop does not jump while the
   user holds still. Rejected: an arrow on the real cube (removed in `scan-paint` because it came
   and went and its meaning was unclear); a text-only hint (the user wanted the movement shown).

6. **Marks: hollow ring vs. dot.** `PaintDot` gets `sure: Boolean`: a known sticker draws the
   filled dot as now, a read-only one a ring of the same outer size, stroke about a quarter of its
   radius, in the read colour. A key keeps its place when it turns sure, so the ring fills in place.

7. **Vibration on a new side.** The same short haptic as for new stickers when `readSides` grows,
   under the existing `BUZZ_MILLIS` spacing so a corner view (three new sides) buzzes once.

8. **Browser: show the read picture.** When the worker is in use, `scanWorkerSend` also makes a
   display-size `ImageBitmap` of the same video frame (the cover crop at the box's CSS size × device
   pixel ratio, capped at 1280 px on the long side) and keeps it under the frame's sequence number.
   When the worker's faces for that number come back, the page holds it as the frame to show; the
   Kotlin side calls `cameraShowFrame(seq)` from the same Compose frame that first draws that
   reading's paint, which draws the bitmap onto a display canvas placed where the video is, hides the
   video and closes older bitmaps. Kept at most two bitmaps (in flight + shown) to bound memory.
   With this on, `Glide` snaps (τ 0) and `MotionFade` always shows; the age fade (`paintAlpha`) stays
   for a cube out of view. Without a worker or without `createImageBitmap`, the live video and the
   old glide/fade stay. Alternative: predicting the motion — rejected, it overshoots when the cube
   stops; optical-flow tracking between readings — too big for the gain.

9. **Android unchanged.** The phone app keeps the live preview with glide and fade; the user tests
   mainly in the browser (2026-10-08). If the browser result is liked, the same idea for Android
   (show the analysis image) is a later change.

## Risks / Trade-offs

- [The picture nykii at ~17 fps and lags ~60 ms] → accepted by the user; if it feels bad, a setting
  could bring back the live video.
- [Extra `createImageBitmap` per frame costs time on the page's thread] → it is asynchronous in the
  browser; log its time in `paintMs`' neighbour (`showMs`) and compare fps with the previous log.
- [Paint and picture drawn in different frames (canvas vs. Compose) would still show a one-frame
  offset] → the show call is made from the Compose draw of that reading; checked on the phone.
- [Early dots from a track that later turns out to be a stray lattice] → a track that never counts
  shows nothing; one that counts and is wrong shows dots only while it is in view.

## Implementation notes (apply, 2026-10-08)

- `readSides` takes a counting track's leading centre colour (rules) or a pile seen `MIN_VOTES` times
  (look). The striped phone fixture is clear with five sides read (the sixth follows), so the test
  asks for at least five by `complete`; the ring is full on complete anyway.
- The turn demo starts from `orientation` when known, else from the last `pose`; with neither, the
  corner tilt. It is shown only where the status line is (not under a stall notice).
- Ring segment looks: faint = 30 % alpha at a third of the stroke, lit = 75 % at half, full = solid.
  The read ring's stroke is ~0.28 of the dot's radius, dark-rimmed like the dot.
- Read picture: up to three copies may be alive for a moment (in flight, waiting, answered not yet
  drawn); each is closed once a newer one is drawn or dropped. The copy is drawn and closed at once.
- `./gradlew check` also runs `:shared:checkComposeUiTestConfigurationForWasmJs`, which fails on
  `main` as it was (no wasm UI tests are declared); the project's chain `test lint assembleDebug`, the
  web build and both browser smoke tests passed.
