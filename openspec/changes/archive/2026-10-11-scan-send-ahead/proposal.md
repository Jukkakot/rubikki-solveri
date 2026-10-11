# Proposal: scan-send-ahead

## Why

The browser video scan reads 22–24 pictures a second on the user's phone (recordings
`web_20261009_*`, `web_20261010_102548`: picture interval 40–43 ms) while the face finder itself
takes 16–19 ms a picture and the camera gives 30. The finder waits about 20 ms between pictures: the
page makes the next picture only after the finder has answered and the next animation frame has
come, then the browser scales it (`createImageBitmap`, 15–22 ms for a similar call on the phone in
`scan-read-picture-android`), and only then the finder starts. In a desktop Chromium trace
(2026-10-11, timeline artifact) the finder stood idle 14 ms of each 30 ms cycle. The steps run one
after the other instead of overlapping. The same trace read 34 pictures a second from a 30 fps
camera: a camera picture can be sent twice.

## What Changes

- Browser: the next picture is prepared while the finder reads the current one. A picture is made
  when the camera gives a new one (not on the next animation frame); at most one waits for the
  finder, a newer one taking its place. The finder starts the waiting picture as soon as it
  answers.
- Browser: a camera picture goes to the finder at most once.
- Log: the video scan snapshot adds the time to prepare a picture for the finder and the finder's
  wait between pictures (browser; absent on the phone).
- Expected (estimate): phone browser 22–24 → close to 30 pictures a second (the camera's rate);
  marks a few ms later at most. A 60 fps camera is out of scope (later, separately).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: "Reading stays quick in a long scan" adds that in the browser the finder does not
  wait for the next picture and reads each camera picture at most once.
- `diagnostics`: "Video scan log" snapshots add the picture preparation time and the finder's wait.

## Impact

- Modules: `web` (`platform.mjs`: sending, frame tracking, timings; `WebCamera.kt`), `shared`
  (`FoundFaces` and the snapshot in `VideoScanScreen` / `VideoScanLog`). Not `cube`, not `app`
  (the phone app's own camera path is unchanged).
- `web/smoke/video.mjs` keeps passing with and without the worker.
