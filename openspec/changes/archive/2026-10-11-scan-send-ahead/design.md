# Design: scan-send-ahead

## Context

Browser video scan today (`platform.mjs`, `WebCamera.kt`): the Kotlin camera loop runs once per
animation frame (`withFrameMillis`). When the finder worker is idle (`scanWorkerIdle`: ready, not
busy, nothing pending) it calls `scanWorkerSend`, which draws the read-picture copy (`copyFrame`)
and asks the browser for a scaled `ImageBitmap` (`createImageBitmap` with `resizeWidth/Height`);
when that resolves the bitmap is posted to the finder (`workerQueue`). The finder answers, the
faces go on to the scan worker (pipeline, `scan-speed-up-4`), and the page shows the read picture
with its marks on the scan's answer.

So one finder cycle = wait for the next animation frame + bitmap + transfer + finder. Measured
(desktop Chromium, deployed 2026-10-11, fake camera `web_181940` at 30 fps): finder 10 ms median,
finder round trip 14 ms, idle gap between answer and next picture 14 ms, 34 pictures/s from a 30
fps camera. Phone recordings: finder 16–19 ms, picture interval 40–43 ms. The bitmap time on the
phone is not logged; a similar `createImageBitmap` with resize took 15–22 ms there
(`scan-read-picture-android`).

`scanWorkerSend` never checks whether the camera gave a new picture (`cameraGrab` does, via
`requestVideoFrameCallback` / `currentTime`), so the same camera frame can be read twice.

## Goals / Non-Goals

**Goals:** the finder never idles while a new camera picture exists; each camera picture read at
most once; the log tells where the browser's time goes (prepare, wait).

**Non-Goals:** a faster finder or scan logic; a 60 fps camera; the phone app's camera path
(CameraX already delivers every analysis frame to its own buffer); the guided scan's grid reading.

## Decisions

1. **Send on a new camera picture, from JavaScript.** `platform.mjs` gets a send-ahead mode: Kotlin
   gives the crop and finder size once (and again when the box or video size changes,
   `scanWorkerCrop(x, y, w, h, long)`), and the page's own frame watcher (`requestVideoFrameCallback`)
   starts the next picture when a new camera frame arrives, and again when the finder answers if a
   frame came meanwhile. Without `requestVideoFrameCallback` (old Safari) the Kotlin loop keeps
   calling the send on each animation frame, now also when the finder is busy, guarded by the
   video's `currentTime`. *Alternative:* keep the Kotlin loop as the trigger but drop the "idle"
   condition: simpler, but still tied to the animation frame (up to 16 ms late at 60 Hz) and a Wasm
   call per frame.
2. **One picture ahead, never more.** A picture is started only when no picture is being made and
   none waits for the finder. While the finder reads picture N, picture N+1 is made and waits; a
   newer camera frame does not start another bitmap (it is taken on the next send chance), so no
   bitmap work is thrown away. *Alternative:* replace the waiting picture with every newer frame
   (fresher, but a 15–20 ms bitmap per dropped frame on the phone). The waiting picture is at most
   one finder time old; the marks may come a few ms later than today, which the user does not see.
3. **A camera frame read once.** Each frame from the watcher gets a number
   (`metadata.presentedFrames`, or a counter); a picture is sent only for a number above the last
   sent. Same guard in the fallback via `currentTime`.
4. **Timings in the snapshot.** Each picture carries `prepMs` (from asking for the bitmap until it
   is ready, page side) and `idleMs` (finder's answer of the previous picture to this picture's
   post; 0 when it was waiting already). They travel with the faces to `FoundFaces` like `showMs`
   and are averaged in the snapshot as `prepMs` and `idleMs`; absent (null, not logged) on the phone
   and on the page fallback.
5. **Read-picture pool 6.** One more picture can be in flight (made and waiting for the finder), so
   `POOL_SIZE` 5 → 6; a picture without a free canvas is still sent without a read picture (seq 0),
   as today.

## Risks / Trade-offs

- [The phone's bitmap is not the cause] → the new `prepMs`/`idleMs` show it on the first phone
  test; the change still removes the animation-frame wait and the double reads.
- [More work a second costs battery and heat] → bounded by the camera's 30 pictures a second; the
  finder is already the same work per picture.
- [Busier workers slow the page] → the page thread does only the copy and the bitmap request, as
  today; checked with the desktop trace (animation frame work stays 1–4 ms).

## Migration Plan

Browser-only; ships with the next Pages deploy. Rollback = revert the commit.

## Open Questions

- Phone numbers after the change (fps, `prepMs`, `idleMs`) come from the user's next browser scan.

## Findings (implementation, 2026-10-11)

Desktop Chromium in the cloud container, fake camera from the stills of `web_181940`, scan of the
striped cube; numbers are medians unless said.

| Build | Camera | Pictures a second | Finder wait (`idleMs`) | Picture → scan answer |
|---|---|---|---|---|
| before (timings only) | 30 fps | 29–36 (34 with double reads) | 13–16 ms | 30 ms |
| after | 30 fps | 29.7–30.4 (the camera's rate) | 18–22 ms (the finder is faster than the camera) | 23 ms |
| before (timings only) | 60 fps | 27–32 | 16 ms | 44 ms |
| after | 60 fps | 44–55 | 1–5 ms | 44 ms |

`prepMs` on the desktop 2–5 ms before, 0.4–1.5 ms after (no longer waits behind the page's frame).
Page animation-frame work unchanged (1.4 → 1.5 ms). Without `requestVideoFrameCallback` (removed in
an init script) the scan runs at 30 a second and finishes; without `Worker` the page fallback runs
as before. The phone's own numbers come from the user's next browser scan.

Deviation from decision 1: Kotlin calls `scanWorkerCrop` on every animation frame (not only when the
box changes); it is a cheap call and doubles as the trigger where there is no frame callback.

`web/smoke/video.mjs --no-worker` no longer blocked the worker (a blocked route does not stop a
worker's script with Playwright 1.56; it failed the same on `main`); it now removes `Worker` from the
page instead.

Seen while measuring, not caused by this change: the striped cube finished as its mirror image
(`GRB…` instead of `BRG…`) in 3 of 16 fake-camera runs, before and after the change alike (1 of 9
before, 2 of 7 after); twice more the page fallback stayed on that cube without finishing. Which
pictures are read decides it. Kept in the roadmap backlog.
