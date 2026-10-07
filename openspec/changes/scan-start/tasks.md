# Tasks

## 1. Exposure control (cube)

- [ ] 1.1 Metering point = middle of all faces found in the frame (not the largest face); verify: JVM test in `ExposureControlTest` that two faces whose sizes swap from frame to frame do not move the point or restart the settle
- [ ] 1.2 Metering time limit (about 1 s from the first face): lock then unless a darkening step is waiting; washed-out darkening steps still run; verify: JVM tests that a cube moving every frame locks by the limit, and that washed-out frames still step darker before locking
- [ ] 1.3 Replay check: the 2026-10-07 log case (two faces, cube turned, `darker=0`) as a test of frames with jumping largest face reaching the lock within the limit; existing video-scan regression tests stay green

## 2. Adjusting status (shared)

- [ ] 2.1 While the exposure control is metering after a face was found, the video scan's status says "Säädän kameraa…" / "Adjusting the camera…"; verify: a Compose test or state test that the status changes from "show the cube" to "adjusting" once a face is found and away again on lock (no exact copy asserted)

## 3. Docs

- [ ] 3.1 Mark `scan-start` done in `openspec/context/roadmap.md`; touch `docs/` only if a page describes the exposure steps in detail
