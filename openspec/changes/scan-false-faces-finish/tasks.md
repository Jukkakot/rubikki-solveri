# Tasks

## 1. Material and measurement

- [ ] 1.1 Recording `web_20261012_100233` into `cube/src/jvmTest/resources/recordings/` (striped cube, `STRIPED_TRUTH`); a harness (`FALSE_FACES=1`) that counts per source the lattices whose reading fits no true face (fewer than 6 of 8) and, for true faces, the half-step and outer-row size ratios (design 1); run it on every committed still and the recordings; verify the numbers separate true faces from lattices over an edge, or report.

## 2. Finder (cube, commonMain)

- [ ] 2.1 `FaceFinder`: reject a lattice whose half-step or outer-row size ratio is beyond the limits measured in 1.1; JVM test: a lattice over the cube's edge from a committed still is rejected and that still's true faces are still found; verify `./gradlew :cube:jvmTest` passes and the harness shows no true face lost.

## 3. Mirror finish

- [ ] 3.1 Reproduce a half-round (mirror) finish offline (design 2: `202156` with CENTRES, a slower picture order of `web_181940`, the 10:02 recording) and log at the finish which faces were settled how and the half-round cube's cost; write the cause in design Findings; verify a reproduction exists, or stop and report.
- [ ] 3.2 Fix the cause with a general rule (design 2); JVM test: the reproduced source no longer finishes wrong; verify `./gradlew :cube:jvmTest` passes.

## 4. Comparison

- [ ] 4.1 `ColourSpikeHarness` on every source before and after: no source later by more than 10 % or wrong where it was not (design 3); the table in design Findings; verify the go rules are stated as met or not.

## 5. Docs

- [ ] 5.1 `docs/architecture.md` (finder flatness check, finish rule), roadmap row, backlog item on the striped mirror closed; list the phone check (scans of the striped cube); verify the roadmap row matches the outcome.
