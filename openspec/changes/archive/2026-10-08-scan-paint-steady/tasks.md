# Tasks

## 1. Sure tilt (cube)

- [x] 1.1 `Orientation.choose` returns how it chose (only one answer, a cue from another face, the previous one, a guess); `VideoScan` keeps whether its last orientation was sure and builds the projection and sets `state.orientation` only from a sure tilt (sure = one answer or a cue, or the previous when that was sure and had a pose within `HOLD_MILLIS`). The held projection keeps working as now. Tests in `VideoScanTest` (both scanners): one slanted face alone gives no projection and no orientation; with a second known face in the picture it does; after that, the slanted face alone keeps it

## 2. Paint and picture (shared, web)

- [x] 2.1 `ScanPaint.of`: a projection side whose centre lies within about one sticker step of a found face's centre is not drawn (named or not). Test in `ScanPaintTest`: an open found face over a projected side gives only the face's own marks
- [x] 2.2 `VideoScanContent`: in read-picture mode (`FoundFaces.show`), a reading with no face keeps the shown picture and its paint (no `show` call) while the last picture with a face is under `HOLD_PICTURE_MILLIS` (300) old; the scan still takes the reading. Smoke test in `VideoScanScreenTest`: with a `show` callback, a faceless picture right after one with a face does not call `show`, one after 300 ms does
- [x] 2.3 `platform.mjs`: the read picture's copy capped at 720 px on its long side

## 3. Wrap-up

- [x] 3.1 `./gradlew check`, web build and both browser smoke tests
- [x] 3.2 Docs: `docs/architecture.md` read-picture paragraph (hold, 720 px) and the projection's sure tilt in one line; roadmap entry
- [x] 3.3 For the user to try in the browser: no grey ghost side on the table when the cube is taken up, no striped double veils on a face, the marks no longer blink out, and whether the scan runs at about 15–17 pictures a second again (`fps`, `showMs` in the log)
