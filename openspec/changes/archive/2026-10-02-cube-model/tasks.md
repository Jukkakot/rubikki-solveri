# Tasks

## 1. State and geometry

- [x] 1.1 `CubeColor`, `Face`, `ColorScheme.STANDARD`, sticker geometry (position + normal per facelet index), `Cube.solved()`; verify tests "Solved cube" and that geometry matches the URFDLB net
- [x] 1.2 `Move` (face/slice/wide/rotation × amount) with cached permutations, `Cube.apply`; verify tests "Four quarter turns", "Known cycle", "Turn direction", "Rotation keeps solved"

## 2. Notation and sequences

- [x] 2.1 Parser and printer with error position; verify tests "Parse and print", "Bad token"
- [x] 2.2 Inverse and simplification; verify tests "Inverse undoes", "Merge and cancel"
- [x] 2.3 Scramble generator with injectable random; verify test "Scramble shape"

## 3. Pieces and validity

- [x] 3.1 Corner/edge facelet tables and piece view (permutation + orientation) through the centres; verify tests that the tables match the geometry and that the solved cube is all-home
- [x] 3.2 Validity check with reasons; verify every validity scenario and 1000 random scrambles valid
- [x] 3.3 Update docs (architecture: cube model section) and mark roadmap item 1 done
