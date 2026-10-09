# Proposal

## Why

The user (2026-10-09) finds colours easier than corners: "Näytä sininen puoli", not a corner of
three colours. The progress should stay honest: when every ball is ticked, the cube is scanned. The
state should also live: when the scan finds it was wrong about a side, the tick goes away.

## What Changes

- The corner row is replaced by a row of six coloured balls, one per side in its centre's colour.
- A ball gets a tick once all nine stickers of its side are part of the clear cube. All six are
  ticked only when the scan is complete. A tick goes away again when the scan changes its mind.
- The side worth showing next pulses: an unread side first, else the side with the most still to
  settle, held steady.
- The status line names that side all the time: "Näytä sininen puoli" / "Show the blue side". The
  "show the grey parts" and "N corners left" lines go away.
- The small cube (after 2 s without progress) turns the next side forward. Known stickers are in
  colour, and the ones still needed on that side blink.

## Capabilities

### Modified Capabilities

- `video-scan`:
  - Added: "Side row" and "Next side chosen".
  - Removed: "Corner row" and "Next corner chosen".
  - Modified: "One status line" and "Turn shown on a small cube", plus the corner-row mentions in
    "Recognised by agreement", "Progress on the real cube" and "Marks can be hidden".

## Impact

- `cube`:
  - Done sides come from the clear stickers, never all before `complete`.
  - `NextSide.choose` replaces `NextCorner`/`ScanCorners`.
  - The state, `ScanStateCodec` and the log get `sides=N/6 next=<colour>`.
- `shared`: the overlay row, the status line strings (fi, en, side colour names in their right form),
  and `TurnDemo` targeting a side.

## Implementation notes

- **Colour names (fi):** "Näytä valkoinen / keltainen / vihreä / sininen / punainen / oranssi
  puoli". English: "Show the white side" etc.
- **Ball:** about 22 dp, in the side colour with a thin dark outline.
  - Done: 25 % opacity, scaled to 0.8, with a white tick that has a dark outline.
  - Next: pulses (1.0 → 1.22 over 1.1 s); under reduced motion it stays enlarged instead.
- **Next side score:** its stickers that are not clear, plus 3 if its turn is open. A side never
  read wins first. A new pick needs 1.5× the current one's score. On a tie the current pick stays,
  otherwise the row order wins (white, red, green, yellow, orange, blue).
- **The last ball held:** if every side would be done but the cube is not complete, the side
  touching the open doubt stays undone. That is the side with the most open doubt, else the current
  pick.
