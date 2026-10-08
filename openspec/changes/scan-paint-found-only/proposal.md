# Proposal: scan-paint-found-only

## Why

Browser screen recording (2026-10-08 18:00): the scan works, but the grey marks still flicker and
distract. Nearly all of it comes from the cube's *guessed* sides (the projection): big yellow and
white dots float in the air above the cube for a second, grey veils and thin ghost outlines lie
beside it, and the guessed cube jumps from picture to picture. With the cube tilted and moving in the
hand the pose estimate is not good enough, and the held projection (1.5 s) keeps a wrong one on
screen. The faces the finder found are always on the stickers.

## What Changes

- **Marks only on faces found in the picture.** The projection's sides are not painted at all
  (veils, rings, dots, outlines, ticks). Which sides are still to show is told by the six-colour ring
  and the turn demo cube (both from `scan-feedback`). The scan itself still works out the pose and
  the projection (the turn demo uses the orientation); only the paint stops using the projection.
- **Thin outline only for a face read steadily** (its track counts: `FoundFace.read` is set; the
  look scanner: a face whose pile is named), so a lattice found for one picture across an edge or
  beside the cube gets none (user: keep the outline unless it floats too).
- Confirmed sides keep their white outline and tick, on found faces.
- The projection's age fade (`paintAlpha`) no longer has anything to fade; the motion centre for
  the live-picture fallback comes from the largest face found.

Not done: improving the pose estimate (the reason for this change is that it is not reliable enough
in the hand; marking guessed sides can come back later with a better estimate).

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: "Progress on the real cube" — marks only on faces found; outline only for faces read
  steadily; the projection's veils on sides at an angle are dropped.

## Impact

- `shared`: `ScanPaint.of` drops the projection part and the outline of faces not read steadily;
  `PaintLayer` drops the projection's alpha and centre.
- `app`: `ScanPaintTest` (projection tests replaced). `cube`, `web`: none.
