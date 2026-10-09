# Design

## Context

`FaceTracks` assigns each followed face (track) a face and turn (`FaceOption`). The cost of an option
is the track's stickers against the settled tracks and the pieces they form. A track settles when
every other option costs `ASSIGN_MARGIN` more, and it re-opens only when another option becomes
cheaper, measured against the other settled tracks. A jointly wrong set is therefore a stable local
minimum. In `web_20261009_100824` every main track settled the right face with the wrong turn (U by
one quarter, D by a half, L and R by a quarter) and stayed there. Replay with `RecordingReplay`, as
`RECORDING=web_20261009_100824` with `--rerun`.

## Goals / Non-Goals

**Goals:**
- Escape a jointly wrong assignment within about a second of the reading that shows it.
- Down-weight the readings that disagree with the best cube, so a misread face cannot stall the rest.
- No regression: every fixture keeps its finish and cube, and none finishes wrong.

**Non-Goals:**
- Changing the finder, the colour reading or the finish rule (`FINISH_MILLIS`, `CLEAR_MARGIN`).
- Any visible message.

## Decisions

1. **Whole-cube recheck.** When the cube is not clear and a counting track got a new reading, the
   faces are named from the counting tracks' leading centres plus each still-doubtful naming.
   One representative track per face is taken, the one with the most readings, with its leading
   colours. All 4⁶ turn combinations are searched with `BestCube` cost and validity, reusing
   `RotationSearch`, which already does this for the guided scan. Cost: at most 4096 cheap
   evaluations, with pruning by pieces. A budget test keeps the recheck at no more than about 2 ms
   on the JVM, and the recheck runs at most every 250 ms.
2. **Adopt only when clearly better.** The recheck's best whole replaces the current assignment
   when its cube cost is lower by `ASSIGN_MARGIN` (the existing margin) and it is valid. It then
   sets every counting track's face and turn, settled or not (`byCube = true`), and the tracks'
   votes are re-read in the new turns. "Renaming keeps what stickers were read as" already holds,
   because votes live in the track's frame.
3. **Disagreement score.** For each counting track, it is the share of its leading outer stickers
   that differ from the best cube in its assigned place. When no whole is clear, the evidence
   from a track scoring above 3/8 counts half. Its newest readings can bring it back. This is the
   "found fast" part, and it acts within the same frame.
4. **Silent.** No UI change. The paint already follows `confirmed` and the known stickers each frame.
5. **Order of work.** Write the failing test for the recording first. Then do the recheck (task 2),
   then the score (task 3) only if the fixtures show it is still needed after the recheck.

## Risks / Trade-offs

- **Flip-flopping between two wholes.** Adoption needs the margin, and the 250 ms gate limits how
  often it can happen. The fixtures' "Held still / nothing switches back and forth" tests guard it.
- **A wrong whole adopted.** The recheck uses the same `BestCube` validity as the finish, and the
  finish still needs `FINISH_MILLIS` of clearness. The acceptance harness (13 fixtures, robustness
  variant) must keep zero wrong finishes.
- **Time.** The budget test and the logged `scanMs` keep it in check. `scanMs` was 6–20 ms in the browser.

## Implementation notes

- **Turns only, not namings.** `FaceTracks.wholeRecheck` turns each face's assigned readings together
  (all its tracks, not one representative: same-face tracks stay aligned) over every way, 4ⁿ for the n
  faces with readings. Doubtful namings were left out: the joint assignment already re-names open
  tracks every picture, and the recording needed only the turns. The spec says so.
- **Pictures bind.** A way that breaks a `PairRules` rule between two tracks is not tried. Without
  this, three faces seen together (`aDarkBlueCentreNamedWhiteDoesNotSpoilTheWhiteFace`) were turned
  against what the picture showed and flipped back every 250 ms.
- **Pruning.** Ways are ranked by wrong pieces in the leading colours (an edge of two colours that
  cannot meet, a corner not in the right order); only ways with no more wrong pieces than now, the
  best `RECHECK_TRIALS` = 16, get `BestCube.cost`. Without the "no more than now" filter the recheck
  took 1.8 ms median; with it 0.22 ms median, 2.9 ms 90th, 4.7 ms most (JVM, 58 runs on the recording).
- **Before `hold`.** The recheck runs before `hold`, so the other steps cannot turn its result back
  without a new reading (`aTrackDoesNotSwitchBackAndForthWithoutNewReadings` failed when it ran after).
  Adopted turns are by the cube (`byCube`) from then on.
- **Recording.** `web_20261009_100824` now finishes with the true cube about 10 s in (picture ~205 of
  891); before it never finished. `web_20261009_100814` unchanged. Synthetic: of 60 scrambles with
  each face shown alone and its reading turned, 5 never finished before; all 60 finish right now.
- **Fixtures (`ScanAcceptanceHarness`).** As recorded: every finish frame and cube the same (13 of 18
  finish, all right). Robustness: `20261007_202403` now finishes right at 148 (never before; 8 → 9 of 18); the
  rest the same. No wrong finish. Per frame 2.0 → 2.2 ms on average.
- **Task 3 (disagreement score) not done:** no fixture or recording needed it after the recheck. Kept
  in the roadmap backlog.
