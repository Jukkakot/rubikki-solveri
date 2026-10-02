# Proposal

## Why

product.md: showing the moves is a core feature — every move must be unmistakable for a beginner.
The stepper from `fast-solve` animates the move and names it in words, but a beginner still has
to work out which layer and which way, especially for the back, left and bottom.

## What Changes

- The moving layer is highlighted: the rest of the cube is dimmed.
- A big curved arrow on the turning face shows the direction (a quarter or a half arc).
- The 3D view follows the move while keeping the user's hold (white on top, green in front): it
  swings round to show the left, back or bottom when that side turns, and back again.
- Each new move plays its demo once by itself; "show" replays it.
- Haptics: a confirming vibration on "done", a light tick at the end of each demo.
- Optional notation: a setting "show move notation" adds the standard notation (e.g. R') under
  the words; off by default (product.md).

## Capabilities

### New Capabilities
- `move-guide`: how a single move is presented so a beginner cannot get it wrong.

### Modified Capabilities
(none — the stepper's behaviour in `fast-solve` stays; it now presents moves through the guide)

## Impact

- `app`: `Cube3D` gains layer highlight and an arrow overlay; `MoveGuide` composable used by the
  solution screen (and later the beginner solver and lessons); settings gets the notation switch.
