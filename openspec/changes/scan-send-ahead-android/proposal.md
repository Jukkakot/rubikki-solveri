# Proposal: scan-send-ahead-android

## Why

The browser's send-ahead (`scan-send-ahead`) paid off on the phone (browser scans 2026-10-11 16:33:
30 pictures a second from a 30 fps camera, 39–51 from a 60 fps one, `idleMs` 1.6–3.4, `prepMs`
3–5 ms; four scans finished right in 3–10 s). Roadmap item 80 asks the same for the phone app.

Reading the app's camera path shows it already overlaps: the camera thread copies the frame, turns
it upright and makes the read picture's bitmap, then hands it over (one waiting at most, newer
replaces it), while the finder reads the previous picture on its own thread. What the app lacks is
the log to see where its time goes (its snapshot has no `prepMs` / `idleMs`), and it still does work
nobody uses: every frame is also sampled for the guided scan's grid, which the video scan throws away.

## What Changes

- Phone app: the video scan snapshot adds `prepMs` (camera thread: copy, upright picture and bitmap
  of one frame) and `idleMs` (the finder's wait between its answer and the next picture), as in the
  browser.
- Phone app: the video scan no longer samples each frame for the guided scan's grid (unused there).
- The send-ahead requirement and the log requirement say "on the phone and in the browser".
- Not in this change: asking the phone's camera for 60 pictures a second (see design, open question).
- Expected: the same scans; the log tells whether the camera (30 a second), the preparation or the
  finder sets the pace in the app.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: "Reading stays quick in a long scan": the picture made ready while faces are found,
  read once, one waiting at most, on the phone too.
- `diagnostics`: "Video scan log": the preparation time and the finder's wait on the phone too.

## Impact

- Modules: `shared` (`CameraPreview.android.kt`, `Camera.kt` `ScanImage`, `VideoScanScreen.kt`).
  Not `web`, not `cube`.
- Tests: a unit test for the app's timings (pure helper), `VideoScanLogTest` unchanged or extended.
