# Tasks

## 1. Read stickers and read sides in the scanner (cube)

- [x] 1.1 `FaceTracks`/`VideoScan` (rules engine): each `FoundFace` carries `read` (its track's leading colours in reading order once the track counts, else null); the look engine sets `read` = `known`. Test in `RulesVideoScanTest`: on today's striped-cube fixture a face held for a few frames has `read` colours while its track is still open
- [x] 1.2 `VideoScanState.readSides`: centre colours of every track that has counted in this scan, kept after the track ends, cleared by `reset()`. Test: the fixture's scan reaches all six before `complete`, and a reset empties it

## 2. Paint, ring and status line (shared)

- [x] 2.1 `ScanPaint.of`: found faces mark `known` (filled dot, `sure`) else `read` (hollow ring), veil only when neither; projection sides likewise from `stickers` / `leading`; drawing of the ring mark. Tests in `ScanPaintTest`/`RulesScanPaintTest`: a read-but-open face has rings and no veils; a known colour wins over a different read one and is sure
- [x] 2.2 Progress ring as six segments (faint / lit / full, W R G Y O B), accessibility text naming the unread sides; full in every segment when complete. Smoke test in `VideoScanScreenTest`: renders with a state of five read sides
- [x] 2.3 Status line: `video_status_turn` also when all six sides are read and the cube is not complete; order done > no cube > turn > grey. Unit test on `videoStatus`
- [x] 2.4 Vibration when `readSides` grows, within the existing `BUZZ_MILLIS` spacing. Covered by a small unit test of the buzz decision (pure function)
- [x] 2.5 Turn demo cube: a pure `turnDemo(state, sinceProgress)` (cube module or shared logic) giving null / (start orientation, end orientation): unread side with known pose = real orientation to that side facing the camera by the shortest whole-cube turn; all read or no pose = face-on to corner view; same result while the side to show stays. JVM tests for the three cases. Then the small `Cube3D` (~64 dp, grey stickers, coloured centres, looping ~2.5 s) by the status line, shown after 2 s without progress and hidden on progress; smoke test in `VideoScanScreenTest`

## 3. Browser: the read picture with its marks (web)

- [ ] 3.1 `platform.mjs`: with the worker, keep a display-size `ImageBitmap` of each frame sent (sequence number, at most two alive, older closed); `cameraShowFrame(seq)` draws it on a display canvas in the video's place and hides the video; back to the live video when the scan stops or the worker is not used. Verify: `npm`-free web build (`./gradlew :web:wasmJsBrowserDistribution`) and the smoke test pass
- [ ] 3.2 `WebCamera.kt` / `VideoScanScreen`: pass the sequence number with the worker's faces, call `cameraShowFrame` from the Compose draw that first draws that reading's paint; with the read picture on, glide snaps and the motion fade stays shown (the age fade stays). Log `showMs` (bitmap time per picture) in the snapshot line; `VideoScanLogTest` covers the field

## 4. Wrap-up

- [ ] 4.1 Build and checks: `./gradlew check` (JVM tests, lint) and the web build + smoke test green
- [ ] 4.2 Docs: `docs/architecture.md` video-scan paint/ring and the browser's read-picture path (where it lives, why); roadmap: add `scan-feedback` as done with a one-line summary
- [ ] 4.3 For the user to try in the browser: does a face "take" within about half a second, does the ring tell the missing side, does the small turn cube appear when stuck, and does its movement make sense, do the hollow rings fill to dots, does the phone buzz on a new side, do the marks stay on a quickly turned cube, and how the ~17 fps picture feels
