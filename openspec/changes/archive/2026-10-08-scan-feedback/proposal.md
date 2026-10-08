# Proposal: scan-feedback

## Why

In phone testing (2026-10-08, browser) the video scan felt aimless: the user turns the cube and
cannot tell whether anything is happening. The log shows why: the rules scanner keeps faces it has
read in reserve until it knows which face and turn they are, so the ring and the grey veils stood
still at 24/54 for eight seconds and then jumped to done. The work is done but not shown. Also the
marks lag behind a moving cube, because they are worked out from a picture about 60 ms older than
the video shown under them.

## What Changes

- **Read shows at once:** a face's grey veils give way to dots in the colours the camera read as soon
  as that face has been read steadily, before the scanner knows which face it is. A side's white
  outline and tick still mean the rest of the cube confirms it. Grey now means "the camera has not
  read this yet".
- **Ring of six colours:** the progress ring becomes six segments, one per centre colour. A segment
  shows pale while that face is unread, lights when a face with that centre has been read, and is
  solid once that side is confirmed. A pale segment tells which side to show; no arrows.
- **Read and sure look different:** a read sticker shows a thin hollow ring in its read colour; it
  fills to a dot once the cube makes it sure.
- **A small 3D cube shows how to turn** (instead of a text hint, user 2026-10-08): when nothing new
  has been read for about two seconds, a small cube by the status line, grey except its six centres,
  shows a short repeating turn: from how the cube is held to the unread side facing the camera, or,
  once every side is read but the cube is not clear (or the pose is unknown), a tilt from face-on to
  a corner view (three sides at once). It goes as soon as something new is read. No arrow on the
  real cube (removed in `scan-paint` because it came and went).
- **Vibration** also when a ring segment lights (a new side read).
- **Browser: the picture and the marks from the same frame:** the browser shows the very picture
  the scanner read, with its marks, instead of the live video. The marks sit exactly on the cube;
  the picture runs at the scanner's rate (about 17 a second) and about 60 ms late (user accepted,
  2026-10-08). Without the scan worker the live video stays as now. The Android app is unchanged.
- Not done: moving work to the GPU (does not remove the lag, and would be built twice; rejected for
  now, 2026-10-08).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: what the veils, dots and ring show (read vs. confirmed), the turn demo cube by
  the status line, vibration on a new side, and in the browser the picture shown being the one read.

## Impact

- `cube`: `VideoScan` / `FaceTracks` give each found face its track's steady reading and the centre
  colours read so far.
- `shared`: `ScanPaint` (dots from readings), the progress ring, the turn demo cube (the app's 3D cube view, small); the browser path
  turns off glide and motion fade when the picture is the read one.
- `web`: `platform.mjs` keeps a display copy of each frame sent to the worker and draws it on a
  canvas in the video's place when its faces come back; `WebCamera.kt` switches to it.
- `app` (Android): no change beyond the shared UI.
