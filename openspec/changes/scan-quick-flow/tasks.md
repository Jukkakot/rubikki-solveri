# Tasks

## 1. Face review accepts by itself

- [ ] 1.1 `ScanScreen`: during a review, "Hyvä, seuraava" fills up over 2 s (same look as the check's "Näyttää oikealta") and then calls accept; a touch on the review picture stops it; retake and manual stay. Verify: Compose test that a captured face is accepted after the time with no tap, and stays in review after a touch.

## 2. No check after a confident scan

- [ ] 2.1 `RubikkiNavHost`: a confident scan result navigates to the check and on to the solution in one go, so the check sits behind the solution; an unsure one opens the check as now. Remove the check's auto-continue timer (no longer used). Verify: test that a confident result shows the solution and back shows the check; an unsure result shows the check.

## 3. Arrow before the demo

- [ ] 3.1 `StepperState`: auto demo delay 1.5 s (arrow shown meanwhile); "show" unchanged. Verify: update `MoveGuideTest` timings; test that the arrow move is presented before the demo starts.

## 4. Docs and roadmap

- [ ] 4.1 Roadmap: row `scan-quick-flow` done, remove the "Next fix batch" backlog entry. Verify: roadmap row present.
