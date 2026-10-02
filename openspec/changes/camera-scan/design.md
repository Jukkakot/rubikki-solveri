# Design

## Context

Stickerless standard-colour cube (product.md). The phone is the only place the camera can be
tested, so everything except the camera plumbing lives in the pure `cube` module (`scan` package)
with synthetic-image tests.

## Goals / Non-Goals

**Goals:** robust colours under ordinary indoor light; never a silent wrong cube; easy tuning
from a shared log.

**Non-Goals:** finding the cube anywhere in the frame (the user aligns it to the grid), 3D
tracking (`camera-follow`), machine-learning models.

## Decisions

- **CameraX `LifecycleCameraController` + `PreviewView`** in an `AndroidView`. The controller
  applies the preview's viewport to image analysis, so the analysis `cropRect` is exactly the
  visible preview. Analysis output RGBA_8888, keep-only-latest.
- **Grid geometry**: a centred square of 72 % of the shorter side of the visible area, both on
  screen and in the crop rect (the shorter side is rotation-invariant). Cell (row, col) in screen
  orientation maps to image coordinates by the frame's rotation (0/90/180/270), a pure function
  tested by rotating synthetic images.
- **Sampling**: per cell the middle 40 % (avoids the dark gaps and edges), every 2nd pixel, the
  per-channel median (robust to glare).
- **Colour space**: CIE Lab from sRGB (D65). Distance weights lightness by 0.5, because lighting
  changes lightness most while hue separates the colours.
- **Live reading** (per frame): nearest of a default reference palette for stickerless cubes,
  only for the dots and the centre check.
- **Final classification**: balanced assignment — every colour exactly nine stickers — by the
  Hungarian algorithm on a 54×54 cost matrix (each centre colour repeated nine times), centres
  fixed. References start at the six centre readings and are refined twice to the mean of their
  nine stickers (balanced k-means). Confidence per sticker = (d₂ − d₁)/(d₁ + d₂) between the
  nearest and second-nearest reference; below 0.12 is uncertain. Red/orange and white/yellow are
  the usual confusions; the balance constraint resolves most of them.
- **Capture**: 6 consecutive analysed frames (~0.5 s at the usual 10–15 fps) with identical live
  readings and the right centre → capture the median of those frames' samples. The capture button
  takes the latest frame. Haptic `Confirm` on capture.
- **Session** (`ScanSession`, pure): current face, captured samples, stability counter, events
  (captured / wrong face / waiting), redo; result builds a `CubeEditor`, uncertain set and validity.
- **Result routing**: valid and no uncertain stickers → `SolveRoute`; otherwise `ManualInputRoute`
  with the colours, marks (uncertain ∪ validity stickers) and `fromScan`.
- **Logging**: `scan.face` with the nine raw RGB samples as hex and the live reading; `scan.done`
  with validity and uncertain count. Never images.
- **Permission**: `ActivityResultContracts.RequestPermission`; `CAMERA` in the manifest with
  `uses-feature android.hardware.camera` not required.

## Risks / Trade-offs

- [Real lighting/camera behaviour differs from synthetic tests] → thresholds are constants in one
  place; raw samples in the log make tuning from a shared log possible. Listed for the phone check.
- [Auto white balance shifts between faces] → classification uses all six faces together and the
  balance constraint; lightness is down-weighted.

## Decisions made while building

- `Validity.markedStickers` moved into `cube` so the scan outcome can mark problem stickers.
- The manual editor opens on the face of the first marked sticker; scan marks clear on the first
  edit, together with the note.
- Torch icon is a small vector drawable (the core icon set has no flashlight).
- A Gradle init script outside the repo (container only) points at Google's Maven Central mirror,
  because Maven Central rate-limited the container (HTTP 429). CI and Android Studio are unaffected.
