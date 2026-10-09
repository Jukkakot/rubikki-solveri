# Proposal

## Why

The user asked how the scan could say more clearly what to show next. Today, once every side has
been read, the line only says "Käännä kuutiota" and the user waits "hoping it finishes soon"
(2026-10-09 logs: every side read in 2–4 s, then 1–4 s more of waiting). The scan knows exactly
which stickers are still unknown and which faces' turns are in doubt. The user reviewed mockups
("Skannausvinkit", "Kääntönuolet", "Kulmarivi") and chose: no arrows. A row of the cube's eight
corners should be shown all the time (option E1). The six-side ring goes, and the small cube stays.

## What Changes

- **Corner row.** Eight small corner pictures, each in the three centre colours that meet there,
  lie above the status line during the whole scan. A corner whose three stickers are known dims
  and gets a tick in place. The corner worth showing next pulses.
- **Next corner chosen.** Among the unread corners, the scan picks the one whose view would
  settle the most (unknown stickers, stickers in doubt, open turns), held steady.
- **The six-side progress ring is removed.**
- **Status line.** Once every side has been read, the line says how many corners are left
  ("Vielä 3 kulmaa") instead of "Käännä kuutiota".
- **The small cube** (after about 2 s without progress) turns the pulsing corner forward, with
  known stickers in colour and the needed ones blinking. Before every side is read it works as
  today.
- No arrows. "Hitaammin/Suoremmin" stays out (backlog).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`:
  - Added: "Corner row", "Next corner chosen".
  - Removed: "Progress ring".
  - Modified: "One status line", "Turn shown on a small cube", plus the ring mentions in
    "Recognised by agreement", "Progress on the real cube" and "Marks can be hidden".

## Impact

- `cube`: the corner state and the next-corner choice are pure functions of the scan state
  (known stickers, doubts, open turns, how the cube is held), with tests. They are added to the
  state and to `ScanStateCodec` for the browser worker.
- `shared`: `ScanOverlayBar` loses the ring and gains the corner row. `VideoScanScreen` gets the
  status line, `TurnDemo` the target corner, and strings change in fi and en. The screenshot tests of
  the scan screens change.
