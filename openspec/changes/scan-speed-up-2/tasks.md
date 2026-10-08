# Tasks

## 1. Finder picture size (cube, shared, web)

- [x] 1.1 Harness measurement (design 1): full faces per picture, finder time and scan result on `testdata/video/` for long sides 360/320/280/240; numbers and the choice into `design.md`
- [x] 1.2 `FrameSampler.upright` by long side (`FINDER_LONG_SIDE`, the chosen value), the browser's `PREVIEW_LONG_SIDE` the same constant; tests that use `upright` updated

## 2. Phone build (app)

- [x] 2.1 `profile` build type (design 2), installed with adb over the debug app; compare `finderMs`/`scanMs` with the debug build on the phone (log or a short on-device timing run); result into `design.md`; keep or drop as decided there

## 3. Search window (cube, shared, web)

- [x] 3.1 (dropped after measuring: ~3 %, see design) `FinderWindow` in `cube` (design 3) with JVM tests: grows the faces' box by half, clamps, whole picture every 5th search and after a miss; faces moved back by the window's corner. Harness check: the recorded videos give the same scan result with the window as without
- [x] 3.2 (dropped with 3.1) Phone: the finder loop crops the `ArgbImage` to the window; browser: the page sends only the window's part of the video at the same scale and moves the faces back

## 4. Two finders (shared, web)

- [x] 4.1 (dropped after measuring: both read at the camera rate, see design) Phone: two finder coroutines, answers numbered, older ones dropped; browser: two finder workers, the page sends to an idle one, older answers dropped (their picture copies freed). Unit test of the drop rule (pure)

## 5. Scan logic off the drawing thread (cube, shared, webworker, web)

- [x] 5.1 Phone: `VideoScan.onFrame` in the pipeline's single-threaded background context, the state handed to Compose
- [x] 5.2 (in the finder worker, see design) Browser: kotlinx.serialization in `cube` for the state the screen uses and `ScanOutcome`; a scan-logic worker (second entry of `webworker`) owning `VideoScan` (faces in, state out; reset, outcome, engine as messages); the page uses it when workers run, else as now. Codec round-trip test on the JVM; `web/smoke/video.mjs` passes

## 6. Wrap-up

- [x] 6.1 `./gradlew check`, web build, both browser smoke tests
- [x] 6.2 Docs: `docs/architecture.md` (finder size, window, two finders, scan worker; where each lives); roadmap entry
- [x] 6.3 Install on the phone; list for the user to try: pictures a second (`fps`) and `finderMs` in the log of both, a cube held at the usual distance still read, nothing else changed
