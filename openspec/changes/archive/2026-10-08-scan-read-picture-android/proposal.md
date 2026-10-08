# Proposal: scan-read-picture-android

## Why

In the browser the scan now shows the very picture it read, with that picture's marks
(`scan-feedback` design 8, `scan-paint-steady`), and the user found it good (2026-10-08). The phone app
still shows CameraX's live preview (`PreviewView`) under marks worked out from the analysis frame,
which is a moment older: the marks glide after a moving cube and fade while it moves fast. The user
asked for the same on the phone.

## What Changes

- **The phone shows the read picture:** each analysis frame the scan reads is turned upright and
  shown as a bitmap filling the camera box, drawn by Compose in the same frame as its marks (no
  JavaScript hand-off as in the browser). The live `PreviewView` stays bound beneath it (CameraX
  needs it for the crop that matches the box) and is seen only until the first picture is read.
- **Marks as in the browser:** they snap (no glide) and do not fade for movement; the age fade for a
  cube out of view stays; a reading with no face keeps the shown picture for up to 300 ms
  (`holdPicture`).
- **Analysis size stays 640×480** (user, 2026-10-08: try it as it is first; the picture is softer
  than the live preview). The bitmap's time per picture is logged as `showMs`, as in the browser.
- **Browser copy made cheaper:** the phone log of 1.0.317 shows the read picture's copy at 15–22 ms
  and the scan at 10–13 pictures a second (17 before the read picture). The copy is made instead by
  drawing the video frame onto one of a few reused canvases (three, placed where the box is) when
  the picture is sent, and shown by making that canvas the visible one: no `createImageBitmap` with
  a resize and no second draw. `showMs` is then that draw's own time.
- Other camera screens (guided scan, camera follow) keep the live preview.

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: the read picture with its marks on the phone too, not only in the browser.

## Impact

- `shared` (androidMain): `CameraPreview.android.kt` makes the upright bitmap of each analysis frame
  (for the video scan only) and hands it with the faces; the analysis resolution.
- `shared` (common): `FoundFaces` carries the picture to draw (an `ImageBitmap`) besides the browser's
  `show`; `VideoScanScreen` draws it under the paint; read-picture mode = either.
- `web`: `platform.mjs` copies into a pool of canvases instead of `ImageBitmap`s.
- `app`: tests only.
