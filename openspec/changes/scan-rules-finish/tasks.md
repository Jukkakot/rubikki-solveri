# Tasks

## 1. Finish

- [ ] 1.1 Test first: a striped fixture played until the rules scan is clear, then a new face track (a face seen again from another angle) starts every few frames; today it never finishes. Verify it fails.
- [ ] 1.2 Fix: once complete, an open track that reads like the best cube where it is assigned (at most one sticker otherwise, like `settledFor`'s short tracks) does not revoke it; a track that reads against it or a clearness drop still does. 1.1 passes.

## 2. Speed

- [ ] 2.1 Measure: a fixture looped three times (many tracks); per-frame time and track count in the first and last third. Note the numbers in the proposal's Decisions.
- [ ] 2.2 Drop ended short unsettled tracks; per-frame work (pair costs, tables, rechecks, assignment) over a bounded set of tracks per face, older ones kept only as their votes. Test: the looped fixture's track count in the work stays bounded and its last third is not slower than ~1.5× the first.

## 3. Phone fixture

- [ ] 3.1 The camera part of `testdata/video/2026-10-08c` screen recording as stills (360 wide, 10 fps, like `web_084657`), added to the fixtures and the acceptance harness (rules scanner never wrong).

## 4. Check

- [ ] 4.1 Acceptance harness never wrong, the confirmed eleven within the bar; `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` passes.
- [ ] 4.2 Profile a frame on the fixtures (finder, tracking, assignment, best cube, paint) and bring the user a short list of speed-up ideas with expected gain; no code for them here.
