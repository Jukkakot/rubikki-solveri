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
- **Light colour taken out:** before colours are read, each frame's colour cast is estimated from
  its white stickers and removed, so warm or bluish light reads like daylight.
- **Glare left out:** a sticker's colour ignores its washed-out brightest pixels.
- **Torch from the notice:** where the device has a torch, the "more light" / stuck notice offers to
  turn it on.
- **The stall panel becomes a notice** at the bottom of the picture that does not cover the cube;
  scanning goes on underneath, the notice goes away when new stickers become known, and a tap
  anywhere outside it closes it. "Start over" and "fix colours" stay in the notice only.

Target: both dim evening videos finish with the true cube, no other test video gets slower or
wrong, the simulation shows no wrong finish with the threshold (re-checked, re-chosen if needed).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: readings counted by how well they fit each colour; light colour and glare taken out;
  the restart panel becomes a non-blocking notice with an optional torch button.

## Impact

Modules: `cube` (colour fit per reading, light correction, glare in `FaceFinder`'s sticker colour,
fractional evidence in `BestCube`/`VideoScan`, fixtures regenerated) and `shared` (notice, torch
button, texts). `app` and `web` unchanged (both already switch the torch).
