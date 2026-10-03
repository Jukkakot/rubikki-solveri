# Proposal

## Why

A half turn now plays as one smooth 180° sweep, slightly longer than a quarter turn. On the phone
it is hard to see whether the layer went a quarter or a half, so the user cannot tell how far to
turn the real cube (user priority 2026-10-03).

## What Changes

- A half turn animates as two quarter steps with a short pause between them, so the eye counts
  "one, two". A quarter turn stays one step.
- This holds everywhere the 3D cube plays moves (solve steps, demos, back, lessons, free cube), so
  the same move always looks the same.
- During a demo of a half turn the phone gives a light tick after each quarter step (two ticks),
  backing the count by touch.
- Decisions taken here (no design.md):
  - The split is done in the cube's move player, not per screen: one place, every screen gets it.
  - Pause between the steps about 0.25 s; each step uses the quarter-turn timing (about 0.3 s).
  - With the phone's animations off, moves still apply at once (no steps, no pause).

## Capabilities

### New Capabilities

### Modified Capabilities
- `cube-view`: Move animation — a half turn plays as two quarter steps with a pause.
- `move-guide`: Haptics — a demoed half turn ticks after each quarter step.

## Impact

App module only (3D cube move player, solve-screen demo haptics); the cube module is unchanged.
