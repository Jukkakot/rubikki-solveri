## 1. Home screen

- [ ] 1.1 Rewrite `ui/home/HomeScreen.kt`: hero (app name + spinning, draggable `Cube3D`), filled pill "scan" button with camera icon, 2×2 tonal tile grid with icons, optional solve summary line, version at the bottom, settings as `RoundIconButton`; landscape puts cube and actions side by side; `spin` flag for tests. Verify: builds, screen fits portrait and landscape without scrolling in the screenshot test
- [ ] 1.2 Add short tile labels and the summary string in Finnish and English; remove the "coming later" path and the unused `coming_soon` string. Verify: `StringsTest` passes
- [ ] 1.3 Wire home in `RubikkiNavHost.kt`: entries with icons, summary from `progress.timedSolves` (best time and count; null when empty). Verify: `ShellTest` home navigation tests pass (scan, manual, learn, timer, free cube)
- [ ] 1.4 Tests: one Compose test that the summary line shows with timed solves and is absent without; refresh the home screenshot (light and dark, portrait and landscape). Verify: `./gradlew testDebugUnitTest` green, screenshots look right

## 2. Docs and roadmap

- [ ] 2.1 Update `docs/architecture.md` home row if the entries changed, mark `home-design` done in `openspec/context/roadmap.md`. Verify: `./gradlew assembleDebug` and lint pass before commit
