# Tasks

## 1. Reading in different light (cube)

- [ ] 1.1 Measuring harness (design 1): per video, distances of true red and orange readings to both references, raw; table into `findings.md`. Verify: runs on all six fixtures, numbers printed.
- [ ] 1.2 Light correction per frame (design 2). Verify: unit test (a frame tinted warm reads like the untinted one); harness numbers before/after in `findings.md`.
- [ ] 1.3 Glare left out of a sticker's colour (design 4); fixtures regenerated. Verify: unit test (a blob with a bright washed-out patch keeps its colour); all video tests still pass.
- [ ] 1.4 Soft votes through groups and `StickerEvidence` (design 3). Verify: unit tests (a borderline red/orange reading gives each about half; clear readings unchanged); video tests: both dim evening videos clear with the true cube, frames to clear on the others not worse.
- [ ] 1.5 Simulation with soft votes, threshold re-checked (design 5). Verify: sweep in `findings.md`; the small simulation test passes.

## 2. Notice (shared)

- [ ] 2.1 Stall notice at the bottom of the picture: icon, words, "Aloita alusta", "Korjaa värit", "Sytytä valo" (fi + en) per design 6; tap outside closes it until the next stall. Verify: Compose tests (scanning goes on with the notice shown; tap outside closes it; restart clears progress); one screenshot.

## 3. Docs

- [ ] 3.1 `docs/architecture.md` video scan step where the map changes; roadmap row `video-scan-light` done.
