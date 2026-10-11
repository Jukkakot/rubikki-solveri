# Tasks

## 1. Variants (jvmTest)

- [x] 1.1 Add idea A's re-read (design 1): finder at 240 px on a still, each sticker read again from the full still at its lattice point; JVM test on a synthetic still with a sharp sticker grid (re-read colours equal the painted ones, 240 px colours blended at the edges); verify `./gradlew :cube:jvmTest --tests "*ColourSpike*"` passes.
- [x] 1.2 Add idea B's warm split (design 2): the 2-means hue split with its start condition and the pull to group means; JVM test with readings of red and a reddish orange under one cast (split right, lightness kept, nothing changed before the start condition); verify the same test command passes.

## 2. Comparison

- [x] 2.1 Add the harness (design 3, `COLOUR_SPIKE=1`): stills sources through today / A / B / A+B, recordings with a known cube through today / B; print the table; verify it prints for every source and that today's rows match today's scan (same finish as `FinderSizeHarness` at 240 and `RecordingReplay`).

## 3. Findings

- [x] 3.1 Write `findings.md` in this change: the tables, why the stuck recordings stuck or finished, go / no-go per idea (design 4), and the follow-up change if go; verify it answers both questions in the proposal.
- [x] 3.2 Add the outcome and any follow-up to `openspec/context/roadmap.md` (spike done, follow-up planned, backlog notes for the stuck recordings updated); verify the roadmap lists them.
