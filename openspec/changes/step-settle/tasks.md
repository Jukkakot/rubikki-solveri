# Tasks

## 1. Stepping without a replayed turn

- [ ] 1.1 `StepperState`: the demo (automatic and "show") ends after the move (no snap back); "show" snaps to before the move first; `done()` snaps to the next step's cube instead of playing the move; `back()` snaps to before the previous move and lets its auto demo play. Verify: update the stepper/solve-screen tests (done after the demo does not play a turn; done during the demo lands on the next cube; back shows the cube before the previous move).
- [ ] 1.2 Nod: a short tilt of the whole cube (a few degrees and back, ~300 ms) on every step change, skipped when the animator is instant. Verify: shared build passes; one test that a step change with animations off leaves no tilt.

## 2. Mirror without the arrow

- [ ] 2.1 `Cube3D`: stop drawing the arrow in the reflection. Verify: shared build passes.

## 3. Docs and roadmap

- [ ] 3.1 Roadmap row `step-settle` done; screen gallery not refreshed (small UI change). Verify: roadmap row present.
