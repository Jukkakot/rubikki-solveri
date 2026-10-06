# Tasks

## 1. Held projection (cube)

- [x] 1.1 In `VideoScan`, keep the last projection and its build time:
  - Re-anchor it by translation onto the largest found face when a frame has faces but no settled
    orientation.
  - Return it unchanged in a frame without faces.
  - Add `projectionAge` to `VideoScanState` and clear the held projection on reset.

  Verify with new tests in `cube/src/jvmTest/.../VideoScanTest.kt`:
  - The projection is held over a faceless frame.
  - It is moved onto a found face's centre.
  - Its age grows.
  - Reset clears it.
- [x] 1.2 Remove `hint` and the tilt search from `VideoScanState` and `VideoScan`, together with the
  log fields that use them. Verify that `./gradlew :cube:jvmTest` and the log test
  (`VideoScanLogTest`) pass.

## 2. Reading every picture

- [x] 2.1 Android: remove the `IMAGE_MILLIS` throttle in `CameraPreview.android.kt`. Keep
  keep-only-latest and the drop-oldest buffer. Verify that `./gradlew :app:assembleDebug` builds
  and that the video snapshot still logs `fps`.
- [x] 2.2 Browser: in `WebCamera.kt`, send a picture to the worker whenever it is idle, and read
  frames every animation frame on the worker path. The page-thread fallback keeps 66 ms. Verify
  that the web build (`./gradlew :web:wasmJsBrowserDistribution` or the repo's web build task)
  succeeds.

## 3. Painted screen (shared)

- [x] 3.1 Draw tiles instead of dots:
  - Known stickers: shrunk quadrilaterals from the neighbouring centres.
  - Needed stickers: grey dashed tiles.
  - Confirmed sides: a white outline.
  - The paint fades with `projectionAge` (from 0.6 s, gone at 1.2 s).

  Verify with a Compose test in `VideoScanScreenTest`: a known sticker and a needed sticker are
  drawn, and the paint is hidden after 1.2 s without faces.
- [x] 3.2 Smooth the drawn centres at the display rate:
  - Exponential approach of about 60 ms per sticker index.
  - Jump at once when the front face changes.

  Verify with a unit test of the smoothing function: it moves part-way per frame, and it snaps on
  a pose change.
- [x] 3.3 Remove the turning arrow and the six-ball row, and add the progress ring (known/54, full
  at finish). Verify with a Compose test: the ring's described share is 0.5 with 27 known, and 1
  once finished.
- [x] 3.4 New layout:
  - Camera edge to edge.
  - Overlaid round back, torch and ⋮ icons.
  - A menu with "Kuva kerrallaan", "Syötä käsin" and "Korjaa värit".
  - A status pill: show the cube, show the grey parts, or ready.
  - Nothing below the picture.

  Add the Finnish and English strings. Verify with Compose tests:
  - No bottom buttons.
  - The menu entries switch to the guided scan, manual input and the check.
  - The pill text follows the state.
- [x] 3.5 Update the screenshot tests' reference for the video scan, if the repo has one for it.
  Verify that `./gradlew test` passes.

## 4. Docs and roadmap

- [x] 4.1 Update the scan part of `docs/architecture.md` (held projection, every picture read, no
  arrow). Add `scan-paint` to `openspec/context/roadmap.md` as done, together with the user's
  decision of 2026-10-06. Verify by reading the pages through once.

## Workflow follow-up

- Archive the change after it is pushed, and push again.
- On the phone: check how the paint feels while turning, tune the tile size and smoothing, and read
  `fps` and finder time from the log.
