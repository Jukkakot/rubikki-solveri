# Tasks

## 1. Failing test

- [ ] 1.1 Test: replaying recording `web_20261009_100824` finishes with the true cube `RGYGWBWOGWWGGRRYWOGWROGYBYROBGRYRWOWBWOOOBRBYORYYBGBYB` (fails today); `web_20261009_100814` still finishes with it
- [ ] 1.2 Record every fixture's finish frame and cube now (acceptance harness, normal and robustness) as the before state

## 2. Whole-cube recheck

- [ ] 2.1 In `FaceTracks`: when not clear and a counting track got a new reading (at most every 250 ms), search every turn of one representative track per named face (plus doubtful namings) with `BestCube` cost/validity, reusing `RotationSearch`
- [ ] 2.2 Adopt a valid whole that is cheaper by `ASSIGN_MARGIN`: set every counting track's face and turn, also settled ones (`byCube`)
- [ ] 2.3 Tests: the recording of 1.1 passes; a synthetic case with three faces settled at jointly wrong turns is corrected; a clear cube is never changed; budget test for the recheck (~2 ms)

## 3. Disagreement score (only if still needed)

- [ ] 3.1 Per-track disagreement with the best cube; a track above 3/8 counts half while no whole is clear
- [ ] 3.2 Test: one face misread with wrong colours does not stop the others from finishing

## 4. Check, docs

- [ ] 4.1 All cube tests and fixtures: same finishes and cubes as 1.2, no wrong finish, acceptance bar kept; build both apps and run all unit tests
- [ ] 4.2 `docs/architecture.md` scanner section: the whole-cube recheck; roadmap row 73 `scan-never-locked` done
