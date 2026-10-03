# Proposal

## Why

On the phone, red and orange were hard to tell apart. The live reading (grid dots, the review and
the centre check) compares against a fixed default palette, and the camera re-adjusts exposure and
white balance for every face, so the same red can read red on one face and orange on another.
Worse, a cube whose red reads as the default orange could not be scanned at all: the red centre
failed the centre check.

## What Changes

- Exposure and white balance are locked after the first face, so all faces are read alike.
- The live reading learns the cube: once a face is accepted, its centre's reading becomes the
  reference for that colour.
- In the review, tapping a sticker changes it to the next closest colour. The correction is kept in
  the result and teaches the live reading for the following faces.
- The centre check accepts the expected centre also when it reads second closest after a colour the
  cube has not shown yet.
- The log records the corrections and the lock, for tuning.

## Capabilities

### Modified Capabilities
- `camera-scan`: calibrated live reading, tap-to-fix in the review, exposure lock.

## Impact

`ColorClassifier` (references, ranked reading, fixed stickers), `ScanSession` (references, cycle,
corrections), `ScanContent` review tiles, `CameraPreview` AE/AWB lock via Camera2 interop.
