## Context

`ScanSession.looksLikeCube(labs)` counts cells that are neither chroma ≥ 20 nor L ≥ 55. The grid
picture (`FrameSampler.picture`, 120×120, 40 px per cell) is already computed every frame for the
capture pictures. Phone data (2026-10-03): seven pictures in the device's `files/logs/scan/`, one
desk and one full valid scan.

## Goals / Non-Goals

**Goals:** reject desks and similar surfaces whatever their colour; accept every face of the user's
cube, including a dark white face.

**Non-Goals:** stickerless cubes (they have no dark gaps; the capture button covers them); finding
the cube's outline or perspective.

## Decisions

- **Gap contrast per cell.** In each 40×40 cell of the picture: median lightness (Lab L) of the
  middle 16×16 minus the 10th percentile of the outer 8 px ring. Measured: cube cells 27…76, desk
  cells 2…5 (one 55 at the dark corner). Threshold **15**, at least **6 of 9** cells: robust to a
  grid shifted half a sticker (the L-face picture) and to glare on a few cells.
  Alternatives: colour rule (fails, see proposal); edge detection along the grid lines (needs exact
  alignment, which users don't keep).
- **Check stays in the session's flow, computed in the app.** `ScanSession.onFrame(samples, now,
  looksLikeCube = true)`; `FrameSampler.looksLikeCube(picture)` is pure and tested with the real
  pictures as fixtures. `CameraPreview` hands over the picture before the readings so both come
  from the same frame; `ScanContent` gets a `looksLikeCube: () -> Boolean` (default true for tests
  and the screenshot).
- **Regression test from the phone.** The six `scan.face` readings of the successful scan go into a
  cube test: classification gives exactly the logged cube, valid, nothing uncertain.
- **Fixtures** live in `cube/src/test/resources/scan/` as the PNGs pulled from the phone; the
  JVM test decodes them with `javax.imageio` (no Android needed).

## Risks / Trade-offs

- [Another cube with grey/white plastic] → no gap contrast, button fallback; a future change can
  add a second cue once a picture of such a cube exists.
- [A textured surface with dark lines (tiles, a keyboard)] → may pass; the end check catches it.
