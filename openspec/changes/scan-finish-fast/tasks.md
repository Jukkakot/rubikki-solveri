# Tasks

## 1. Measure

- [ ] 1.1 Harness metric per fixture (normal and robustness), recording and synthetic scramble: picture/time when the best cube first became right and clear, finish time, delay between them, wrong finishes; print a table (before state into the proposal's Implementation notes)
- [ ] 1.2 For the slowest delays, find what holds the finish (which condition, which tracks)

## 2. Tune

- [ ] 2.1 Try the levers one at a time (outvote ratio, clearness-relaxed ratio, turn settling once clear, `FINISH_MILLIS`, anything 1.2 found), keep each only if it shortens delays with zero wrong finishes and no fixture later than before
- [ ] 2.2 Regression test: `web_20261010_102548` finishes within the delay reached (with a small margin); every recording and fixture still right

## 3. Check

- [ ] 3.1 All unit tests, both app builds; per-frame time within the budget test
- [ ] 3.2 Before/after table and kept levers in the proposal's Implementation notes; roadmap row 78 `scan-finish-fast` done; archive
