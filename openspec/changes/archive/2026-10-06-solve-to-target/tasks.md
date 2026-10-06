# Tasks

## 1. Solving to a target (cube)

- [x] 1.1 Two-phase solve from S to T via cubie cubes (X = T⁻¹·S); JVM tests: S.apply(m) == T for random S/T pairs, solved target gives the same length as today, S == T gives no moves.
- [x] 1.2 `Patterns`: the list from design §3 with ids and move sequences; tests: every pattern is a valid cube, checkerboard alternates on every side, six spots has each centre different from its ring, superflip has every edge flipped and everything else solved. Drop any pattern that fails its look check.
- [x] 1.3 `SolveTarget` (solved, pattern, stage, painted) with a short string form; round-trip test.

## 2. Picker and solution screen (shared)

- [x] 2.1 Picture of a target for the gallery and the target row (small still `GoalCube`, as in the lessons list; design §4).
- [x] 2.2 `TargetScreen`: surprise button, gallery grid with a preview sheet (large turnable cube + "Valitse"), stages list with goal pictures, "Maalaa oma"; strings fi/en with playful pattern names. Compose smoke test: renders, tapping a pattern and "Valitse" returns that target.
- [x] 2.3 Hand input in target mode (title, prefilled with the current target, no scan helpers, returns a painted target on valid).
- [x] 2.4 Solution screen: target row with "Vaihda", plan per target (pattern/painted → shortest only, stage → learn cut after the stage), "Kohde valmis!" finish, already-at-target message with "Valitse kuvio"; `SolveRoute` carries the target. Compose test: changing the target restarts the guide at move 1.
- [x] 2.5 Home tile "Kuviot" (fifth tile spans the last row) → picker → solution from a solved cube; home screenshot test updated; build, unit tests and web build pass.

## 3. Docs

- [x] 3.1 `docs/architecture.md` (solve to target, picker, routes in the screen table); roadmap: row 46 `solve-to-target` done, backlog item replaced by the left-over ideas (typed sequence, scan a target, partial target).
