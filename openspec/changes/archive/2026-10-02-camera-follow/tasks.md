# Tasks

## 1. Follow logic (cube module)

- [x] 1.1 `FrontArrow.of(move)`; verify tests "Right turn", "Top turn", all face moves and none for B
- [x] 1.2 `LiveCalibration`; verify test "Warm light"
- [x] 1.3 `FollowTracker` (centre guard, match with tolerance, stable done, wrong move with fix, not-visible moves); verify tests "Move done", "Wrong direction", "Back turn", one misread cell still matches

## 2. App

- [x] 2.1 `CameraPermissionGate` shared by scan and follow; verify the scan permission test still passes
- [x] 2.2 Camera mode in the solution screen (switch, preview with grid, dots, arrow, corner guide cube, messages, auto-advance with haptics); verify Compose tests "Switch to camera mode" (permission screen in tests) and a frames-driven test that advances and reports a wrong move
- [x] 2.3 Docs (architecture: follow mode) and roadmap item 6 done
