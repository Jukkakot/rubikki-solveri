## Why

The lessons are walls of text: a stage lesson is three long paragraphs and an algorithm card that
falls below the screen, and nothing shows what the cube should look like when the stage is done.
The user wants pictures to replace most of the text, and to see the goal of every stage.

## What Changes

- **Goal picture per stage:** the solved cube held as the method holds it, with the pieces that
  are not yet in place grey and the pieces this stage adds outlined. Turnable by dragging.
- **Lessons become swipe pages** that each fit one screen: Goal → Cases → one page per algorithm →
  Practice, with page dots and next/back buttons. Basics becomes four picture pages (centres,
  edges and corners, a move, the holding position).
- **Case pictures ("which situation do you have?")** replace most of the "how" text: each stage's
  typical situations as small cubes with the relevant piece highlighted, a one-line caption and
  what to do (which algorithm, how many times). Tapping a case plays it.
- **Algorithm pages show the effect:** before and after pictures with the pieces the algorithm
  moves outlined; while the demo plays, the current move is highlighted in the notation and said in
  words (the long list of moves in words goes away).
- **The lessons list shows the path:** a small goal picture beside every stage.
- **Goal in practice and guided solve:** when a stage begins, a card shows its goal picture
  ("Next: …"); during the stage a small goal picture sits beside the stage name and replaces the
  stage's intro text.
- The "how" texts are removed; summaries and tips are shortened to one line each.

Modules: cube (goal masks and case positions, pure data with tests) and app (screens).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `lessons`: lesson list with goal pictures; lesson content as swipe pages with goal, case and
  algorithm pictures; algorithm demo shows the moved pieces and the current move; practice starts
  with the goal card. New requirements: stage goal picture, case pictures.
- `beginner-solver`: learn mode shows the goal picture instead of the stage's intro text, and a
  goal card when a stage begins.

## Impact

- cube module: new `beginner/StageGoals.kt` (which stickers are in place after each stage, which
  are new) and `beginner/StageCases.kt` (case positions and the moves that solve them), with tests.
- app: `ui/lessons/` rewritten (pager, goal, cases, algorithm pages), a shared goal-cube
  composable, `SolveScreen` learn mode and the practice screen, strings in Finnish and English.
- Removes the `lesson_N_how` strings; `stage_N_intro` stays only where still used.
