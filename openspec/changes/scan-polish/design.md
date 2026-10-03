## Context

See proposal.md. Phone log 2026-10-03 08:16–08:17: `scan.lock` came 80 ms after the front face's
`scan.face` (accept); the front picture is normally exposed, all later pictures are blown out.
`ScanContent` locks with `LaunchedEffect(index > 0)`. On capture, `handle()` saves the PNG on the
main thread. `FrameSampler.gapContrast` boxes ~11 k doubles per frame on the camera thread.
`ManualInputScreen` is one scrolling column (note card, map + 3D, title, hint, big grid, palette,
buttons, check, result); in the screenshot of the check, the palette is cut off and the check
button is below the fold.

## Goals / Non-Goals

**Goals:** right exposure for the whole scan; no main-thread work at capture and data to find any
remaining stutter; a check page a first-time user understands and that fits one screen.

**Non-Goals:** guessing which stickers are wrong beyond the classifier's marks; a new solver of
invalid cubes; changes to the 3D view.

## Decisions

- **Lock on capture.** `locked = index > 0 || review != null` drives `onLockExposure`. At capture
  the user has held the face still for 1.5 s, so the camera has settled on the cube. "Scan again"
  on the front face (review cleared, index 0) unlocks; redo back to the front unlocks too.
  Alternative: never lock — rejected for now: the first good scan was made with a lock, and
  unlocked auto exposure changes the brightness between faces.
- **Stutter.** (1) the PNG is written on `Dispatchers.IO`; its name is decided at once so the log
  line still has it. (2) `gapContrast` uses primitive arrays and a 256-entry sRGB→linear table
  (lightness only), no `Lab` objects. (3) Stall logging: the camera analyzer logs
  `scan.stall where=camera ms=…` when frames are ≥ 300 ms apart; a `withFrameNanos` loop on the
  scan screen logs `where=ui` for frames ≥ 150 ms apart. If the user still sees a freeze, the log
  says which side it is on.
- **Pictures to the check.** `ScanScreen` keeps the latest captured picture per face (in memory,
  retakes overwrite) and hands them to a small app-wide holder `LastScanPictures` when the result
  is shown; the check route reads it when `fromScan`. After process death the pictures are simply
  not shown.
- **Manual input layout (both modes).** No scroll. Top: a row with the face map (smaller cells) and
  the 3D preview at a fixed height (~140 dp). Then the face title and the one-line hold hint. The
  middle takes the remaining height: the editable 3×3 grid sized to fit; in check mode the camera
  picture ("Kamera näki") and the grid ("Värit") side by side, each half the width. Bottom bar:
  the palette (six 44 dp swatches with counts) and a row "‹" / "›" / **Tarkista** (primary). The
  result of a check is a short line above the palette.
- **Check-mode text.** Title stays "Tarkista värit". The long note card becomes one line under the
  top bar: "Vertaa kameran kuvaan. Valitse väri alta ja napauta väärää tarraa. Punareunaiset ovat
  epävarmoja." and a "Skannaa uudelleen" text button in the top bar.

## Risks / Trade-offs

- [Small phones: the grid gets small] → the grid takes all remaining height and at least 44 dp per
  sticker; the 3D preview is the first to shrink.
- [The picture's orientation differs from the grid] → both use the holding position of the face
  (the scan asks for that exact holding), so they match; checked in the screenshot.
- [Lock at capture but the user scans again] → unlock on retake of the front, as above.
