# Proposal

## Why

The step-by-step solution teaches while solving one cube. To really learn, the user needs the
ideas explained calmly, the algorithms shown on their own, and practice on positions where only
the stage being learned is left to do.

## What Changes

- Lessons screen from home: "Basics" (pieces, centres, notation, holding) and one lesson per
  beginner stage (7).
- A lesson: what the stage achieves, how to recognise the cases and what to do, a tip, and the
  stage's algorithms, each with a name, its notation, its moves in words and a 3D demo that plays it.
- Practice: a fresh position where the earlier stages are already solved and this stage is not;
  the step-by-step guide runs only this stage, then offers a new position.
- Basics practice opens the free cube.

## Capabilities

### New Capabilities
- `lessons`: teaching the method stage by stage, with demos and practice positions.

### Modified Capabilities
- `app-shell`: the home screen gains the lessons entry.

## Impact

- `cube`: practice position generator (pure).
- `app`: `LessonsScreen`, `LessonScreen`, algorithm demo, practice mode of the solution screen,
  routes, texts.
