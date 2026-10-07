# Tasks

## 1. Striped cube fixture

- [x] 1.1 Cut stills from `testdata/video/2026-10-07/web-181940/rec.mp4` (camera part of the screen, 10 fps, 360 px wide) into `testdata/video/2026-10-07/stills/web_181940/`, add it to `VideoScanHarness.writeFixtures` and `VideoFixtures` with the true striped cube; verify the fixture file is written and today's `VideoScan` replays it (frames and finish logged).

## 2. Corner reader (cube)

- [x] 2.1 Add the corner reader (design 1–2): three full faces that pairwise touch → the corner, its 24-hypothesis naming with a margin, and each face's rotation; JVM tests with synthetic readings of a known cube for every corner, including a red centre looking orange beside white and green (named red by handedness) and a slipped lattice (no corner); verify `./gradlew :cube:jvmTest --tests "*Corner*"` passes.
- [x] 2.2 Check handedness on the fixtures: for every three-face frame of the fixtures with known cubes, the corner reader's best hypothesis against the true cube; verify the share right is printed and the sign is not flipped on any source.

## 3. Comparison harness

- [x] 3.1 Add the corner-anchored variant (design 3) in the harness and replay every fixture through it and through today's `VideoScan`; print per fixture frames to finish, wrong cubes, share of corner frames and whether and when both opposite corners were seen; verify the table prints for all fixtures.
- [x] 3.2 Add the robustness run (design 4: red faded towards orange, blue towards white) and print the same table; verify it runs on all fixtures.

## 4. Findings

- [x] 4.1 Write `findings.md` in this change: the tables, what failed and why, go / no-go, and if go the recommended shape (hidden in free scanning or guided two corners) with a sketch of the follow-up change; verify it answers each question in the proposal.
- [x] 4.2 Add the follow-up (or the no-go) to `openspec/context/roadmap.md`; verify the roadmap lists this spike as done with its outcome.
