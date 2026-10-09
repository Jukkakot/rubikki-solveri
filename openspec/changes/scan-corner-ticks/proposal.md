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
