# Tasks

## 1. Steps with parts (cube)

- [ ] 1.1 `StepPart` (`Hold`, `Setup`, `Combo(algorithm, times)`, `Free`) and `Step.parts`; `MacroSearch` returns its chosen macros, `BeginnerSolver` records them; JVM tests on many scrambles: the parts concatenate to the step's moves, every `Combo` is one of the method's algorithms, no `Hold` inside a `Combo`; verify `./gradlew :cube:jvmTest` passes.

## 2. Combinations mode (shared)

- [ ] 2.1 `SolveMethod` gains the combinations way on the start screen (texts FI/EN); the step card: piece and target outlined, hold card, setup line, combination with × n, "Näytä", move-by-move stepping, "Tein sen", lesson link; smoke tests: a corner step shows its combination and count, a cross step shows no combination.
- [ ] 2.2 "Näytä" plays only the combination's layer moves from the cube after hold and setup, the view fixed; test: the animation's moves contain no whole-cube turn.

## 3. Docs

- [ ] 3.1 `docs/` (screens), roadmap row 86 (replacing `solve-challenge`); list for the user what to try on the phone.
