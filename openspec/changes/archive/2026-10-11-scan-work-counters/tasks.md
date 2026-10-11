# Tasks

## 1. Counters (cube, commonMain)

- [x] 1.1 Add internal work counters to `FaceTracks`, exposed through `VideoScan`: sticker namings (every call that names a reading's colour against references), assignment search nodes, best-cube solves, tracks worked through in a picture; running totals that a caller samples per picture; JVM test: a short fixture's counts are the same in two runs and grow with each picture that has faces, and a picture without faces after a steady state adds no namings; verify `./gradlew :cube:jvmTest --tests "*ScanWork*"` passes.

## 2. Budget test

- [x] 2.1 Rewrite `aLongScanStaysWithinItsBudgetPerPicture` on the counters: on both long recordings measure each 100-picture window's namings, nodes and solves; set limits at the measured busiest window plus 50 %, and require late windows (after the first 200 pictures) to stay under 1.5× the busiest early window; keep the CPU-ms window ceiling at 15 ms (tripled on CI) and the faceless-picture median as now; record the measured counts in a comment; verify the test passes three runs in a row here. *Done with twice, not 1.5×: the measured late windows reached 1.68× the early ones in web_121505 (namings, solves) with no growth, only what the camera saw; the slowdown this guards grew fourfold.*

## 3. Harness

- [x] 3.1 Add work per picture (namings, nodes, solves) to `ColourSpikeHarness`'s table beside ms; run it; verify every row prints the columns and the CENTRES rows match the table in `scan-warm-references` design Findings.

## 4. Docs

- [x] 4.1 Note in `docs/development.md` (where the scan's tests and harnesses are described) that the budget test counts work and that ms are only a loose ceiling; add the roadmap row (done); verify the roadmap lists `scan-work-counters` before `scan-warm-references` resumes.

## Workflow follow-up

- Archive with `/opsx:archive scan-work-counters`, commit and push to `main`; then resume `/opsx:apply scan-warm-references`.
