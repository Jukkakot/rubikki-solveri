# Proposal

## Why

The user wants to scan by just turning the cube in front of the camera: no grid to line up, no
holding still, no separate captures. The app works out the state bit by bit from the video and
shows on a 3D cube what it has recognised so far, with visual hints on how to turn the cube to show
what is still missing. The guided scan stays as an option (user, 2026-10-05).

## What Changes

- **Video scan screen:** the camera fills the screen, no grid; faces found in the picture are
  outlined; a 3D progress cube starts grey and fills in sticker by sticker as stickers are
  recognised; a turning hint (arrow on the progress cube and a short line) shows how to turn the
  cube to show unrecognised stickers; a small vibration when new stickers are recognised.
- **Recognition from video:** 3×3 sticker lattices are found anywhere in a frame, straight on or at
  an angle, several per frame; every reading votes for its stickers; a sticker counts as
  recognised once enough frames agree. The cube's pose (which face is towards the camera and which
  is up) is followed from the faces seen, for the progress cube and the hints.
- **Finishing:** when all 54 stickers are recognised and the cube is possible, and it stays so for
  about half a second, the solution opens, with the colour check behind it (like a sure scan
  today). Stickers that contradict each other are marked on the progress cube and the hint asks to
  show them again.
- **Two ways to scan:** the guided scan (grid and captures) stays. Until the video scan is reliable
  the guided scan is the default and the video scan is offered beside it; once it is reliable the
  video scan becomes the default.
- **Built in steps:** `video-scan-spike` first measures the face finder on the user's videos
  (go / no-go); this change is built only after a "go".

## Capabilities

### New Capabilities
- `video-scan`: scanning from continuous video with a filling 3D progress cube and turning hints.

### Modified Capabilities

(The choice between the two ways is in the new `video-scan` spec; the guided scan's own
requirements do not change.)

## Impact

`cube` (face finder, sticker votes, pose tracking, pure Kotlin), `shared` (video scan screen,
progress cube, hints, navigation), platform camera frames on Android and in the browser (already
there for the guided scan). The test videos become regression tests.
