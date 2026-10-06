# Tasks

## 1. Guided scan

- [ ] 1.1 Move the video scan's overlay row into a shared `ScanOverlayBar`:
  - back, a middle slot, torch, ⋮ menu;
  - the video scan uses it with the ring.

  Verify that the `VideoScanScreenTest` still passes.
- [ ] 1.2 Rebuild `ScanScreen` on it:
  - The camera fills the screen and the face marks sit in the middle slot.
  - The menu holds "Videolla" and "Syötä käsin" (single-face rescan: no menu).
  - No title and no "Kuvattu n/6".
  - The "any face" request only while no face is done.
  - The shutter with a ↶ redo icon at the bottom.

  Verify with `ScanScreenTest`:
  - The menu switches to the video scan.
  - The request is gone after the first accepted face.
  - No "Kuvattu" text.

## 2. Colour check and manual input

- [ ] 2.1 Replace "Edellinen / Seuraava / Tarkista" with ‹ › icon buttons and a ✓ main button, keeping
  the names as descriptions. Verify with the manual-input tests, which find them by description.
- [ ] 2.2 Show the check's instruction as one line until the first paint, "looks right" or "scan
  again", and remove the "faces left" line. Verify with a test: the line is shown on opening and
  gone after one fix.

## 3. Free cube

- [ ] 3.1 Layer buttons with a small cube picture (layer highlighted, arrow following the ↻/↺ toggle),
  and icon buttons for scramble, undo and reset. Verify with Compose tests:
  - "Oikea puoli" turns the right layer.
  - The toggle reverses it.
  - Undo takes it back.
- [ ] 3.2 Show the drag hint until the first drag. Verify with a test: the hint is gone after a swipe
  on the cube.

## 4. Timer

- [ ] 4.1 Show the hold-to-start instruction only while no timed solve is saved. Verify with a test:
  with one saved solve the timer area shows only the time.

## 5. Finish

- [ ] 5.1 Render the screenshot tests again and look at the scan, check, manual input, free cube and
  timer shots. Run `./gradlew test lint assembleDebug` and `:web:wasmJsBrowserDistribution` and
  verify that they pass.
- [ ] 5.2 Update `docs/architecture.md` (screens table) and the roadmap. Verify by reading the
  changed rows once.

## Workflow follow-up

- Archive after the push, and push again.
