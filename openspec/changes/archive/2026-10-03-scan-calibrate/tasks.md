# Tasks

## 1. Reading

- [x] 1.1 `ColorClassifier`: `references`, `ranked`, `live(refs)`, `classify(fixed)`; verify tests "references from the cube" and "fixed stickers keep their colour"
- [x] 1.2 `ScanSession`: references from accepted centres and corrections, lenient centre check, `cycle` / `reviewCorrections`, corrections in the outcome and dropped on redo; verify tests "Warm red" (centres teach the reading), "Tap to fix", "redo forgets corrections"

## 2. Screen and camera

- [x] 2.1 Review tiles tappable (centre fixed), strings (fi, en), corrections in the `scan.face` log; verify Compose test "Tap to fix"
- [x] 2.2 `CameraPreview(lockExposure)` with AE/AWB lock via Camera2 interop, locked after the first face, `scan.lock` log; phone check listed for the user
- [x] 2.3 Roadmap entry; operations doc mentions `fixed=` and `scan.lock`
