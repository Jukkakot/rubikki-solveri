# Design

## Context

How the video scan works today:
- The camera hands a downscaled picture to the face finder at most every 66 ms, both on Android and
  in the browser's worker.
- The scan state (`VideoScanState`) carries `projection`, which tells where every sticker lies in
  that picture. It exists only for frames in which a face with a settled rotation is found.
- The screen draws the marks once per reading result, so they jump at about 15 Hz. They vanish in
  every frame without a projection, which is what makes them flicker.
- The turning arrow uses `hint` and is subject to the same gaps.

The finder's time per picture on the phone is not known from this repo. The log's video snapshot
reports it (`finder` ms) together with `fps`.

## Goals / Non-Goals

**Goals:**
- Tiles drawn at the display's rate, gliding between readings.
- A projection that survives short gaps.
- Reading as fast as the camera delivers pictures.
- A screen with only the camera, three icons, a ring and one line.

**Non-Goals:**
- Full 3D tracking of the cube between readings (optical flow, gyroscope).
- Changes to how stickers are recognised or when the scan finishes.
- The guided scan's screen.

## Decisions

1. **Hold the projection in the cube module.**
   - `VideoScan` keeps the last projection and the time it was built.
   - In a frame with faces found but no settled orientation, the held projection is moved so that
     its nearest side's centre lies on the largest found face's centre. Rotation and scale are kept.
     This is a cheap re-anchoring that follows a cube moved sideways.
   - In a frame without faces, the held projection is returned unchanged.
   - The state gains `projectionAge` (ms since a projection was last built or anchored). The screen
     fades the paint from 0.6 s and hides it at 1.2 s.
   - This lives in `cube` so it is unit-tested on the JVM without a screen.
   - *Alternative:* holding it in the screen. Rejected, because the anchoring needs the found faces
     and the net, which belong to the scan.

2. **Smooth on the screen at the display rate.**
   - The drawn sticker centres follow the latest projection with an exponential approach (time
     constant about 60 ms), updated in `withFrameNanos`. Positions are kept per sticker index.
   - When the cube's front face changes (a new pose), the points jump at once instead of sliding
     across the screen, so a tile never travels over the cube.
   - Found faces' own readings are drawn the same way, keyed by the side named by their centre.
   - *Alternative:* predicting motion from the last two readings. Rejected for now, because it
     overshoots on stops. It can be added if the phone still shows lag.

3. **Tiles instead of dots.**
   - A tile is the sticker's quadrilateral, shrunk to about 62 % around its centre.
   - The corners come from the neighbouring centres: half a step along each side's own u and v,
     taken from the projection's neighbours or the face reading's u and v. A side at an angle
     therefore gets a slanted tile.
   - A known tile is filled with the sticker colour and has a thin dark edge. A needed tile is a grey
     fill at 35 % with a dashed white edge.
   - A confirmed side gets a white outline along its outer edge (the outer corners of its corner
     stickers), 3 dp wide.

4. **Read every picture.**
   - Android: drop the `IMAGE_MILLIS` throttle. CameraX's `STRATEGY_KEEP_ONLY_LATEST` and the
     `DROP_OLDEST` buffer of one picture already drop what the finder cannot take.
   - Browser: send the next picture to the worker as soon as it is ready (`scanWorkerReady`) instead
     of every 66 ms. The camera loop's `FRAME_MILLIS` goes from 66 ms to every animation frame for
     the worker path. The page-thread fallback keeps 66 ms.
   - The guided scan's grid reading (`onSamples`) is unchanged.
   - *If the phone's log shows fewer than about 25 pictures a second,* the next step is a separate
     change. It would make the finder search only around the last face. Not built now.

5. **Ring.**
   - The ring's share is the number of known stickers divided by 54, animated over 250 ms. It is set
     to 1 when the scan finishes.
   - It is drawn at the top centre of the picture (36 dp, white on a dark translucent disc) and has
     no number.

6. **Menu and icons.**
   - The top bar is replaced by a row overlaid on the picture: a round back button on the left;
     torch and ⋮ on the right.
   - The menu is a Material `DropdownMenu` with "Kuva kerrallaan", "Syötä käsin" and "Korjaa värit".
     "Korjaa värit" is enabled once a sticker is known.
   - The picture runs edge to edge under the status bar. The icons keep the safe-area insets.

7. **Status line.** One pill at the bottom of the picture:
   - "Näytä kuutio kameralle" while no face is found.
   - "Näytä harmaat kohdat" while stickers are needed.
   - "Valmis!" at the end.

   The stall notice takes the pill's place while it is shown.

## Risks / Trade-offs

- [Re-anchoring by translation drifts when the cube is turned without a settled face.] → It is held
  for at most 1.2 s and replaced as soon as a settled face is read.
- [Faster reading heats the phone during a long scan.] → Accepted (user, 2026-10-06: battery use does
  not matter). The log shows fps and finder time.
- [The browser on a slow phone sends pictures faster than the worker reads them.] → The worker gets
  a new picture only when it is idle, so nothing piles up.
- [Without the arrow, a user may not know which way to turn for the last side.] → The grey tiles show
  it. This should be checked on the phone.

## Open Questions

- The tile size (62 %) and the smoothing constant (60 ms) need tuning on the phone.
