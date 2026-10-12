# Proposal

## Why

The learning mode shows every move: the user follows, but does not have to think. To learn to solve the cube
themselves the user needs to try each stage on their own, with help only when stuck (backlog `solve-challenge`,
user 2026-10-06). The app already knows each stage of the beginner method (`BeginnerSolver`, `Checks.stageDone`),
can show a cube in 3D, and can read the real cube with the video scan, so the pieces exist.

## What Changes

- **A third way on the start screen: "Kokeile itse" / "Try it yourself"**, beside the shortest solution and
  learning step by step.
- **Per stage:** the stage's goal shown as a turnable 3D cube: the user's cube as it will be when the stage is done,
  only the stage's pieces in colour and the rest grey; the stage's name and a one-line aim ("Tee valkoinen risti").
- **Hint:** a button shows the next move of that stage from where the app knows the cube is, with the usual move
  guide; each hint is counted.
- **Check:** "Valmis" opens the video scan; the scanned cube is compared with the stage's goal
  (`Checks.stageDone`). Done → praise and the next stage, worked out from the scanned cube. Not yet → the goal again,
  the pieces still out of place marked, and the hint now works from the scanned cube.
- **End:** solved → the usual finish (timer and history as for learning), with the number of hints.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `beginner-solver`: a new requirement *Try it yourself* beside *Learn mode in the solution screen*.
- `progress`: a challenge solve is saved with its number of hints (only if the user wants it, question 4).

## Impact

`shared` (start screen option, a challenge screen reusing the move guide, 3D cube and the video scan), `cube`
(the stage's goal cube: the beginner solution applied up to the stage's end; pieces of a stage), texts in Finnish
and English. `app` and `web` unchanged beyond the shared screens.
