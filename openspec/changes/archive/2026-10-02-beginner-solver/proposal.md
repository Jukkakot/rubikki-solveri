# Proposal

## Why

The shortest solution solves the cube but teaches nothing. product.md: a human method of our own,
stage by stage, so the user learns real cube knowledge and can eventually solve without the app.

## What Changes

- A layer-by-layer beginner solver in `cube`: white cross, white corners, turn the cube over,
  middle layer, yellow cross, yellow corners turned, yellow corners placed, yellow edges placed.
  Every step says what it does and why, in plain words, with the moves to make. Algorithms are the
  classic beginner ones; the same "R' D' R D" trigger is used for the white corners and the yellow
  corners so it is learned once.
- Whole-cube turns ("turn the whole cube so that red faces you") are part of the steps and are
  shown and described like any move.
- The solution screen gets a choice: shortest solution or learn step by step. The step-by-step
  view shows the stage (1/7 …), what the stage achieves, the current step's explanation, and the
  usual move guide.
- Camera follow accepts whole-cube turns.

## Capabilities

### New Capabilities
- `beginner-solver`: solving any valid cube with a human method, explained step by step.

### Modified Capabilities
(none)

## Impact

- `cube`: `beginner` package (macro search, stages, explanations as data), tests over many cubes.
- `app`: method choice and stage view in `SolveScreen`, texts for stages, steps and whole-cube
  turns; `FollowTracker` handles rotations.
