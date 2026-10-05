# Tasks

## 1. Reading in different light (cube)

- [ ] 1.1 Measuring harness (design 1): per video, distances of true red and orange readings to both references, raw; table into `findings.md`. Verify: runs on all six fixtures, numbers printed.
- [ ] 1.2 Light correction per frame (design 2). Verify: unit test (a frame tinted warm reads like the untinted one); harness numbers before/after in `findings.md`.
- [ ] 1.3 Glare left out of a sticker's colour (design 4); fixtures regenerated. Verify: unit test (a blob with a bright washed-out patch keeps its colour); all video tests still pass.
- [ ] 1.4 Soft votes through groups and `StickerEvidence`, washed-out readings weighted down (design 3, 8). Verify: unit tests (a borderline red/orange reading gives each about half; a washed-out one counts little; clear readings unchanged); video tests: both dim evening videos clear with the true cube, frames to clear on the others not worse.
- [ ] 1.5 Side confirmed only by the best cube's margin (design 9). Verify: unit test (a side read many times but not confirmed has no tick).
- [ ] 1.6 Simulation with soft votes, threshold re-checked (design 5). Verify: sweep in `findings.md`; the small simulation test passes.

## 2. Screen and camera (shared, app, web)

- [ ] 2.1 Stall notice at the bottom of the picture per design 6 (fi + en): description, "Aloita alusta", "Korjaa värit", torch toggle; at least 5 s; tap outside closes it, the reason does not come back. Verify: Compose tests (scanning goes on with the notice shown; it stays 5 s; tap outside closes it and the same reason does not return; restart clears progress); one screenshot.
- [ ] 2.2 Torch re-meters the camera on Android and in the browser (design 7). Verify: unit test of the settle logic (frames in the settling second are not read); phone check listed for the user.
- [ ] 2.3 Browser camera picture from the video element under the app (design 10), guided scan and video scan. Verify: the web build runs; the preview box is transparent over the video in a quick browser check (desktop Chrome); phone check listed for the user.

## 3. Docs

- [ ] 3.1 `docs/architecture.md` video scan and browser camera where the map changes; roadmap row `video-scan-light` done.
