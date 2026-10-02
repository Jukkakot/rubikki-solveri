# Proposal

## Why

The first real payoff: after entering a cube, the user gets the shortest practical solution and
can follow it move by move on the 3D cube.

## What Changes

- Vendor the two-phase solver min2phase (MIT licence option, licence text kept) into `cube`, with
  a Kotlin wrapper: solve any valid cube in at most 21 moves (about 19 on average), report invalid
  cubes, random-state scrambles.
- Solution screen: computes the solution in the background, then steps through it one move at a
  time: the 3D cube shows the user's current cube; "show" plays the move; "done" plays it and goes
  to the next; "back" undoes the last; progress "move 5/19" and a bar; a finish state.
- Each move is described in words ("Turn the right side clockwise, as seen from the right");
  notation stays hidden for now (product.md).
- Manual input and the free cube lead to the solution screen.
- The solver warms up at app start on a background thread.

## Capabilities

### New Capabilities
- `fast-solve`: finding a short solution for a valid cube and stepping through it.

### Modified Capabilities
- `manual-input`: a valid cube now opens its solution.

## Impact

- `cube`: `cs.min2phase` (vendored Java, unmodified), `solve/TwoPhaseSolver`.
- `app`: `SolveScreen`, move descriptions, routes, free cube "solve" button, solver warm-up and
  log events `solve.done`/`solve.failed`.
