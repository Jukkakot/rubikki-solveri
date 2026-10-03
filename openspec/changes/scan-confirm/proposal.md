# Proposal

## Why

On the phone the scan captured a face too early: six steady frames (about half a second) was often
reached while the user was still turning the cube into place, so a wrong or half-turned face was
taken and the scan jumped on. The user wants a longer hold and a chance to check each face.

## What Changes

- A face is captured only after it has been held steady with the right centre for about 1.5 seconds
  (time-based, not frame-based). A progress bar under the grid fills while holding, so the user
  sees that the capture is coming.
- After a capture the camera reading pauses and the screen shows the nine colours as read, with
  "Looks right" (next face) and "Scan again" (same face). The capture button goes through the same
  check.
- "Redo previous" stays for going back one face after confirming.

## Capabilities

### Modified Capabilities
- `camera-scan`: capture timing and a confirm step per face.

## Impact

`ScanSession` (cube module) gains the hold time, a review state and accept/retake; `ScanContent`
shows the hold progress and the review. Existing scan tests change to confirm each face.
