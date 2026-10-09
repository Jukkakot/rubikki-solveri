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
