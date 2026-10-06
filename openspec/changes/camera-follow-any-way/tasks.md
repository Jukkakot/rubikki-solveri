# Tasks

## 1. Placing the seen cube (cube)

- [ ] 1.1 `FollowView`: place found faces on a known cube state (centre → side, rotation by best match against before/after/candidate states, two faces by geometry, continuity for ambiguous single faces, "cannot place"); orientation and projection from the placed faces. JVM tests with faces synthesised from cube states (as `ScanSimulation`): every one of the 24 holds, a corner view, a uniform side with and without continuity.
- [ ] 1.2 `FollowTracker` over placed faces: done / wrong move / not in view / waiting, summed scores, learning on all placed faces; whole-cube turns advance by themselves. Rewrite `cube/FollowTest` for the new input: move done in several holds, wrong direction, back turn seen only from the front → not in view, finger over one sticker still done.
- [ ] 1.3 `SideArrow` (replaces `FrontArrow`): choose the side and the sticker path for a move in a given orientation; tests: R seen from F (up the right column), R seen from R (round clockwise), U from a tilted hold, half turn marked double, nothing when no facing side shows it.

## 2. Camera pipeline and panel (shared)

- [ ] 2.1 Extract the video scan's camera pipeline (image → finder on a background thread / worker, `ExposureControl`, torch) into one composable used by `VideoScanScreen` and `DefaultFollowPanel`; video scan behaviour unchanged (`VideoScanScreenTest` still passes).
- [ ] 2.2 `FollowPanel`: no grid; arrow drawn on the projected cube, "2×" label, the colour dots on placed stickers; messages for no cube, tilt to show two sides, turn so the turning side shows, wrong move; torch button. Strings fi/en (replace `follow_hold`, `follow_bring_cube`). Update `app` `FollowTest` smoke test (renders, done advances).
- [ ] 2.3 Move text by centre colour when held another way; guide cube view set from the placed orientation (snapped, eased, back to the holding view without a cube). A Compose test that the text changes between the usual and another hold (no exact copy).
- [ ] 2.4 Log lines (`follow.event` with view and hold, `follow.place`); `./gradlew` build, unit tests and the web build pass.

## 3. Docs

- [ ] 3.1 `docs/architecture.md` Camera follow section (found faces, placing, side arrow, shared camera pipeline); roadmap: add row 46 `camera-follow-any-way` done, drop the camera-follow backlog item.
