# Tasks

## 1. Test data

- [x] 1.1 Commit the three fixtures extracted while planning (finder readings, 360×640 at 10 fps like 2026-10-05) under `cube/src/jvmTest/resources/video/`, each with a line in `VideoFixtures` (truth `RRRRWWRWGWRGRRGGGGWGRWGGWWWOOOYYOBYOBBBOOBYOBYYYYBBYBO` for all): `20261007_web` (14 clean frames of the failing web scan's start), `20261007_132049` (camera video, white/red/green faces, white reads bluish), `20261007_132721` (camera video, 42 s: white on top with blue beside it, then every face; finishes right today at frame 354, with a blue centre named white in a few frames). The videos themselves stay with the user (too large to commit); the fixtures are enough to rerun
- [x] 1.2 Regression tests on `20261007_132049` and `20261007_132721`: both read the true cube (the latter finishes); record frames-to-finish for 3.3 (before: 132049 never finishes, 39 known at the end; 132721 finishes at 354; after 2.1: 279)
- [x] 1.3 A failing test first: the fixture replayed as a still cube for ~10 s with the blue face's centre made pale (named white by the scan) leaves the white face wrong today; it is the regression test for 2.1 (the camera videos read blue darker than the browser did, so only this fixture with the paler centre shows the bug)

## 2. One centre colour per face per picture (cube)

- [x] 2.1 When two faces of one picture name the same centre colour, the closer keeps it and the other takes its next-best colour not taken in that picture (design 1); verify: the 1.3 test reads the white face right and the blue face gets its readings; a unit test with two synthetic faces naming the same colour

## 3. Later clear readings win (cube)

- [x] ~~3.1 Reading weight by squareness (design 2) on the vote shares and the anchor support; verify: a unit test that a straight-on reading outweighs a steep one~~ dropped (user, 2026-10-07; design "Findings")
- [x] 3.2 ~~Anchor by weighted support and~~ the readings window dropping the oldest (design 3–4); verify: a test that a face known wrong from 60 bad readings is put right by ~40 straight-on right readings, and "one bad frame" still holds
- [x] 3.3 Regression: all video fixtures (2026-10-05 day and evening, 2026-10-07) still finish with the true cube, and not later than now (print frames-to-finish before/after; a few frames' slack is fine)

## 4. Docs

- [x] 4.1 `docs/architecture.md`: the video-scan paragraph gets the per-picture centre rule and the readings window in a line each (details stay in the code); roadmap row for `scan-centre-clash` marked done
