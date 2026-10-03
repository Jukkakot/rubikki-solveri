# Tasks

## 1. Half turn in two steps

- [ ] 1.1 In `CubeAnimator`, play a half turn as two quarter steps (same direction, quarter timing) with a ~0.25 s pause between; the cube shows the state after the first quarter during the pause; `snapTo` during either step or the pause still drops everything; instant mode unchanged
- [ ] 1.2 Let the animator report the end of each quarter step (a callback or counter) so a screen can react to it
- [ ] 1.3 Test (JVM, virtual time): R2 passes through the state after R, pauses, ends after R2; queued moves after a half turn still play in order; a `snapTo` mid half turn leaves the snapped cube

## 2. Demo ticks

- [ ] 2.1 On the solve screen, a demo of a half turn ticks after the first quarter step as well as at the end (two light ticks); done/back do not add ticks
- [ ] 2.2 Keep the existing solve/lesson screen tests green; no new UI test (behaviour is in the animator test)

## 3. Docs and roadmap

- [ ] 3.1 Update the move-player note in `docs/architecture.md` if it describes half-turn timing
- [ ] 3.2 Mark roadmap item 22b `turn-steps` done
