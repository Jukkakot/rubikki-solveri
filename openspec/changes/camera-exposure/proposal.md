# Proposal

## Why

The user's log of 2026-10-05 (Galaxy S24, Samsung Internet, a dark room with the torch): in three
attempts the stickers' median brightness was 255 nearly all the time (the picture washed out), the
red side was never recognised and the scan stalled; after the torch was turned off and on and the
scan restarted, the brightness fell to about 180 and the cube was done in 16 s. The camera meters
the whole picture, which is mostly the dark room, so the torch-lit cube burns out, and the lock
keeps that. The browser already uses the main back camera ("camera 0") with the lock supported, so
the camera itself is not the difference. More frames a second would also make each scan shorter:
both platforms read about ten a second.

## What Changes

- **Exposure and focus on the cube:** once the video scan finds a face, the camera meters and
  focuses on that spot (as a tap does in the phone's camera app), on the phone and in the browser
  where the browser allows it.
- **Exposure down when washed out:** while the stickers read washed out, the exposure is lowered
  step by step (exposure compensation) before the lock; the lock is taken once the stickers read
  well, and the torch's re-metering goes through the same steps.
- **More frames a second:** the phone reads up to 15 pictures a second instead of 10; the browser
  reads the next picture as soon as the last is done, up to 15 a second.
- **Log:** the video scan's snapshots carry frames a second and the torch state; the camera's
  controls (focus modes, exposure compensation range, metering point support, resolution) are
  logged once when the camera opens, on the phone too.
- The browser's work off the page's thread (Web Worker) is the next change, `web-scan-worker`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: a new requirement on the camera's settings during the video scan (metering and
  focus on the cube, exposure lowered when washed out, then locked; frames a second).
- `web-app`: the camera in the browser meters, focuses and lowers exposure as on the phone where the
  browser supports it, and says in the log what it supports.
- `diagnostics`: the video scan's log adds frames a second, the torch and the camera's controls.

## Impact

Modules: `cube` (the exposure steps as pure logic, tested), `shared` (video scan screen drives the
camera through `CameraPreview`; Android camera in `androidMain`: CameraX `FocusMeteringAction`,
exposure compensation, analysis rate), `web` (`platform.mjs`: `pointsOfInterest`,
`exposureCompensation`, `focusMode` constraints and the capabilities line; frame pacing in
`WebCamera.kt`), `app` (tests). The guided scan keeps its own lock as today.
