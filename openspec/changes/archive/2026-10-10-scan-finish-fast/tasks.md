# Tasks

## 1. Measure

- [x] 1.1 Harness metric per fixture (normal and robustness), recording and synthetic scramble: picture/time when the best cube first became right and clear, finish time, delay between them, wrong finishes; print a table (before state into the proposal's Implementation notes)
- [x] 1.2 For the slowest delays, find what holds the finish (which condition, which tracks)

## 2. Tune

- [x] 2.1 Try the levers one at a time (outvote ratio, clearness-relaxed ratio, turn settling once clear, `FINISH_MILLIS`, anything 1.2 found), keep each only if it shortens delays with zero wrong finishes and no fixture later than before
- [x] 2.2 Regression test: `web_20261010_102548` finishes within the delay reached (with a small margin); every recording and fixture still right

## 3. Check

- [x] 3.1 All unit tests, both app builds; per-frame time within the budget test
- [x] 3.2 Before/after table and kept levers in the proposal's Implementation notes; roadmap row 78 `scan-finish-fast` done; archive
