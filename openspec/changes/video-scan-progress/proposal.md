# Proposal

## Why

Phone test 2026-10-05 (screenshot from the browser app): the video scan reached "54/54 tarraa" only
after a long time, then stayed there with nothing happening and no word why. During the scan the
user could not tell which sides were done and which still had to be shown: the small 3D progress
cube shows only three sides and does not help (user: remove it, show everything on the real cube).

Cause of the stop (from the code): the scan finishes only when all 54 colours are confirmed one by
one and make a possible cube. When they do not and the problem has no particular stickers to point
at (ten reds and eight oranges, a twisted corner, a flipped edge, parity), nothing is marked and
the scan never finishes. The screenshot shows no orange at all and many reds: orange read as red in
dim, warm light is the likely case.

A cube has far fewer free choices than 54 stickers: two stickers of a corner fix the third, the
last pieces follow from the others. The scan should use that: find the most likely possible cube
from what it has read, and stop as soon as that cube is clear.

## What Changes

- **Most likely cube instead of 54 confirmations:** every reading is evidence per sticker (red and
  orange count as less sure of each other); the scan looks for the possible cube that fits the
  evidence best, piece by piece. It finishes as soon as that cube clearly beats every other possible
  cube, even with some stickers never seen. A misread sticker that cannot fit a real piece is
  corrected by the rest. Replaces the "54/54 and valid" rule and the dead end.
- **Everything on the camera picture:** the 3D progress cube is removed. On the real cube, every
  sticker on the sides turned towards the camera is marked: a solid dot in its colour when known,
  an empty ring when still needed; a side done gets a tick. A big arrow next to the real cube shows
  which way to turn it. Words are kept to a few.
- **Restart with a reason:** when the scan cannot make progress (too dark, colours that will not
  fit, no new stickers for a while), a panel with an icon and a two-to-three word tip ("Lisää
  valoa", …) and an "Aloita alusta" button. Restarting is quicker than fixing colours by hand.
- **Logging during the scan:** a snapshot every couple of seconds (what is known per side, the
  best cube and how sure, brightness, finder time) and events (side done, cube clear, restart and
  its reason, leaving the screen), so the next problem can be read from the log.

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: finishing by the most likely possible cube; marks and turn arrow on the camera
  picture instead of the progress cube; restart with a reason; logging during the scan.
- `diagnostics`: the video scan's log lines.

## Impact

Modules: `cube` (piece-based best-cube search with margins, sticker evidence in `VideoScan`, the
cube's projection into the picture, stall reasons) and `shared` (overlay, arrow, restart panel,
log lines; progress cube removed). `app` and `web` unchanged.
