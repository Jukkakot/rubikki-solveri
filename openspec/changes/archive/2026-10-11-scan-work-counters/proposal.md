# Proposal

## Why

The scan's budget test (`RulesScanTest.aLongScanStaysWithinItsBudgetPerPicture`) checks CPU milliseconds per
picture. On a slower machine (the cloud session of 2026-10-11) it passes or fails from run to run, with or
without a change. A test that depends on the machine cannot tell a real regression from a slow runner. The
requirement it guards ("Reading stays quick in a long scan": the work per picture does not grow over a long
scan) is about the amount of work, and the scan can count that exactly. The user agreed (2026-10-11) to build
this before going on with `scan-warm-references`, keeping a loose millisecond ceiling for gross slowdowns.

## What Changes

- **Work counters in the scan (`cube`)**, counted per picture and readable by tests and harnesses, never
  shown in the app:
  - sticker namings: a reading's colour named against the references
  - assignment search nodes: the joint face assignment's branch and bound
  - best-cube solves
  - tracks worked through
- **The budget test asserts on the counters.** On both long recordings, each 100-picture window does no more
  work than set limits, and late windows do not do more than the busiest early ones by more than a margin.
  The same answer comes out on every machine.
- **A loose millisecond ceiling stays**, raised from 5 to 15 ms a picture (tripled on CI as now), so a gross
  slowdown that the counters miss still fails.
- **`ColourSpikeHarness` prints work per picture** beside the scan's milliseconds, so comparisons between
  reference modes show where extra work comes from.
- No change to the app's behaviour.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None: tests and a harness measure an existing requirement differently (`skip_specs`).

## Impact

`cube` only: counters in `FaceTracks` (commonMain, internal), `RulesScanTest` and `ColourSpikeHarness`
(jvmTest). `app`, `shared` and `web` are untouched. Counting costs a few integer increments per picture.
