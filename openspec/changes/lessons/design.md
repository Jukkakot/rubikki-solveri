# Design

## Context

`beginner-solver` gives stages, steps and notes; the solution screen gives the guided stepper.
Lessons add calm explanation, isolated algorithm demos and stage practice.

## Goals / Non-Goals

**Goals:** a user who reads a lesson and practises a few positions can do the stage alone.

**Non-Goals:** progress tracking and statistics (`progress`), video, quizzes.

## Decisions

- **Content in string resources** (Finnish and English), one set per lesson: summary, what,
  how, tip. Lesson structure (stage, algorithms with names) in a small Kotlin table.
- **Algorithm demo**: start = solved cube with the algorithm's inverse applied, so playing the
  algorithm visibly solves it; reuses `StepperState` + `GuideCube` (highlight, arrows); "play"
  plays all moves in order, pauses 1.2 s and snaps back. The move list shows each move in words.
- **Practice positions** (`Practice.position(stage, random)` in `cube`): random 25-move scramble,
  beginner-solve it, apply the steps of the earlier stages; retry while the stage is already done.
  Deterministic for a seed (tests).
- **Practice mode of the solution screen**: `SolveScreen(practiceStage = s)` hides the method
  choice and plans only the steps of stage s; the finish text is "Stage done!" with "New
  position". Route `PracticeRoute(stage, seed)`; "new position" navigates to seed + 1, replacing
  the current entry.
- **Basics practice** opens the free cube; the basics lesson's demo plays R U R' U'.

## Risks / Trade-offs

- [Long texts on a phone] → short paragraphs, headings, scrolling; checked with screenshots.

## Decisions made while building

- `Practice.exercise` returns the position together with the steps from the same solve: solving
  the practice position again could hold the cube differently and give other stages.
- `AlgorithmCard` takes its animator from a parameter, so the demo is testable with the test clock.
