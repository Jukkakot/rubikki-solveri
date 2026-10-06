# Proposal

## Why

The guide only leads to the solved cube. The user wants to choose where the real cube ends up: a
known pattern, a surprise, a lesson stage, or any cube they paint (backlog `solve-to-target`, user
2026-10-05; ways 1–4 chosen 2026-10-06).

## What Changes

- A **target** for the solution, solved by default. The solution screen shows the current target
  and lets the user change it; the guide then leads from the real cube to exactly that target.
- Four ways to choose it, on one target picker screen:
  1. **Pattern gallery**: about a dozen known patterns (checkerboard, cube in a cube, superflip …)
     as pictures; a tap shows it as a large turnable cube with a choose button.
  2. **Surprise me**: a random pattern from the gallery.
  3. **Lesson stage**: "stop when stage N is done" (white cross … whole cube), led with the learn
     method's stage cards up to that stage.
  4. **Paint**: the hand-input screen paints the target; it must be a possible cube.
- A home tile "Kuviot" opens the picker for a cube that is solved now (no scan needed); the solution
  screen's target button covers a scanned or painted starting cube.
- The finish says the target is reached (celebration as today).

Modules: `cube` (solving to a target, the pattern list), `shared` (picker screen, target on the
solution screen, home tile, hand input in target mode, strings fi/en). `app`/`web` unchanged.

## Capabilities

### New Capabilities
- `solve-target`: choosing a target and leading the real cube to it.

### Modified Capabilities
- `app-shell`: the home screen gets a pattern entry among the secondary entries.

## Impact

- `cube/solve/TwoPhaseSolver` (solve S → T via the cubie group), new `cube/Patterns`.
- `ui/solve/SolveScreen` (target button and plan per target), new `ui/target/TargetScreen`,
  `ui/manual/ManualInputScreen` (target mode), `ui/home/HomeScreen` + `RubikkiNavHost` routes.
