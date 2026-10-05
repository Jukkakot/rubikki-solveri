# Proposal

## Why

The video scan worked well on the phone (user, 2026-10-05). Three things would make it feel live
and faster: use faces that are only partly visible (a finger over a sticker), show on the camera
picture which colour each sticker was read as, and let the small progress cube turn with the real
cube in real time instead of jumping to a pose now and then.

## What Changes

- **Partial faces count:** faces with 7–8 of their nine stickers found vote for the stickers they
  show (the finder already finds them; today they are dropped).
- **Smaller pieces, through a spike first:** pieces smaller than 7 stickers (a 2×3 block, a row next
  to a face seen in the same frame) are measured on the test videos first; they are built in only
  if they speed up the scan without adding wrong stickers (criterion in `design.md`). Otherwise
  the finding is recorded and nothing changes.
- **Colours on the camera picture:** every sticker of a face found in the frame gets a coloured mark
  in the colour it was read as; a confirmed sticker's mark is solid, an unconfirmed one faint.
- **Progress cube follows in real time:** it turns smoothly with the real cube every frame (also the
  slant of the face, not just which face is towards the camera), and keeps its last pose when no
  face is seen.
- **Unconfirmed stickers faint:** on the progress cube a sticker with readings but not yet
  confirmed shows its leading colour faintly; grey only while nothing is read.

Not in this change: making the video scan the default or changing how the way is chosen (its own
change later).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: partial faces and pieces as input, colours on the camera picture, a progress cube
  that follows the real cube in real time and shows unconfirmed stickers faintly.

## Impact

Modules: `cube` (partial faces into `VideoScan`, faint sticker info, continuous orientation from
the faces found, harness numbers for smaller pieces) and `shared` (camera overlay, progress cube
drawing). `app` and `web` unchanged (frames already flow). Test fixtures regenerated with partial
faces.
