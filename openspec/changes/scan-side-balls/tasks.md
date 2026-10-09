# Tasks

## 1. Sides in cube

- [ ] 1.1 Done sides: all nine stickers clear; never all six before `complete`; the side with the open doubt held back (proposal notes)
- [ ] 1.2 `NextSide.choose(state, previous)`: unread side first, else score, steadiness (proposal notes); remove `NextCorner`/`ScanCorners` and the corner fields
- [ ] 1.3 State, `ScanStateCodec` and log snapshots carry done sides and next side (`sides=N/6 next=<colour>`)
- [ ] 1.4 Tests: done side needs all nine clear; a tick is taken back when a recheck changes the side; on the committed recordings and fixtures all six never before `complete` and all at finish; unread side chosen first; choice steady

## 2. Screen

- [ ] 2.1 Overlay: six coloured balls replace the corner row (done dimmed with a tick, next pulsing)
- [ ] 2.2 Status line: "Näytä <väri> puoli" / "Show the <colour> side" for the next side; "show the cube" when none found; ready at the end (fi, en)
- [ ] 2.3 `TurnDemo`: turns the next side forward, known stickers coloured, needed ones on that side blinking
- [ ] 2.4 Tests: six balls from the start, a done ball dimmed, the next one marked, status line names the next side's colour

## 3. Check, docs

- [ ] 3.1 Build both apps, run all unit tests (and the browser smoke test if it can run here)
- [ ] 3.2 `docs/architecture.md` overlay map; roadmap row 76 `scan-side-balls` done; archive
