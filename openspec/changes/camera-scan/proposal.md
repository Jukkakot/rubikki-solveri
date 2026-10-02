# Proposal

## Why

Painting 54 stickers by hand is slow; the product's main entry is pointing the phone at the cube.
The scan must be quick and, when it is unsure, hand over to the manual editor with the doubtful
stickers marked, never silently guess.

## What Changes

- Guided six-face scan with the back camera: a 3×3 grid over the live preview, the face to show
  and how to hold the cube (same order and holds as manual input), live colour dots per cell, a
  warning when the wrong face is shown, automatic capture when the cube is held still (with a
  vibration), a manual capture button, redo of the previous face and a torch toggle.
- Colour classification calibrated by the cube's own centres: every sticker is assigned to a centre
  colour so that each colour is used exactly nine times; a confidence per sticker.
- After the sixth face: a valid and confident scan goes straight to the solution (product.md);
  otherwise the manual editor opens prefilled, with the uncertain or problem stickers marked.
- Camera permission flow with a manual-input fallback.
- Raw colour samples (numbers only, never images) are logged per face so tuning can be done from
  a shared log.

## Capabilities

### New Capabilities
- `camera-scan`: reading a real cube's colours with the camera.

### Modified Capabilities
- `manual-input`: can open prefilled from a scan, with marked stickers and a note.
- `app-shell`: the scan entry on the home screen becomes available.

## Impact

- `cube`: new `scan` package (image sampling, colour classification, scan session) — pure Kotlin.
- `app`: CameraX (core, camera2, lifecycle, view 1.6.2), `ScanScreen`, camera permission, routes.
