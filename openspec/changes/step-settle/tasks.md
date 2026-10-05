# Tasks

## 1. Stepping without a replayed turn

- [ ] 1.1 `StepperState`: the demo (automatic and "show") ends after the move (no snap back); "show" snaps to before the move first; `done()` snaps to the next step's cube instead of playing the move; `back()` unchanged (snap to before the current move, animate the undo, auto demo). Verify: update the stepper/solve-screen tests (done after the demo does not play a turn; done during the demo lands on the next cube; back still animates the undo).
- [ ] 1.2 Nod: a short tilt of the whole cube (a few degrees and back, ~300 ms) on done and on a move detected by camera follow (not on back, not after the last move), skipped when the animator is instant. Verify: shared build passes; one test that a step change with animations off leaves no tilt.

## 2. Mirror without the arrow

- [ ] 2.1 `Cube3D`: stop drawing the arrow in the reflection. Verify: shared build passes.
- [ ] 2.2 `CubeScene.arrow`: when the turning face points away from the camera, place the arc around the outside of the layer (layer depth, radius outside the cube's outline, middle towards the camera). Verify: unit test that a back turn in the holding view gives an arc whose projected points lie outside the cube's front face, and that a front turn is unchanged.

## 3. Solved celebration

- [ ] 3.1 Solve screen: on finishing, the cube hops and spins once (~1 s) and sticker-coloured confetti bursts from it; success vibration; nothing moves when animations are off. Verify: one Compose test that the solved screen appears after the last done with animations off (no crash, solved text present).

## 4. Docs and roadmap

- [ ] 4.1 Roadmap row `step-settle` done; screen gallery not refreshed (small UI change). Verify: roadmap row present.
