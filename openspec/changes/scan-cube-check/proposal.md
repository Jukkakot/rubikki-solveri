## Why

Since `scan-flow` a face is captured whenever the readings stay steady, so the scan captured the
user's desk and mouse pad with no cube in view. And when a scan goes wrong on the phone, the log
only has nine numbers per face: neither the user nor Claude can see what the camera saw.

## What Changes

- The scan checks that the grid looks like cube stickers (each cell a strong colour or a bright
  white-ish one). If not, the status says no cube is seen in the grid and nothing is captured
  automatically; the capture button still works (guide, don't block).
- Every capture saves a small picture of the grid area on the phone (newest 12 kept). The log line
  of the capture names the picture.
- Sharing the log sends the pictures with it; clearing the log deletes them.

Modules: `cube` (cube check, grid picture from a frame) and `app` (camera, scan screen, picture
files, log share).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `camera-scan`: new requirement — no automatic capture when the grid shows no cube.
- `diagnostics`: new requirement — scan pictures kept on the phone and shared with the log.

## Impact

- `cube/scan`: `ScanSession` (new `NoCube` event, plausibility check), `FrameSampler` (grid picture).
- `app/ui/scan`: `CameraPreview` (latest grid picture), `ScanScreen` (status text, saving pictures,
  capture log line); `app/ui/log/LogScreen` (share several files, clear pictures); strings fi/en.
- `docs/operations.md` "Tuning the camera scan" (pictures, how to give them to Claude).
