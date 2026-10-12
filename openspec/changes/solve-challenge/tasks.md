# Tasks

## 1. Logic (cube)

- [ ] 1.1 Stage goal cube: the beginner solution applied up to a stage's last step, and the pieces each stage places (for greying); JVM tests on scrambles: the goal cube passes `Checks.stageDone` for that stage, grey pieces are those of later stages.

## 2. Challenge screen (shared)

- [ ] 2.1 `SolveMethod.CHALLENGE` on the start screen (texts FI/EN); the challenge screen: stage name, aim, goal on the 3D cube, hint (move guide, counted), check button; smoke test per state (stage shown, hint shown).
- [ ] 2.2 Check through the video scan: done → next stage from the scanned cube; not yet → goal with pieces out of place marked; not the same cube → message and scan again; tests for the three outcomes with a fake scan result.
- [ ] 2.3 Finish: hints shown; saved with the solve if decided (question 4).

## 3. Docs

- [ ] 3.1 `docs/` (screens), roadmap row; list for the user what to try on the phone.
