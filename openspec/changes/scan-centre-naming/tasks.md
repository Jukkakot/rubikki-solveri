# Tasks

## 1. Test data and measurements

- [ ] 1.1 A failing test first: `20261007_132721`'s blue-face-only frames with the centre made pale (named white by the palette), replayed for ~3 s before the whole video; today a known sticker of the white or blue face is wrong at some frame, or the finish comes later than the plain video's. Print frames-to-finish for all video fixtures (the baseline for 3.2)
- [ ] 1.2 Measure on all video fixtures (brightness-free centre distance): the spread of one face's centres and the distance between different faces' centres (blue/white, red/orange); pick `JOIN_WITHIN` and `DOUBT_MARGIN` and note the numbers in `design.md`

## 2. Piles and their names (cube)

- [ ] 2.1 Joint naming for k ≤ 6 centres (distinct colours, least summed distance, current names win a tie); verify: unit tests with 3, 5 (sixth follows) and 6 centres
- [ ] 2.2 Groups by identity, joined by nearest mean centre within `JOIN_WITHIN`, the per-picture rule kept, at most six (stray dropped), merge of close piles (design 1–3); verify: unit test that a pale blue face and the white face shown at different times make two piles and are named blue and white
- [ ] 2.3 Doubtful pile: no evidence, no known stickers, no finish (design 4); verify: unit test that a face alone with a centre between white and blue shows nothing known, and does once the white face is seen; the 1.1 test passes

## 3. Regression

- [ ] 3.1 All existing `VideoScanTest`s pass (one bad frame, clash, window, partial faces)
- [ ] 3.2 All video fixtures finish with the true cube, not later than in 1.1 (a few frames' slack); print before/after

## 4. Docs

- [ ] 4.1 `docs/architecture.md`: the video-scan paragraph's centre line says faces are piled by their own centres and named together, doubtful ones wait (one line); roadmap row for `scan-centre-naming` added as done
