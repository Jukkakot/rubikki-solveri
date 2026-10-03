## Why

The colour-based "looks like a cube" check from `scan-cube-check` let a light grey desk through
(eight cells read as "bright white") and, by the earlier phone logs, would refuse this cube's white
face in shadow (it read at lightness ~25). Colour cannot tell a white sticker from a grey surface.
The pictures from the first successful phone scan show what does: black gaps between the stickers.
Measured on those pictures, every cell of all six faces has a centre-vs-edge lightness contrast of
27 or more; the desk has it in one cell only (a dark corner).

## What Changes

- The cube check looks at the grid picture instead of the nine colours: a cell "looks like a
  sticker" when its middle is clearly lighter than the darkest part of its edge (the gap). At least
  six of nine cells must; otherwise no automatic capture, as before.
- The colour rule (chroma / lightness) is removed.
- The user's first successful phone scan (six faces' readings, valid, 0 uncertain) becomes a
  regression test of the classification; the seven pictures become test fixtures for the check.

Modules: `cube` (check from a picture, tests and fixtures) and `app` (pass the check into the scan).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `camera-scan`: "No cube in the grid" is decided by the gaps between stickers, not by colour.

## Impact

- `cube/scan`: `FrameSampler` (gap contrast from a picture), `ScanSession` (check passed in,
  colour rule removed); test resources with the phone pictures.
- `app/ui/scan`: `CameraPreview` sets the picture before the readings; `ScanContent` passes the
  check result; tests.
- Limitation: stickerless cubes (no dark gaps) won't auto-capture; the capture button still works.
