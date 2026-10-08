# Tasks

## 1. The read picture on the phone (shared)

- [ ] 1.1 `FoundFaces` gets `image: ImageBitmap?` (the picture read, upright, the size the faces' coordinates are in or a multiple of it); read-picture mode in `VideoScanContent`/`PaintLayer` = `image != null || show != null` (snap, no motion fade, `holdPicture` for both). `VideoScanContent` draws the shown picture's `image` filling the box under the paint, from the same `painted` pair, so picture and marks change in one frame
- [ ] 1.2 `CameraPreview.android.kt`: for the video scan only (`onImage` set), each analysis frame also becomes an upright `Bitmap` (copy of the RGBA buffer, cropped to `cropRect` and turned by `rotationDegrees` with a `Matrix`), its time logged per picture as `showMs`; it travels with the `ArgbImage` to the finder and comes back in `FoundFaces.image`. Analysis stays 640×480. `PreviewView` stays bound beneath
- [ ] 1.3 Tests: `holdPicture` with an `image` instead of `show` in `ScanFeedbackTest`; `VideoScanScreenTest` smoke test that a scan fed `FoundFaces` with an `image` renders and finishes as before

## 2. Cheaper browser copy (web)

- [ ] 2.0 `platform.mjs`: the read picture's copy drawn with `drawImage(video, crop → canvas)` into one of three reused canvases (box-sized, at most 720 px long, placed where the box is) when the picture is sent; `cameraShowFrame(seq)` makes that canvas the visible one and hides the others and the video; a canvas is reused only when it is neither shown nor waiting for its faces (else no copy for that picture: it then shows nothing new). `showMs` = the draw's own time. No `createImageBitmap` for the copy (the worker's own bitmap stays). Verify with the web build and `web/smoke/video.mjs`

## 3. Wrap-up

- [ ] 3.1 `./gradlew check` (JVM tests, lint), the web build and both browser smoke tests
- [ ] 3.2 Docs: `docs/architecture.md` read-picture paragraph (now both platforms; where the bitmap is made); roadmap entry
- [ ] 3.3 Install on the phone (`adb install -r`, the user asked for installs this session) and list for the user to try: marks stay on a quickly turned cube, the picture's sharpness and smoothness, fps and `showMs` in the log (phone app and browser)
