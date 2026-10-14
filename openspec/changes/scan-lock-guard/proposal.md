# Proposal

## Why

The OnePlus in Chrome, now with the main camera (`scan-camera-pick`), went dark mid-scan again (log
2026-10-14 18:29). The camera was locked at 1.1 s without any darker step (`darker=0 light=143`). From
the next snapshot on, no face was found at all, so the picture had gone too dark to see the cube. The
earlier failures look the same: brightness halved right after the lock. The browser's exposure lock
(`exposureMode: manual`) is what darkens it on this phone: Chrome on some Android phones holds a
default exposure time instead of the current one. The lighter step from `scan-camera-pick` cannot help,
because it needs faces to measure.

## What Changes

- Browser: the lock also passes the camera's current exposure time and ISO where the browser reports
  them, so the camera holds what it had.
- Browser: the lock is checked. The whole picture's brightness is measured just before the lock and
  0.7 s after it. If it fell below 60 %, the lock is undone (the camera adjusts by itself again). The
  lock is then skipped for that camera from then on (remembered in the browser).
- Log: the lock line tells the exposure time held. A `scan.lock` line tells the check's outcome
  (picture brightness before and after, kept or undone), and a skipped lock says so.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `web-app`: "Camera in the browser" — a lock that darkens the picture is undone and not used again on that camera.

## Impact

- `web`: `platform.mjs` (`cameraLockExposure`, picture brightness, remembered "lock darkens"), `WebCamera.kt`
  (logs the check), `Js.kt` (one external).
- No change to the phone app or the cube module.
