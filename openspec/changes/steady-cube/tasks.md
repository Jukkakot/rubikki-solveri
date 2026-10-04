# Tasks

## 1. Fewer whole-cube turns (cube)

- [ ] 1.1 Before changing code, add a measuring test helper that solves 200 seeded random cubes with `BeginnerSolver` and counts quarter whole-cube turns outside the method's fixed turns; record the current average as a constant
- [ ] 1.2 `MacroSearch.find` with a cost (`ROTATION_COST = 8` per quarter rotation + face moves), exploring all depths up to `maxDepth`, lowest cost wins (design §5); used by every stage that searches with `Y`
- [ ] 1.3 Tests: average ≤ 60 % of the recorded constant; every solution solves its cube; a case where a bottom-layer placement is chosen over a `y`. Verify: `./gradlew :cube:jvmTest` green; selftest beginner time still under 100 ms locally

## 2. Steady view, mirror and reset (shared)

- [ ] 2.1 Remove `CubeScene.guideView`; `GuideCube` stays in `DEFAULT_VIEW` and never animates by itself
- [ ] 2.2 Mirror cube for L/B/D (design §2) with label strings `mirror_left` / `mirror_back` / `mirror_bottom` in both languages
- [ ] 2.3 Reset button (design §4) with `CubeViewState.isAt`; strings `reset_view` in both languages
- [ ] 2.4 Compose tests in `app`: stepping through R, B shows the mirror only for B; drag then reset hides the button again. Verify: `./gradlew :app:testDebugUnitTest` green

## 3. Words and the whole-cube turn step

- [ ] 3.1 `MoveWords.direction` and the new move strings in both languages (design §3); remove the old `move_cw` / `move_ccw` / `from_*` strings
- [ ] 3.2 Cube-model test that each direction matches where a front or top sticker goes; update `MoveWordsTest` / `BeginnerTextsTest` expectations
- [ ] 3.3 Centre colour dots for whole-cube turns (design §6). Verify: tests green; `ComposeStringsTest` green

## 4. Check and docs

- [ ] 4.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` and the smoke test; one screenshot each of a back move (with mirror) and a whole-cube turn, judged by eye
- [ ] 4.2 Docs: `docs/architecture.md` Move guide section (steady view, mirror, reset, words); roadmap item `steady-cube` done; product.md "Fewer whole-cube turns" idea updated
