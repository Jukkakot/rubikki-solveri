# Design: scan-send-ahead-android

## Context

Phone app video scan (`CameraPreview.android.kt`, `VideoScanScreen.kt`): CameraX image analysis,
640×480 RGBA, `STRATEGY_KEEP_ONLY_LATEST`, analyzer on a single-thread executor (the camera thread).
Per frame the camera thread copies the plane into a reused buffer, builds `RgbaFrame`, calls
`onPicture` (null here) and `onSamples(FrameSampler.sample(frame))` (the video scan passes `{}`),
then `FrameSampler.upright(frame)` and `uprightBitmap` (timed as `pictureMs`, logged as `showMs`),
and `images.tryEmit` into a flow with one slot, newest wins. The finder collects on
`Dispatchers.Default`, so it already reads picture N while the camera thread prepares N+1: the
browser's send-ahead exists here by construction. A camera frame is emitted once.

Measured before (roadmap 65, 67): app 17–21 → about 30 pictures a second, finder 10–16 ms on the phone.
Camera at 30 fps, so the app is likely camera-bound; no log shows the preparation or the wait.

## Goals / Non-Goals

**Goals:** the app's snapshot tells `prepMs` and `idleMs` like the browser's; no unused per-frame work.

**Non-Goals:** a faster finder or scan; a 60 fps camera (open question); the guided scan.

## Decisions

1. **`prepMs` on the camera thread.** From the analyzer's start of a frame (before the buffer copy)
   to just before the emit: copy, upright `ArgbImage` and bitmap. Carried in `ScanImage` (new field
   beside `pictureMs`) and into `FoundFaces.prepMs`. `showMs` stays the bitmap time.
2. **`idleMs` in the finder loop.** Time from the finder's previous answer to taking the next
   picture from the flow (0 for the first picture). Into `FoundFaces.idleMs`; the snapshot's
   averaging already exists (`VideoScanScreen` around `prepMs` / `idleMs`). Computed by a small pure
   helper so it is unit-tested without a camera.
3. **No grid sampling for the video scan.** `onSamples` becomes optional on `CameraPreview` (null =
   not sampled); the video scan passes null, the guided scan and solve screen keep theirs. The
   browser side ignores it as today.
4. **No new pipeline.** The overlap and "one waiting, newest wins" are kept as they are; the spec
   only now says it holds on the phone too.

## Risks / Trade-offs

- [The app turns out finder-bound, not camera-bound] → the new numbers show it; a follow-up then.
- [Timing calls cost] → two `nanoTime` reads a frame, negligible.

## Open Questions

- 60 fps camera for the app (browser on the same phone reads 39–51 a second with it). CameraX's
  controller may not offer a target frame rate for image analysis; worth a separate change only if
  the new log shows the finder idle most of the time and faster scans matter.
