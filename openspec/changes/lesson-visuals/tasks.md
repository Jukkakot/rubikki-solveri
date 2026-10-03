## 1. Cube module: goals and cases

- [ ] 1.1 `beginner/StageGoals.kt`: per stage the in-place stickers and the new stickers in the stage's hold; tests for the cross, middle layer and yellow cross masks. Verify: `./gradlew :cube:test`
- [ ] 1.2 Moved-pieces helper (stickers an algorithm changes on a solved cube); test for the trigger and the yellow cross. Verify: `./gradlew :cube:test`
- [ ] 1.3 `beginner/StageCases.kt`: cases for every stage (setup, highlight, solving moves); a test that every case's moves reach what it promises. Verify: `./gradlew :cube:test`

## 2. Goal picture component

- [ ] 2.1 `GoalCube` composable (masked colours, outlines, fixed or draggable view) in `ui/lessons`; screenshot of the middle layer goal in light and dark. Verify: screenshot looks right in both themes

## 3. Lesson pages

- [ ] 3.1 Lesson screen as a pager with dots and back/next bottom bar; goal page, practice page; basics as four picture pages. Verify: Compose test that next moves to page 2 and the dots follow
- [ ] 3.2 Cases page: 2×2 grid, tap opens the case large with play. Verify: Compose test that tapping a case shows its play button
- [ ] 3.3 Algorithm page: large demo, notation with the current move highlighted, current move in words, before/after thumbnails with moved pieces outlined. Verify: existing demo test still ends solved
- [ ] 3.4 Strings: case captions, one-line summaries and tips, shorter basics, in Finnish and English; remove `lesson_N_how`. Verify: `StringsTest` passes
- [ ] 3.5 Lessons list with goal thumbnails. Verify: screenshots of the list and of each page type fit the phone without scrolling

## 4. Goal in learn mode and practice

- [ ] 4.1 `SolveScreen` learn mode: goal card when a stage begins (Continue), thumbnail beside the stage name opening the goal large; `stageIntro` text removed from the stage card. Verify: Compose test that practice opens on the goal card and Continue shows the first step

## 5. Docs and roadmap

- [ ] 5.1 Update `docs/` pages that describe lessons and learn mode; mark `lesson-visuals` done in `openspec/context/roadmap.md`. Verify: `./gradlew testDebugUnitTest assembleDebug lintDebug` green before commit
