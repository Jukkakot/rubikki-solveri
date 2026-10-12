# Design

## Context

- `BeginnerSolver.solve(cube)` gives steps grouped by `Stage` (7 stages, white cross … yellow corners turned), each
  with moves and a note; `Checks.stageDone(cube, stage)` tells whether a stage is done.
- The solution screen (`SolveScreen`, `SolveMethod.FAST`/`LEARN`) runs the move guide (`MoveGuide`, `StepperState`)
  and the learning mode's stage cards with a goal picture (`Stage goal picture`, lessons).
- The video scan returns a cube (`ScanOutcome`); the start screen opens after it with a new scan behind.

## Goals / Non-Goals

**Goals:** the user solves each stage on the real cube by themselves, sees the goal, gets one move at a time when
stuck, and the app checks the result from the real cube.

**Non-goals:** checking every move as it is made (camera follow does that elsewhere); a new solving method;
timing competitions.

## Decisions

1. **`SolveMethod.CHALLENGE`.** A third option on the start screen; the solution is the beginner solution as for
   learning, used only for the goals and hints.
2. **The goal cube.** The user's cube after the stage's last step of the beginner solution, drawn with the 3D cube
   (turnable by dragging); stickers of pieces the stage does not place yet are grey. *Alternative:* the lessons'
   generic goal picture: simpler, but does not show where the user's own pieces go.
3. **Known cube.** The app tracks a "known cube": the scanned cube, then each hint's move applied. A hint is valid
   only while the user has made just the hinted moves; after own tries the user scans ("Valmis") and the known cube
   becomes the scanned one. The hint button says so ("Vihje edellisestä tarkistuksesta").
4. **Check by video scan.** "Valmis" opens the video scan; its cube must be the same cube (same pieces; a cube that
   does not fit, e.g. a misread, says "Tämä ei näytä samalta kuutiolta" and offers to scan again). If the stage and
   all earlier ones are done → next stage, solution worked out again from the scanned cube. If not → the goal again
   with the pieces out of place marked.
5. **Counting.** Hints per stage and in total; shown on the finish and saved with the solve (question 4).

## Risks / Trade-offs

- [Scanning after every stage is slow] → the scan takes 3–8 s now; the user may also skip the check ("Seuraava
  ilman tarkistusta"), then the known cube is assumed (question 1).
- [Hints wrong after own moves] → decision 3 makes it explicit.

## Open Questions

See the questions to the user (proposal review).
