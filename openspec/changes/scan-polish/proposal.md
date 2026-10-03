## Why

Phone testing on 2026-10-03 found three problems:

- **Overexposed scan.** Exposure and white balance are locked when the first face is *accepted*. By
  then the user is already turning the cube, the camera has re-exposed for a darker view, and the
  lock keeps that bright setting: every later face read as near-white (`ffffff`, pale yellow, pale
  orange), the cube came out invalid.
- **Stutter.** The user sees a short freeze around every capture.
- **"Check colours" page is unclear.** After the invalid scan the user did not understand what to
  do there: a tall scrolling page, a note about "marked stickers", and nothing showing what the
  camera actually saw.

## What Changes

- Lock exposure and white balance at the moment the first face is **captured** (the cube is held
  still and the camera has settled on it); unlock when the front face is scanned again.
- Remove work from the main thread around a capture (picture saving), make the per-frame cube
  check allocation-free, and log any stall (camera frames or UI frames stopping for a noticeable
  time) so remaining stutter can be located from a shared log.
- Rework the manual input screen to fit one screen (product rule "no tall pages"), and in its
  check-a-scan mode show the camera picture of the face next to the editable face, a plain
  instruction ("compare with the picture; pick a colour below, tap the wrong sticker"), and a
  "Scan again" action.

Modules: `cube` (allocation-free gap check) and `app` (camera lock, scan screen, manual input
screen, strings).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `camera-scan`: exposure lock at the first capture; capture without stutter, stalls logged.
- `manual-input`: one screen; checking a scan shows the camera picture, a clear instruction and
  scan again.

## Impact

- `app/ui/scan`: `ScanScreen` (lock timing, pictures handed to the check, save off the main
  thread, stall logging), `CameraPreview` (camera stall logging).
- `app/ui/manual/ManualInputScreen` (layout, check mode), navigation (scan again from the check).
- `cube/scan/FrameSampler.gapContrast` (no boxing).
- Strings fi/en; screenshot of the check page; docs.
