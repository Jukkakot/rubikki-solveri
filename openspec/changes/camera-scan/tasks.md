# Tasks

## 1. Scan logic (cube module)

- [x] 1.1 `Rgb`, Lab conversion and distance; verify tests for known Lab values and the lightness weighting
- [x] 1.2 Image sampling (`RgbaFrame`, grid geometry with rotation, median of the cell middle); verify tests that a synthetic upright face sampled at rotations 0/90/180/270 reads the same nine colours
- [x] 1.3 Live reading with the default palette; verify each default reference reads as its colour and noise does not change it
- [x] 1.4 Balanced classification (Hungarian, refinement, confidence); verify tests "Different lighting" (warm tint, noise, 200 scrambles) and "Doubtful sticker"
- [x] 1.5 `ScanSession` (stability, wrong face, capture, redo, result); verify tests "First face", "Wrong face", "Held still", "Redo", "Confident scan" and "Unsure scan" routing decision

## 2. App

- [x] 2.1 CameraX dependencies, manifest permission/feature; `ScanScreen` with preview, overlay, hints, status, capture/redo/torch, permission flow, haptics, logging; verify Compose test "Permission denied" (no camera in tests) and a session-driven overlay test
- [x] 2.2 Routes: scan entry on home, result to solve or to manual input with marks and note; verify tests "Open the scan" and "Opened from a scan"
- [x] 2.3 Docs (architecture: scan pipeline; operations: tuning from the log) and roadmap item 4 done
