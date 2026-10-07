# Tasks

## 1. Exposure control (cube)

- [ ] 1.1 Every frame is read, in every phase (searching, metering, darker steps, re-metering after a torch change or washed-out stickers); the control only steers the camera; verify: JVM tests in `ExposureControlTest` that frames are read during metering and after a torch change
- [ ] 1.2 Metering point = middle of all faces found in the frame (not the largest face); verify: JVM test that two faces whose sizes swap from frame to frame do not move the point or restart the settle
- [ ] 1.3 Metering time limit (about 1 s from the first face): lock then unless a darkening step is waiting; washed-out darkening steps still run; verify: JVM tests that a cube moving every frame locks by the limit, and that washed-out frames still step darker before locking
- [ ] 1.4 Regression: the existing video-scan regression tests (the dim and warm-light videos included) still finish with the true cube now that pre-lock frames count; a test of the 2026-10-07 case (two faces, cube turned, `darker=0`) where stickers are known before the lock

## 2. Spinner (shared)

- [ ] 2.1 While a face is found and no sticker is read yet, a small spinner shows on the video scan's picture (no text); "show the cube" stays for no cube in view; verify: a Compose or state test that the spinner shows after a face is found and goes away at the first reading

## 3. Docs

- [ ] 3.1 Mark `scan-start` done in `openspec/context/roadmap.md`; touch `docs/` only if a page describes the exposure steps in detail
