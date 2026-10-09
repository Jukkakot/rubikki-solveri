# Tasks

## 1. Corners in cube

- [ ] 1.1 Corner positions by centre colours (row order, design 1); `readCorners(state)` (three stickers known; all at finish)
- [ ] 1.2 `NextCorner.choose(state, previous)`: score (design 3), steadiness, none when all read
- [ ] 1.3 Tests: read corners from a partial state; the missing white–red–blue corner is chosen; the choice holds under small score changes; on the committed recordings the chosen corner always has an unknown or doubtful sticker in its view
- [ ] 1.4 State carries read corners and next corner (also through `ScanStateCodec`); `corners=`/`next=` in log snapshots

## 2. Screen

- [ ] 2.1 `ScanOverlayBar`: remove the ring; corner row above the status line (design 2), read corners dimmed with a tick, next one pulsing
- [ ] 2.2 Status line: "Vielä N kulmaa" once every side is read (fi, en); the look-alike "turn the cube" text removed
- [ ] 2.3 `TurnDemo`: after every side is read, known stickers in colour, unknown grey, needed ones blinking, turning the next corner forward; unchanged before
- [ ] 2.4 Tests: row shows eight corners from the start; a read corner is dimmed; the next corner is marked; demo gets the needed stickers; update screenshot tests of the scan screens

## 3. Check, docs

- [ ] 3.1 Build both apps, run all unit tests
- [ ] 3.2 `docs/architecture.md` scan overlay map; roadmap row 74 `scan-next-view` text and done
