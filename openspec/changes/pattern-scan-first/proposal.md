# Proposal

## Why

The home screen's "Kuviot" assumes the cube in the user's hands is solved. Usually it is not, and
then the user has to go back, scan, and change the target on the solution screen by hand: three
detours for the main case (user, 2026-10-07).

## What Changes

- On a target chosen from home (pattern, surprise, stage or painted), the app asks where to start:
  **"Skannaa kuutio"** (primary) or **"Kuutio on jo ratkaistu"**.
- "Skannaa kuutio" opens the usual scan (video scan by default, the switch to the guided scan
  works too) with the target carried along. After the scan the solution screen opens from the
  scanned cube with the chosen target; the colour check and hand input in between keep it as well.
- "Kuutio on jo ratkaistu" works as today: the solution screen from the solved cube.
- The scan screens themselves do not change; at most they show the target's name.

Decisions (light lane, no design.md):
- Pattern first, then the start question: picking the pattern is the fun part and the reason the
  user came; the question comes once, right before leaving the picker.
- The question is a small dialog on the target picker, not a new screen.
- The target travels as a route argument through the scan, check and hand-input routes (the same
  encoded form the solution route already uses); nothing new in the cube module.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solve-target`: "Patterns from a solved cube" becomes "Patterns from home": choose to scan first
  or start from a solved cube.

## Impact

- Modules: `shared` only (navigation, target picker dialog, scan/check routes carry the target,
  texts fi/en). No change in `cube` or `app`.
- Docs: the solve-target part of `docs/` pages that describe the home → patterns flow; roadmap row.
