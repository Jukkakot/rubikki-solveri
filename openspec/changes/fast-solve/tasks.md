# Tasks

## 1. Solver

- [x] 1.1 Vendor min2phase with LICENSE (MIT election, commit); verify `:cube:compileJava` and lint ignore it
- [x] 1.2 `TwoPhaseSolver` (solve with probe settings, invalid/solved handling, random-state scramble); verify tests "Scrambled cube" (200 random cubes, ≤ 21, average ≤ 19.5), "Solved cube", superflip, invalid cube

## 2. Solution screen

- [x] 2.1 Move words (side, direction, seen-from) in both languages; verify unit test "Describe a move" for all 18 face moves in Finnish and English
- [x] 2.2 `SolveScreen` (background solve, loading, stepper with done/back/show, progress, finish, invalid message) and `SolveRoute`; verify Compose tests "Next and back", "Finished", "Solved cube"
- [x] 2.3 Wire manual input "Valid cube" and a free cube solve button; solver warm-up and log events; verify test "Valid cube" opens the solution route

## 3. Docs

- [x] 3.1 Update architecture (solver, screens), nfr licence note and mark roadmap item 3 done
