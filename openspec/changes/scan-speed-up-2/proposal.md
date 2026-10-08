# Proposal: scan-speed-up-2

## Why

The user wants the video scan quicker and accepts a lower picture quality for it (2026-10-08). Logs of
the same day, with the cube in view:

| | finder | scan logic | pictures/s |
|---|---|---|---|
| phone app | 40–60 ms (peaks 90–107) | 9–15 ms (peak 55) | 16–21 |
| browser | 23–32 ms (worker) | 8–28 ms (page thread) | 9–18 |

The finder (`FaceFinder`) is the main cost. The phone's finder picture is about three times the
browser's (short side 360 px, e.g. 295×640, against the browser's long side 360, e.g. 166×360).
The scan logic runs on the drawing thread on both (the browser has only one; on the phone the
collecting coroutine is on the main thread).

## What Changes

In this order, each measured before the next (user: try 1 and 5 first):

1. **Smaller finder picture:** both platforms give the finder a picture whose long side is the same
   (`FINDER_LONG_SIDE`), chosen by measurement on the recorded videos (360 or smaller, see design).
2. **Phone build speed (finding the cause of 40–60 ms):** measure the finder in a non-debuggable
   build; the debug build the user installs runs with the debugger's handicaps. Act on what is found.
3. **Search near the cube:** after a picture with faces, the next search covers only the faces' area
   grown by half its size; the whole picture every 5th search and after any search without a full
   face. Not tuned for very fast turns (user).
4. **Two finders side by side:** two workers in the browser, two finder coroutines on the phone,
   taking pictures in turn; an answer older than one already shown is dropped.
5. **Scan logic off the drawing thread:** on the phone the scan runs on a background thread; in the
   browser in its own worker (the faces go to it, its state comes back to the page).

Nothing the user sees changes apart from speed (and, with item 1, possibly a far-away small cube read
less well). Specs unchanged (performance only).

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

- `cube`: `FrameSampler` (long-side finder picture), a `FinderWindow` (search area), a codec for the
  scan state and outcome (browser scan worker).
- `shared`: the finder loop (window, two finders, scan off the main thread), log fields.
- `webworker`: a second worker kind (the scan) beside the finder; `web`: `platform.mjs` (two finder
  workers, the scan worker, the window crop), `WebCamera.kt`.
- `app`: possibly a build setting (item 2).
