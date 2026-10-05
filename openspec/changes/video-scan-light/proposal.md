# Proposal

## Why

Two of the four evening test videos (2026-10-05: dim ceiling light, another room) never finish:
a whole side's red is read as orange. Each frame names every sticker as one colour before voting,
so a reading that is nearly halfway between red and orange counts as a sure orange, and warm light
pushes red towards orange on every frame alike. The camera evens out brightness, so the scan cannot
tell this light from good light by brightness alone. Speed is not the problem (finder 17–22 ms per
frame on the phone in the browser); GPU or a Web Worker would not help the colours.

Phone test of `video-scan-progress` (user): the restart panel covers the cube and leaves only
"start over" or "fix colours"; the user must always be able to keep scanning.

## What Changes

- **Readings stay as sure as they are:** each reading tells how well it fits every colour, not just
  its nearest one. A borderline red/orange reading stays borderline, and the best possible cube
  decides from the rest of the cube (`BestCube` evidence becomes fractional votes).
- **Glare left out:** a sticker's colour ignores its washed-out brightest pixels.
- **Torch from the notice:** where the device has a torch, the "more light" / stuck notice offers to
  turn it on.
- **The stall panel becomes a notice** at the bottom of the picture that does not cover the cube;
  scanning goes on underneath, each stall waits 5 s longer before the notice shows, it goes once
  the scan gets on, and a tap anywhere outside it closes it for good (that reason). It describes the situation ("Heikko
  valaistus") instead of giving orders. "Start over" and "fix colours" stay in the notice only.
- **Torch re-meters the camera:** turning it on or off lets exposure and white balance settle and
  lock again (screenshots 2026-10-05: torch on after the lock washed the picture out). Washed-out
  readings count little.
- **Tick only when confirmed:** a side gets its tick when the rest of the cube confirms it, not on
  its own readings.
- **Sharp browser picture:** the browser shows the camera's own video under the app (today a
  360-px copy redrawn at most 15 times a second: blurry and jerky).

Target: both dim evening videos finish with the true cube, no other test video gets slower or
wrong, the simulation shows no wrong finish with the threshold (re-checked, re-chosen if needed).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: readings counted by how well they fit each colour; glare and washed-out readings
  handled; torch re-meters; tick only when confirmed; the restart panel becomes a
  non-blocking notice with a torch toggle.
- `web-app`: the browser's camera picture as sharp and smooth as the camera delivers it.

## Impact

Modules: `cube` (colour fit per reading, glare in `FaceFinder`'s sticker colour,
fractional evidence in `BestCube`/`VideoScan`, fixtures regenerated), `shared` (notice, torch
toggle, texts, re-metering), `web` (video element under the app, re-metering in `platform.mjs`) and
`app` (tests).
