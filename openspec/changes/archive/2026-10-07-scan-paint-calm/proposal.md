# Proposal

## Why

The user's web test on 2026-10-07 at 14:55 (`testdata/video/2026-10-07/web-145543/`): the screen
is busy. Every sticker gets a full tile in the read colour and every needed one a dashed grey tile;
when the cube is close or moving the tiles lag, sit beside the stickers or float in the air next to
the cube (10–14 s, 20–22 s, 37–38 s of the recording), and wrong readings show as confident tiles.
The user asked for calmer movement and less on screen.

Six options were mocked up (`mockup.html`, also https://claude.ai/artifact/5jyg4mKrJLu2ZkhrYnHNX5);
the user chose **C + D** (2026-10-07): mark only what is still needed, and mark a finished side.

## What Changes

- **Known stickers are left as they are.** No tile over a known sticker; the real cube shows.
- **Still-needed stickers get a grey veil:** a filled grey tile, no dashed line, so the grey parts
  are what is left to show (the status line keeps asking for them).
- **A finished side** (confirmed by the rest of the cube, as now) keeps its white outline and gets a
  small tick at its centre.
- **The marks fade while the cube moves** and come back when it rests, so nothing floats beside a
  moving cube. They also fade, as now, when the cube is out of view.
- No read colours on screen any more: a misread sticker is no longer shown in its wrong colour.
  Accepted by the user with this choice; `scan-centre-naming` removes the main source of misreads.

Decisions taken here (small change, no `design.md`):
- "Moving": the paint's own glide (`Glide`) already tracks where the stickers are; the marks fade
  when the cube's centre moves faster than about a third of a side per second, and come back after
  it has stayed below that for about 0.3 s. Tuned on the phone by the user.
- The dim outline of a face found in the picture stays (it is how the user sees that the app sees a
  face) but fades with the marks while moving.
- The vibration when stickers become known stays.

Decisions while implementing (2026-10-07):
- The dim outline of a found face did not exist yet; it is added (a thin white line round each face
  found that is not a confirmed side), since with known stickers left bare a fully known face would
  otherwise show nothing.
- The speed is measured on the projection's centre (the whole cube), else on the largest face found:
  in a corner view the largest face alternates between two faces and its centre would jump.
- The screen smoke test is the existing `VideoScanScreenTest` (a partly known cube, the whole cube
  confirmed), which draws veils, outlines and ticks; no extra test.
- Two follow-ups of `scan-centre-naming` found by those tests, fixed here: a second lattice on a face
  the picture already has (same stickers) is left out instead of starting a pile, and a new pile whose
  own clear colour is free counts at once (its centre is known from the first picture, as before).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `video-scan`: "Colours on the camera picture" (no read colours, grey veil on needed stickers
  only), "Progress on the real cube" (known left bare, tick on a finished side), "Paint follows the
  cube" (fades while the cube moves).

## Impact

- Modules: `shared` only (`ScanPaint`, the paint layer in `VideoScanScreen`). No change in `cube`,
  `app` or `web`.
- The screen gallery is not refreshed (UI change described in words).
