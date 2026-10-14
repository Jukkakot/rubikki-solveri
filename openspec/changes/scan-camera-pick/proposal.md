# Proposal

## Why

A first video scan in Chrome on a OnePlus phone (log 2026-10-14) never finished. The browser opened
`camera 3`, a back camera with only manual focus, which gave about 12 pictures a second (the user's
phone 18–25). The scan made it two to three steps darker within the first second, locked it, and the
picture then kept getting darker by about half (`light` 233 → 100). Nothing brings it back lighter. The
user saw a dark, lagging picture. We cannot test on that phone, so the next try must either fix it or
show in the log what went wrong.

## What Changes

- Browser: the back camera is chosen by what it can do. The camera kept is a back camera with continuous
  focus and a torch, then one with continuous focus, then one with a torch, else the browser's own
  pick. Cameras with the same abilities are ranked by the lowest number in their name. The browser's
  pick is kept at once when it already has continuous focus and a torch. The choice is remembered as
  before. Each phone chooses once more under the new rule.
- Browser: the camera is asked for about 30 pictures a second.
- Both platforms (shared exposure control): after the lock, if the cube's stickers stay much darker than
  at the lock for a second, the camera is unlocked and set one step lighter, or metered again by itself
  when it is not set darker at all. It is not made darker again than that until the torch changes.
- Both platforms: a darker step is judged only once the picture has actually changed, or after a longer
  wait. This stops extra steps on a camera that adjusts slowly.
- Log: the camera line lists the back cameras tried, with their focus and torch, and the frame rate
  the camera gives. Every darker or lighter step and the lock get a line with the cube's brightness.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `web-app`: the camera choice in the browser (by focus and torch, not only torch); about 30 pictures a second asked.
- `video-scan`: "Camera set for the cube" gains the step lighter after a lock that turned dark, and the darker step that waits for the picture to change.
- `diagnostics`: the video scan log names the cameras tried and the frame rate, and gives a line with brightness for each exposure step.

## Impact

- `cube`: `ExposureControl` (lighter step, step settles on a picture change, brightness on its events) and its tests.
- `app` (shared): `VideoScanScreen`'s logger lines for the steps.
- `web`: `platform.mjs` camera choice, frame rate, camera line.
- Android camera choice is unchanged (CameraX opens the main camera).
