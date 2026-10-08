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
GPU work; arrows or turn hints.

## Decisions

1. **"Read" is track-level.** A found face's sticker is read when its track counts (≥ `MIN_READINGS`
   pictures) and that sticker's votes have a leading colour; the dot shows that leading colour. The
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

4. **Corner hint in the status line.** New text `video_status_corners` ("Näytä kuution kulmia"),
   shown when `readSides` has all six colours, the cube is not complete, and no stall or
   `undecided` hint takes precedence. Order: done > no cube > undecided (turn) > corners > grey.

5. **Browser: show the read picture.** When the worker is in use, `scanWorkerSend` also makes a
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

6. **Android unchanged.** The phone app keeps the live preview with glide and fade; the user tests
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
