# Tasks

## 1. Solution screen

- [ ] 1.1 Stepper: fixed column; the guide area gets the leftover height (`weight(1f)`), `GuideCube` draws the largest 1.1 box that fits, centred; under 160 dp leftover the scrolling layout as today
- [ ] 1.2 Camera follow: the camera box shrinks the same way (keeps 3:4, centred)
- [ ] 1.3 Compose test in `app` at a short window (about 411 × 560 dp): "Tein sen" and the move text are displayed without scrolling; one at a tall window: cube width unchanged

## 2. Free cube screen

- [ ] 2.1 Same layout: the cube takes the leftover height, the turn buttons and "Ratkaise" stay visible; scroll fallback under 160 dp
- [ ] 2.2 One smoke test: "Ratkaise" displayed without scrolling at the short window

## 3. Check and docs

- [ ] 3.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` and the smoke test; one screenshot of the solve screen at the short window, judged by eye
- [ ] 3.2 `docs/architecture.md`: one line on the fit-the-screen layout (where it lives, the 160 dp fallback)
