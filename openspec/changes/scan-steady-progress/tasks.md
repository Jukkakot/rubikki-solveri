# Tasks

## 1. Naming and progress in the scan (cube)

- [x] 1.1 Add a JVM test in `VideoScanTest` with the striped cube `WWWWWWWWWBRGBRGBRGOGROGROGRYYYYYYYYYGOBGOBGOBRBORBORBO`: the red centre made orange-ish (nearest colour orange), U, D, F, L, B shown in corner views, then the red face several times; assert the red face is named red, its stickers become known and the scan completes. Verify it fails on the current code and note which `nameJointly` branch kept red doubtful.
- [x] 1.2 Keep a face out of a pile whose neighbour sides it contradicts (design 1, replaces the planned sixth-by-elimination); verify 1.1 passes and the existing naming tests (pale blue alone, blue first, orange first, two faces naming the same centre) still pass.
- [x] 1.3 Keep `sticky` on a rename (design 2); add a test that renames a pile with known stickers (e.g. a pile first named by what was left, then by the joint naming) and assert the known count does not drop; verify `./gradlew :cube:jvmTest` passes.

## 2. Paint (shared)

- [ ] 2.1 Add dots for known stickers to `ScanPaint` (found faces and projection sides) and draw them in the paint layer with the veils' glide and alpha (design 3); extend `ScanPaintTest` to assert a known sticker gets a dot in its colour and a needed one a veil; verify `./gradlew :app:testDebugUnitTest --tests "*ScanPaintTest*"` passes.
- [ ] 2.2 Raise `MOVING_SIDES_PER_SECOND` to 1.0 (design 4) and update the `MotionFade` test values so a slow drift (0.5 side/s) keeps the marks and a fast move (2 side/s) hides them; verify the same test run passes.

## 3. Wrap-up

- [ ] 3.1 Run the project's check chain (`./gradlew :cube:jvmTest :app:testDebugUnitTest` and the web build used by CI) and fix what fails.
- [ ] 3.2 Update `docs/` where the scan paint is described (dots back on known stickers) and the roadmap entry; verify no doc still says known stickers are left bare.
