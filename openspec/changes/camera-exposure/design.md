# Design

## Context

Today (`video-scan-light`): the video scan locks exposure and white balance once a face is found
(`VideoScanContent` → `CameraPreview(lockExposure)`) and unlocks it for a second after a torch change
(`LightSettle`). The camera meters the whole picture. Android: CameraX `LifecycleCameraController`,
analysis 640×480 RGBA, the video scan's picture every 100 ms (`IMAGE_MILLIS`), AE/AWB lock through
Camera2 interop. Browser: `getUserMedia` 1280×720 ideal, frames grabbed at most every 66 ms, the
video scan's 360-px copy every 100 ms, lock by `exposureMode`/`whiteBalanceMode` = `manual`; the
face finder runs on the page's one thread (25–64 ms per picture on the user's S24 in Samsung
Internet, 17–22 ms on a desktop). The user's log (2026-10-05) shows median sticker brightness 255
through three failed attempts with the torch in a dark room, about 180 in the one that finished in
16 s.

## Goals / Non-Goals

Goals: stickers not washed out with the torch in a dark room, on the phone and in the browser; the
camera focused on the cube; about 15 pictures a second; a log that says what the camera can do and
how fast the scan reads; in the browser the video scan's reading off the page's thread.

Non-goals: moving the guided scan's grid reading or the solver to a worker; manual ISO/shutter; changing the guided scan
(it keeps its lock at the first capture); brightening a dark picture (the "Hämärää" notice and the
torch cover that).

## Decisions

1. **One exposure controller in `cube`, pure and tested** (`ExposureControl`). Each frame it gets the
   faces found (where the largest is, as a share of the picture) and how many of their readings are
   washed out (brightest channel at `VideoScan.WASHED_FROM` or more), and the time. It answers what
   the camera should do: the point to measure and focus at (or none), how many steps darker
   (`darker`, 0 = the camera's own choice), and whether to lock. States:
   - *Searching* (no face yet): unlocked, no point, 0 steps.
   - *Metering*: a face found → point at its centre; after the camera has had `SETTLE_MILLIS`
     (about 0.6 s) to adjust, if more than a third of the readings are washed out, one step darker and
     wait again; else (or at the darkest step the camera allows) → *Locked*.
   - *Locked*: exposure and white balance locked. If the readings stay washed out for 2 s (the cube
     brought closer to the torch) → back to *Metering* from the current step.
   - A torch change → *Metering* again (the torch off goes back to 0 steps first); this replaces
     `LightSettle` (its "frames not read while settling" stays: frames are not read while *Metering*
     waits).
   - While unlocked, the point follows the face when it moves more than 15 % of the picture; once
     locked only focus follows (at most once a second), measuring stays where it was locked.
2. **Step size**: the camera's own exposure-compensation step, as many as make about −0.5 EV per step
   (Android `ExposureState` step; browser `exposureCompensation` capability `step`), down to −2 EV or
   the camera's minimum. Measured on the phone; the log line of decision 6 tells the real ranges.
3. **Android**: the point goes through `FocusMeteringAction` (AF, AE, AWB flags, no auto-cancel)
   built from a `SurfaceOrientedMeteringPointFactory` in the analysis frame's upright coordinates;
   steps through `CameraControl.setExposureCompensationIndex`; the lock stays the Camera2 AE/AWB
   lock. Locking after a metering action keeps that metering.
4. **Browser**: `applyConstraints({ advanced: [{ pointsOfInterest: [{x, y}], focusMode:
   'single-shot' or 'continuous', exposureCompensation }] })`, each only if `getCapabilities()` lists
   it; the lock as today. Anything not supported is skipped and named in the log line; the scan then
   behaves as today. The point is converted from the visible picture to the full video frame
   (the cover crop).
5. **Frames a second**: Android `IMAGE_MILLIS` 100 → 66 (the finder already runs on its own thread
   and drops pictures it cannot keep up with). Browser: the 360-px copy for the video scan every 66 ms
   too; the finder's queue keeps only the newest, so a slow page reads fewer, never piles up.
6. **Log**: a `scan.camera` line when the camera opens (both platforms): name/id, resolution, focus
   modes, exposure-compensation range and step, metering-point support, torch, lock support. The
   video scan's snapshot adds `fps` (pictures read since the last snapshot per second), `torch`
   and `darker` (steps); a `kind=lock` line when the controller locks, with the steps and the
   washed-out share.
7. **Tests**: `ExposureControl` unit tests (steps down while washed out, locks when good, stops at the
   minimum, torch change re-meters, relock after 2 s washed out, point follows the face); the video
   scan replay tests run unchanged (the controller only drives the camera). Compose test: the screen
   passes the controller's point/steps/lock to the preview. The real effect is checked by the user on
   the S24 (app and browser) in a dark room with the torch.

8. **Web Worker for the browser's video scan.** A new Gradle module `webworker` (Kotlin/Wasm,
   `binaries.executable()`, depends only on `cube`) builds `scan-worker.js` + its `.wasm`; the web
   build copies them into its distribution (and `precache.json`). The page takes each picture as an
   `ImageBitmap` of the visible part (`createImageBitmap(video, crop…, {resizeWidth/Height})`, done by
   the browser off the page's work) and transfers it to the worker; the worker draws it into an
   `OffscreenCanvas`, reads the pixels, runs `FaceFinder` and posts the faces back as numbers (centre,
   steps, nine colours, missing marked), with its time. At most one picture is in the worker at a
   time; a newer one waits in place of the older (only the newest is kept). The page turns the
   numbers back into `FaceReading`s for `VideoScan` (which stays on the page: it is cheap, about 2 ms).
   The guided scan keeps reading its small square on the page. If the worker fails to load or throws,
   the page logs it and reads on its own thread as today (`scan.worker` line with the reason).
9. **The finder's time** in the snapshot is the worker's own time per picture; the snapshot also
   says `worker=true/false`.

## Risks / Trade-offs

- Two Kotlin/Wasm runtimes load in the browser (the app and the worker, about a few hundred kB more
  for the worker's `cube` code); the worker starts when the video scan opens, not at app start.
- `createImageBitmap` with resize may be missing in an old browser: then the page draws the picture
  itself and transfers its pixels (still off-thread finding).

- Samsung Internet may not offer `pointsOfInterest` or `exposureCompensation`; then the browser stays
  as today and the log says so (the next step would be our own exposure guess, out of scope).
- Lowering exposure darkens the room too; the scan reads only the stickers, so that is fine, but the
  picture looks darker to the user.
- 15 pictures a second costs battery.
