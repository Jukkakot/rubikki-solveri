# Proposal

## Why

Phone testing (2026-10-05): the guide works, but the user still waits for the animation to see
whether a move is one or two quarter turns, and taps "Näytä" to see a move again. A number on the
arrow and a repeating demo make solving quicker.

## What Changes

- **Count on the arrow:** the arrow carries the number of quarter turns (1 or 2) in a small badge,
  readable at a glance. Placement and whether "1" is shown are decided with the user (mockups).
- **Demo repeats:** after the demo ends, the cube waits about 5 s in the state after the move, then
  jumps back to before it and plays it again, until the user moves on. "Näytä" still plays at once.

## Capabilities

### New Capabilities

### Modified Capabilities
- `move-guide`: the count on the arrow; the demo repeats.

## Impact

`shared` only: `ui/cube3d/Cube3D.kt` (badge), `ui/guide/StepperState.kt` (repeat), `MoveGuideTest`.
