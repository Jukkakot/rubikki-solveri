# Proposal

## Why

Browser phone test 2026-10-09 11:54: a video scan locked the white face 180° wrong in its first
picture and never finished. The log showed what went wrong, but not why. A screen recording of the
scan could not be replayed either. The scan's own marks on the picture are read as sticker colours,
and the finder found faces in under half the pictures, so the replay knew only 12 stickers where
the phone had all six sides. A failed scan is therefore lost as test material unless the user
happens to film it again with a clean camera video. The user called catching the exact failing
scan "really important".

The scan already turns every picture into a short list of found faces (place and nine colours).
That list is all the scan logic sees. Recording it gives the very input of the failing scan, which
replays exactly on the JVM with no camera video, no cropping and no finder differences.

## What Changes

- Every video scan records, per picture read, its time and the faces found (places and nine
  colours), together with how the scan ended (finished, left, restarted).
- The newest three recordings are kept beside the log, each the last 90 seconds of its scan.
  Sharing the log sends them with it, and clearing the log deletes them. They never leave the
  device otherwise.
- A shared recording goes into the test fixtures as a file. A harness replays it with its real
  times and prints the timeline. A test fixes the scan's outcome once the bug is fixed.
- A setting "Piilota skannauksen merkit" (hide scan marks) shows the camera picture without veils,
  rings, dots or outlines, for clean screen recordings. Off by default and kept across starts. The
  progress ring and status line stay.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `diagnostics`: a new requirement "Video scan recordings" (record, keep, share, clear).
- `video-scan`: a new requirement "Marks can be hidden" (the setting).

## Impact

- `cube`: a text format for a recording (header, one line per picture using the existing face
  encoding, end line), a writer and a reader. It is shared by the app and the test harness.
- `shared`: a recording store interface beside `ScanPictureStore`. `VideoScanScreen` records each
  picture where the faces reach the scan, on the phone and in the browser. Share and clear include
  recordings. A setting and its Settings row.
- `app`: a file store in the app's files directory, and sharing through the existing share intent.
- `web`: a store in the browser's key-value store; recordings join the share sheet / zip download.
- Test harness: `RecordingReplay` and a loader in `VideoFixtures`. Docs: the recording flow in
  `docs/development.md`.
