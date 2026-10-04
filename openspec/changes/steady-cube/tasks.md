# Tasks

## 1. Words for the holding view

- [x] 1.1 `MoveWords.steadyParts` and `moveDescription(..., steady)` with the new strings in both languages (design §3)
- [x] 1.2 Test in `app`: each face move's direction checked against the cube model (a front or top sticker followed through the move); one Finnish and one English sentence via `Strings`; learn wording unchanged (`MoveWordsTest` still green)

## 2. Steady guide cube, mirror and reset

- [x] 2.1 `GuideCube(state, steady, mirror)`: steady keeps `DEFAULT_VIEW`; `SolveScreen` passes `steady = method == FAST` and the steady words to `MoveWordsText`; `FollowPanel` gets `steady` without mirror (design §1)
- [x] 2.2 Mirror cube (design §2) with `mirror_label`
- [x] 2.3 `CubeViewState.isAt`, reset button with its vector icon and `reset_view` (design §4)
- [x] 2.4 Compose tests in `app`: fast method stepping R → B keeps the view (`viewState.rotation` unchanged) and shows the mirror; learn method has no mirror; drag shows the reset button, tapping it hides the button. Verify: `./gradlew :app:testDebugUnitTest` green

## 3. Check and docs

- [x] 3.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` and the smoke test; screenshots of a fast-method back move (with mirror) and of the dragged state with the reset button, judged by eye
- [x] 3.2 Docs: `docs/architecture.md` Move guide section (steady view in fast, mirror, reset, two wordings); roadmap row 28 done; backlog idea "camera follow: notice when the cube is held differently, or keep helping in any orientation (after the user has tried camera follow)"
