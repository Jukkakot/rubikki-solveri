# Tasks

## 1. Frames

- [ ] 1.1 Extract frames from both test videos with ffmpeg (rotation applied, ~360 px wide, 10 fps) into `testdata/video/2026-10-05/frames/<video>/`. Verify: frame counts printed; one frame looked at.

## 2. Face finder

- [ ] 2.1 `cube`: sticker blobs from an ARGB frame (sticker-like pixels, connected blobs, size and squareness filters). Verify: JVM unit test on a synthetic frame with a drawn 3×3 face finds nine blobs.
- [ ] 2.2 `cube`: 3×3 lattices from the blobs (two step vectors from neighbours, predicted positions, all nine must match; several per frame), with the nine colours read per lattice. Verify: unit tests with a straight and a sheared synthetic face; one of the scan pictures in `scan-pictures/` gives one lattice.

## 3. Harness and numbers

- [ ] 3.1 JVM test harness (skipped when the frames are missing, so CI passes): per video, frames with ≥1 lattice, lattices per frame, sticker colours vs the true state (best rotation), faces seen, and whether the assembled cube equals the true state. Verify: the harness prints a table for both videos.
- [ ] 3.2 Tune the thresholds once on the numbers (no overfitting to single frames), write `findings.md` in this change: numbers, example frames where it fails, and a go / no-go proposal for the live feature. Verify: findings written; the user decides the next step.
