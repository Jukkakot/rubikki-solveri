## Context

`ScanSession.onFrame` captures after every cell stays within ΔE 12 for 1.5 s; nothing checks that
the cells are stickers. `CameraPreview` turns each analysis frame (RGBA, ~640×480) into nine
readings with `FrameSampler.sample`. The log share sends `files/logs/app.log` through the
FileProvider root `logs/`.

## Goals / Non-Goals

**Goals:** a desk, mouse pad or hand in the grid does not auto-capture; Claude can see what the
camera saw for each capture.

**Non-Goals:** recognising the cube's outline or the gaps between stickers (fragile with
stickerless cubes and loose alignment); full-resolution photos; uploading anything.

## Decisions

- **Plausible sticker = chroma ≥ 20 or L ≥ 55 (CIE Lab).** Cube colours are either clearly
  coloured (red, orange, yellow, green, blue — blue is dark but saturated) or bright (white).
  Dark, grey and brown surfaces fail. At most one implausible cell is allowed (glare, a shadow).
  A light grey desk can still pass; the pictures will show whether that matters. Constants live in
  `ScanSession` for tuning. Rejected: distance to the palette (a grey desk is as close to white as a
  dim white sticker is); sticker-gap detection (see Non-Goals).
- **New `ScanEvent.NoCube`** checked before the steadiness streak; it resets the streak, so the
  progress bar stays empty. `captureNow` ignores the check (the user decides). The previous-face
  check comes first, as now.
- **Grid picture in the cube module.** `FrameSampler.picture(frame, size = 120)` returns upright
  ARGB pixels of the grid square (nearest neighbour through `toFrame`), so it is unit-testable and
  matches exactly what is sampled. `CameraPreview` keeps the latest one per frame (≈14 k pixel reads,
  negligible) in a holder the scan screen reads at capture time.
- **Files:** `files/logs/scan/<yyyyMMdd-HHmmss>-<face>.png`, newest 12 kept, written on capture
  (a 120×120 PNG takes a few milliseconds). New log event `scan.capture face=… picture=… rgb=…` at every capture (auto or button),
  so retakes are visible too; `scan.face` on accept stays.
- **Share:** `ACTION_SEND_MULTIPLE`, type `*/*`, the log plus the pictures; with no pictures it is
  the same single-file share as before. Clear deletes the `scan/` folder. The pictures exist only on
  the phone until shared — this replaces the earlier "numbers, never pictures" rule, at the user's
  request for debugging. No setting: 12 small PNGs (~20 kB each) cost nothing.
- **Getting them to Claude:** share to Google Drive; Claude reads them with the Drive connector.

## Risks / Trade-offs

- [A real cube in poor light fails the check] → status tells the user, the capture button works,
  and the pictures show the case so the thresholds can be tuned.
- [Grey desk passes] → acceptable for now; the end check catches a nonsense cube.
