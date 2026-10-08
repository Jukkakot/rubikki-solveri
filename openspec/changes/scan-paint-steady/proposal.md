# Proposal: scan-paint-steady

## Why

Browser phone test (2026-10-08 16:40, log and screen recording): the scan finishes right in 10–16 s,
but the paint still flickers, mostly the grey veils:

- A whole ghost side of grey veils appears on the table beside the cube. With one face in view the
  cube's tilt has two mirror answers; with no other face to tell, `Orientation.choose` takes the
  first, and the mirrored projection puts its sides off the cube.
- Striped double veils on a face: a projection side is skipped only under a found face whose side
  is named, so an open face gets the projection's veils too, a little off.
- All marks blink out for 3–4 pictures (~0.3 s) now and then: the browser shows the read picture,
  and a picture in which the finder found no face has no marks (the glide used to hide such gaps).
- The read picture's copy costs 20–27 ms a picture on the phone (7 ms on the desktop); the scan ran
  at 10–15 pictures a second instead of 17.

## What Changes

- **Projection only with a sure tilt:** the cube's other sides are drawn only when its tilt is
  sure: the face is seen straight on (one answer), another known face in the same picture tells the
  tilt, or the tilt follows a sure one from the frames just before. Otherwise only the faces found
  are marked (the spec already says so for an unknown pose). The turn demo starts from the cube's
  hold only when it is sure, else from the last pose.
- **No second layer:** a projection side is not drawn where a face found in the picture lies
  (its centre within about a sticker step of the side's centre), named or not.
- **Browser: hold the last picture instead of a blank one:** when a reading finds no face, the
  screen keeps the previous read picture with its marks, at most 300 ms; then the empty one shows.
  The scan itself still takes every reading. Android unchanged.
- **Smaller copy:** the read picture's copy has its long side at most 720 px (was the box at the
  device's pixel ratio, up to 1280).

Decisions: the tilt counts as sure from the previous frame only while frames with a pose follow each
other (a gap of more than `HOLD_MILLIS` without a pose starts over). "Hold the picture" was chosen
over keeping old marks on a newer picture (they would sit beside a moving cube) and over a short gap
(user, 2026-10-08, on my recommendation).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: the projected sides need a sure tilt and give way to found faces; in the browser a
  picture without a face found does not replace the shown one for a moment.

## Impact

- `cube`: `Orientation.choose` tells how it chose; `VideoScan` builds the projection (and gives the
  orientation) only from a sure tilt.
- `shared`: `ScanPaint` skips projection sides under found faces; `VideoScanContent` holds the
  shown picture and its paint over a faceless reading in read-picture mode; `TurnDemo` unchanged
  apart from the orientation it gets.
- `web`: `platform.mjs` caps the copy at 720 px.
- `app`: tests only.
