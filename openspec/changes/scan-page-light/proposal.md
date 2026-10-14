# Proposal

## Why

The OnePlus log with `scan.timing` (2026-10-14 18:56) showed what holds the browser video scan back.
The page drew only 11–17 frames a second (`pageFps`), and each picture's round trip (75–90 ms) equalled
a page frame. The finder needed 25–45 ms. Measured in a container with the cube video at pixel ratio 3
and a throttled CPU:

- The page copied two pictures from the video 15 times a second and read one back for the guided grid,
  which the video scan does not use (`drawImage` 42 % of the page's time).
- The page's drawing cost grows with the pixel ratio: 38 frames a second at ratio 1, 23 at 1.5, 11 at 3.

## What Changes

- Browser: the camera loop draws and reads the grid picture only when something uses it (the guided
  scan, camera follow, or the video scan before its worker is ready). Otherwise it only notes a new
  camera picture for the stall check.
- Browser: up to two pictures are with the finder, the second waiting in the worker. The finder then
  starts the next one without waiting for the page.
- Browser: if the page draws fewer than 18 frames a second for two timing lines in a row (8 s) during
  a video scan, at a pixel ratio above 1.5, the app draws at ratio 1.5 from the next start. Text is a
  little softer; the phone's own ratio is kept for the log. The timing line tells the ratio used and
  when the cap is set.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `web-app`: "Speed in the browser" — the page does no reading the video scan does not use, and a slow
  phone draws at a lower pixel ratio from the next start.

## Impact

- `web`: `platform.mjs` (`cameraTake`, two pictures in flight, pixel ratio cap in the timing line),
  `index.html` (cap applied before the app starts), `WebCamera.kt` (grid picture only when needed),
  `Js.kt`.
