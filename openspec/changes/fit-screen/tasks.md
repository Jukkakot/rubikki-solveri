# Tasks

## 1. Helper and solution screen

- [x] 1.1 `ui/common/FitColumn`: fixed column with a `weight(1f)` slot for the big element (as large as fits, centred, never larger than full width), scrolling fallback under 200 dp
- [x] 1.2 Stepper on `FitColumn`: step count on the progress row, hold line in `bodySmall`, 8 dp spacing, method choice without extra padding; `GuideCube` sized by its slot (mirror and reset scale with it)
- [x] 1.3 Camera follow: the camera box (3:4) in the slot
- [x] 1.4 Compose tests in `app` at about 411 × 560 dp: "Tein sen" and the move text displayed without scrolling, and the cube's size is the same after a step; at 411 × 891 dp the cube is full width
- [ ] 1.5 Learn method at 411 × 560 dp: the stage card leaves ~106 dp for the cube (under the 200 dp floor), so the screen scrolls; open question to the user

## 2. Other screens

- [x] 2.1 Free cube, scan (camera view in the slot), timer (tap area in the slot), lessons (picture in the slot): `FitColumn`, trimmed spacing
- [x] 2.2 One smoke test per screen at 411 × 560 dp: its main action is displayed without scrolling

## 3. Check and docs

- [x] 3.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` and the smoke test; screenshots of the solution and scan screens at 411 × 560 dp, judged by eye
- [x] 3.2 `docs/architecture.md`: one line on `FitColumn` (where, the 200 dp floor)
