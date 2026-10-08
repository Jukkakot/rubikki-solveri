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
- **Status line:** once every face has been read but the cube is not yet clear, the line asks to
  show the cube's corners (three faces at once), which is what lets the scanner place the faces.
- **Browser: the picture and the marks from the same frame:** the browser shows the very picture
  the scanner read, with its marks, instead of the live video. The marks sit exactly on the cube;
  the picture runs at the scanner's rate (about 17 a second) and about 60 ms late (user accepted,
  2026-10-08). Without the scan worker the live video stays as now. The Android app is unchanged.
- Not done: moving work to the GPU (does not remove the lag, and would be built twice; rejected for
  now, 2026-10-08).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: what the veils, dots and ring show (read vs. confirmed), the status line's corner
  hint, and in the browser the picture shown being the one read.

## Impact

- `cube`: `VideoScan` / `FaceTracks` give each found face its track's steady reading and the centre
  colours read so far.
- `shared`: `ScanPaint` (dots from readings), the progress ring, the status line; the browser path
  turns off glide and motion fade when the picture is the read one.
- `web`: `platform.mjs` keeps a display copy of each frame sent to the worker and draws it on a
  canvas in the video's place when its faces come back; `WebCamera.kt` switches to it.
- `app` (Android): no change beyond the shared UI.
