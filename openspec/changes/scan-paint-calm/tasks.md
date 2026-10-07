# Tasks

## 1. Marks (shared)

- [ ] 1.1 `ScanPaint`: tiles only for needed stickers (projection sides and found faces), known ones dropped; a tick position per confirmed side beside its outline; verify: unit tests that a known sticker has no tile, a needed one has, and a confirmed side has an outline and a tick
- [ ] 1.2 Paint layer: needed stickers as a filled grey veil (no dashed line, no read colours), the outline with a small tick at the side's centre, the found face's dim outline kept; verify: one smoke test that the video scan screen draws with a state holding known, needed and confirmed stickers

## 2. Fade while moving (shared)

- [ ] 2.1 A motion fade from the cube's speed in the picture (centre of the largest face or the projection, in side widths per second): fades out above about a third of a side per second, back after about 0.3 s below it; multiplies the existing age fade; verify: unit test on the pure fade function (still → 1, fast → 0, back after the rest time)

## 3. Docs

- [ ] 3.1 `docs/architecture.md` (video scan screen paragraph): one line for veil, tick and motion fade; roadmap row for `scan-paint-calm` added as done; list for the user what to try on the phone (still cube, quick turn, a finished side)
