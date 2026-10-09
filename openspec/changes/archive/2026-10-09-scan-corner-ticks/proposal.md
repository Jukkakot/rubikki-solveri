# Proposal

## Why

Phone test 2026-10-09 (user): every corner in the corner row was ticked, but the scan had not
finished. A corner ticks once its three corner stickers are known. The finish, though, also needs
the edges, the faces' turns settled and the whole cube clear. The user wants the ticks to be
honest: all corners ticked means the cube is done.

## What Changes

- A corner ticks only when everything it stands for is sure. Its three corner stickers and the
  stickers of its three edges must be part of the clear cube, not known from the stickers' own votes
  alone. Every edge touches two corners, so all corners ticked covers every sticker.
- The row never shows all eight ticked before the scan is complete. If everything else is ticked
  while the cube is not yet complete, the corner touching the open doubt stays unticked and pulses.
- The status line counts these honest corners ("Vielä N kulmaa"). The special case for zero
  corners left goes away.

## Capabilities

### Modified Capabilities

- `video-scan`: "Corner row" (what a read corner means, and all ticked means complete).

## Impact

- `cube`: `readCorners` from clear stickers (the corner and its three edges) and the complete
  flag, with tests.
- `shared`: the status line's count. Nothing else changes visually.

## Implementation notes

- The scan state carries `clear` (the stickers that are part of the clear cube, from `clearAt`,
  including the red/orange hold) so `ScanCorners.read` needs nothing else; the browser state text
  carries it too.
- The corner held back when only the complete flag is missing is the one with the highest
  `NextCorner.score` (unclear stickers and open turns on its three sides); on a tie the corner
  already pulsing, else the first in the row, so the pulse does not jump. The next-corner choice
  then lands on it, as it is the only unread one.
- The status line shows "Vielä N kulmaa" whenever every side is read and the scan is not complete;
  the count is never zero there, so the grey-parts fallback for zero corners is gone.
