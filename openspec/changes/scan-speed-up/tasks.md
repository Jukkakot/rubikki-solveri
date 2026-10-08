# Tasks

## 1. Face finder

- [ ] 1.1 Measure on the phone stills (`web_121505`, JVM): time for blobs against time for lattices per frame. Put the numbers in the proposal's Decisions.
- [ ] 1.2 Speed up the larger part without changing what is found. Running `VIDEO_HARNESS=1 writeFixtures` leaves every committed fixture unchanged. Note the new numbers.

## 2. Rules scanner

- [ ] 2.1 Use per-face turned vote sums for the trial turns of `settleTurns`, `turnsClear` and `leadingOf`. Tests and the acceptance harness give the same results. Note the per-frame time before and after (`202058` looped).

## 3. Log

- [ ] 3.1 Add the paint's average time per frame to the video scan log's snapshots.

## 4. Check

- [ ] 4.1 The acceptance harness is never wrong, and `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` passes.
