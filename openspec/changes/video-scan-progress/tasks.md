# Tasks

## 1. Best possible cube (cube)

- [ ] 1.1 Sticker evidence: per-colour vote counts per net sticker, cost = −log smoothed share, red/orange lending to each other. Verify: unit tests (clear votes → low cost; red votes leave orange cheaper than blue).
- [ ] 1.2 Best cube by pieces: corner and edge assignments (Hungarian, twist/flip inside the cost), cheapest fix for twist, flip and parity; margins per slot. Verify: unit tests (full right evidence → that cube; one orange read as red → right cube; two corner stickers known → third known; empty evidence → not clear).
- [ ] 1.3 Simulation harness (design 4): random cubes and scans with realistic misreads; wrong finishes and stickers read at finish per threshold; pick T; write `findings.md`. Verify: a JVM test runs a smaller simulation with the chosen T and asserts zero wrong finishes.
- [ ] 1.4 `VideoScan` uses it: `known` per sticker, `clear`/`finished` from margins with the support guard, outcome marks structure-decided stickers uncertain. Verify: replay tests on both videos (true cube, never a wrong clear cube, frames to clear printed and fewer than today).
- [ ] 1.5 Stall reasons (dark, no cube, no progress / nothing fits) and `reset()`. Verify: unit tests with synthetic frames and times.
- [ ] 1.6 Projection of every sticker into the picture from the orientation and the main face; sides facing the camera. Verify: unit tests against synthetic projections (known rotation → sticker points within a fraction of a step).

## 2. Screen (shared)

- [ ] 2.1 Overlay: solid / empty marks on the projected stickers of sides facing the camera, found faces as fallback, tick on done sides; progress cube removed; done-sides row of six colours under the picture. Verify: smoke test renders with a partly known state.
- [ ] 2.2 Turn arrow on the picture beside the cube; few-word status. Verify: covered by the smoke test.
- [ ] 2.3 Restart panel (icon, short tip, restart button, "Korjaa värit") per stall reason; small lamp notice for dim light; the bottom button renamed to "Korjaa värit" (fi + en). Verify: Compose test that a stall shows the panel and restart clears the progress.
- [ ] 2.4 Log lines: snapshot every 2 s, side / clear / stall / restart / leave. Verify: unit test of the snapshot line's content from a state.

## 3. Docs

- [ ] 3.1 `docs/architecture.md` video scan section where the map changes; roadmap row 42 `video-scan-progress` done.
