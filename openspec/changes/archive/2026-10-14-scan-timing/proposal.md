# Proposal

## Why

On the OnePlus in Chrome the video scan reads only 13–16 pictures a second although the camera
reports 30. The finder takes about 15 ms (`finderMs`) and waits 0–3 ms between pictures (`idleMs`).
Each picture's round trip is therefore about 70 ms, and about 50 ms of it is not in the log: drawing
and reading the picture back in the worker, passing messages, or a busy page. The next log from that
phone must show where the time goes.

## What Changes

- Browser: every 4 s during the video scan a `scan.timing` line gives the following averages:
  - `camFps`: camera pictures a second as the page sees them.
  - `pageFps`: the page's animation frames a second (a busy page shows here).
  - `sentFps`: pictures sent to the finder a second.
  - `tripMs`: round trip from sending a picture to the finder's answer on the page.
  - `pixMs`: the worker drawing the picture and reading its pixels back.
  - `workMs`: the worker's whole work for a picture.
  - `pipeMs`: the scan worker's round trip.
  - The transport time is `tripMs − workMs`.
- No behaviour change.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `diagnostics`: the video scan log in the browser gains a timing line every few seconds.

## Impact

- `web`: `platform.mjs` (timers, `scanTimings()`), `Js.kt`, `WebCamera.kt` (logs the line).
- `webworker`: `worker.mjs` (pixel and work times in its answer).
