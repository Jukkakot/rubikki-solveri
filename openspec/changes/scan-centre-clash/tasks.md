# Tasks

## 1. Test data

- [ ] 1.1 Commit the fixture `cube/src/jvmTest/resources/video/20261007_web.txt` (extracted while planning) and the 14 clean frames as JPEG stills under `testdata/video/2026-10-07/stills/` (360 px wide, like 2026-10-05), with a line in `VideoFixtures` (name, truth `RRRRWWRWGWRGRRGGGGWGRWGGWWWOOOYYOBYOBBBOOBYOBYYYYBBYBO`, what it shows)
- [ ] 1.2 A failing test first: the fixture replayed as a still cube for ~10 s with the blue face's centre made pale (named white by the scan) leaves the white face wrong today; it is the regression test for 2.1

## 2. One centre colour per face per picture (cube)

- [ ] 2.1 When two faces of one picture name the same centre colour, the closer keeps it and the other takes its next-best colour not taken in that picture (design 1); verify: the 1.2 test reads the white face right and the blue face gets its readings; a unit test with two synthetic faces naming the same colour

## 3. Later clear readings win (cube)

- [ ] 3.1 Reading weight by squareness (design 2) on the vote shares and the anchor support; verify: a unit test that a straight-on reading outweighs a steep one
- [ ] 3.2 Anchor by weighted support and the readings window dropping the oldest (design 3–4); verify: a test that a face known wrong from 60 bad readings is put right by ~40 straight-on right readings, and "one bad frame" still holds
- [ ] 3.3 Regression: all video fixtures (2026-10-05 day and evening, 2026-10-07) still finish with the true cube, and not later than now (print frames-to-finish before/after; a few frames' slack is fine)

## 4. Docs

- [ ] 4.1 `docs/architecture.md`: the video-scan paragraph gets the per-picture centre rule and the readings window in a line each (details stay in the code); roadmap row for `scan-centre-clash` marked done
