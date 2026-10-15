# Proposal

## Why

Phone check of `auto-panel` (user 2026-10-15, screenshot): the tap-the-cube confirm and its hint on the first
move are needless now that a wide "Seuraava" button goes one move. Remove both.

## What Changes

- A tap on the guide cube no longer confirms the move; dragging still turns the view.
- The hint "Napauta kuutiota = tein sen · raahaa = käännä" is gone.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `handsfree-guide`: the "Tap the cube to confirm" requirement is removed.

## Impact

Module `shared` (solve screen, one string pair); `app` handsfree screen test. No change to `cube`.
