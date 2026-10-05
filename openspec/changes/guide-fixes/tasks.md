# Tasks

## 1. Show always plays

- [ ] 1.1 Reproduce in `MoveGuideTest`: done (or show) while a demo is turning, then show on the next move; it must play and end after the move. Fix the cause in `CubeAnimator` (a snap during a running turn must not stop later moves). Verify: the new test fails before the fix and passes after.

## 2. Arrow, pause and nod

- [ ] 2.1 `GuideCube`: show the arrow while the presented move turns from the step's own cube (not after the demo); remove the nod (keep the celebration). `StepperState`: auto demo delay 0.5 s; drop the nod counter. Verify: `MoveGuideTest` timings updated; shared build passes.

## 3. Roadmap

- [ ] 3.1 Roadmap row `guide-fixes` done. Verify: roadmap row present.
