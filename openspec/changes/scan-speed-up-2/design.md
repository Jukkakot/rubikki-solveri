# Design: scan-speed-up-2

## Context

See the proposal's table. Pictures flow: camera → finder picture (phone: `FrameSampler.upright`,
short side 360; browser: the cover crop at long side 360 into the worker) → `FaceFinder.find` →
`FoundFaces` → `VideoScan.onFrame` in `VideoScanContent`'s collector (main thread) → paint. The phone
finder runs in one coroutine on `Dispatchers.Default`; the browser in one worker (`scan-worker.js`).

## Decisions

1. **One finder size, measured.** `FrameSampler.upright` takes a long side (`FINDER_LONG_SIDE`), as the
   browser already does. The value is chosen with the video harness on the recorded videos under
   `testdata/video/`: for 360, 320, 280 and 240 the finder's full faces per picture, its time, and
   whether the whole scan (finder + `VideoScan`) still reads the true cube where one is known. The
   smallest size whose full faces stay within 5 % of 360 and whose scans stay right is taken; the
   numbers go into this file. The picture shown is not affected (it is made separately).
2. **Phone build first measured, then changed.** A `profile` build type (as debug, but not
   debuggable, signed with the debug key so it installs over the debug app without uninstalling) is
   installed once with adb to compare `finderMs` with the debug build on the same scan. If it is
   clearly faster (≥ 25 %), the debug build the user runs from Android Studio is made non-debuggable
   too (the user does not use the debugger); otherwise the type is removed again. Result recorded here.
3. **Search window (`FinderWindow`, pure, in `cube`).** In finder-picture pixels: after a picture with
   at least one full face, the window is the bounding box of all faces' outer corners (full and
   partial), grown by half its width and height on each side, clamped to the picture. A picture is
   searched whole when there is no window, every 5th search, and after any search without a full
   face. The platform crops the finder picture to the window (phone: from the `ArgbImage`; browser:
   the page sends only that part of the video, at the same scale) and the faces found are moved back
   by the window's corner. The finder's minimum sizes are in pixels, so the same scale keeps results
   the same as on the whole picture. Not tuned for very fast turns (user, 2026-10-08): a cube that
   leaves the window is found again within at most 5 pictures.
4. **Two finders in turn.** Phone: two coroutines on `Dispatchers.Default` take pictures from the
   camera's buffer (each the newest when it is free); the browser: two workers, the page sends to
   whichever is idle. Each answer carries its picture's number; an answer older than the newest
   already passed on is dropped (in the browser its picture copy is freed). The window (3) is kept
   by the side that crops (phone: the finder loop, shared by both; browser: the page).
5. **Scan logic off the drawing thread.** Phone: `VideoScan.onFrame` runs in the finder pipeline's
   own single-threaded context (one at a time, in picture order), the state is handed to Compose.
   Browser: a third worker (`scan-logic.js`, the `webworker` module with a second entry) owns the
   `VideoScan`; the page forwards each picture's faces to it and gets back the state for paint, ring
   and status, encoded with kotlinx.serialization (added to `cube` for `VideoScanState`,
   `ScanOutcome` and the log's centre line). Reset, `outcome()` and the engine are messages. The
   paint layer stays on the page. Without workers everything stays on the page as now.

## Risks / Trade-offs

- [A smaller finder picture loses a small, far-away cube] → measured (1); the user holds the cube
  near the camera.
- [The window misses a cube that jumped] → whole search every 5th picture and after a miss.
- [Two finders deliver out of order] → dropped by number.
- [Serialising the state each picture costs on both sides] → only the fields the screen uses;
  measured in the log (`scanMs` becomes the worker's time, a new `stateMs` the page's decode).
- [Item 5 is the biggest] → last, after 1–4 are measured; if 1–4 already reach the camera's rate in
  the browser, 5 can be stopped and recorded as not needed (asked of the user then).

## Findings

**Finder size (task 1.1, `FinderSizeHarness`, JVM desktop, committed stills scaled bilinear):**

| video | 640 (phone before) | 480 | 360 (browser before) | 300 | 240 |
|---|---|---|---|---|---|
| 20261005_151828 | 1.04 faces, 16.5 ms, clear@223 ok | 1.04, 8.6 | 1.04, 5.6, ok | 1.06, 4.7, ok | 1.06, 3.7, clear@223 ok |
| web_181940 | 1.03, 14.0, clear@197 ok | 1.02, 8.5, @111 | 1.03, 5.9, @108 | 1.03, 5.2, @111 | 1.03, 4.2, clear@108 ok |
| 20261007_202058 | 0.52, 13.0, – | 0.51, 6.5 | 0.52, 4.1 | 0.53, 3.3 | 0.50, 2.6, – |
| web_084657 (scan 1) | 0.56, 13.3, clear@100 ok | 0.57, 8.1, @100 | 0.59, 5.5, @112 | 0.58, 4.5, @100 | 0.58, 3.7, clear@100 ok |
| web_121505 (rules 3) | 0.79, 15.7, – | 0.79, 9.7 | 0.79, 6.8 | 0.76, 5.8 | 0.78, 5.0, – |

Full faces per picture do not change down to 240, the scans clear as early and right (the two
without a clear never cleared before either). **Chosen: `FINDER_LONG_SIDE = 240`** (phone finder ~4×
less work, browser ~1.4×). A cube far away and small may be read less well (accepted by the user).
