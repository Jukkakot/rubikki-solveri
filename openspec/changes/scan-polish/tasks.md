## 1. Cube module

- [ ] 1.1 `FrameSampler.gapContrast` without boxing (primitive arrays, sRGB→linear table, lightness only); the phone-picture tests still pass with the same results (`./gradlew :cube:test`)

## 2. Scan screen

- [ ] 2.1 Exposure lock on the first capture (`index > 0 || review != null`), released by scanning the front again; Compose test: capturing the front calls the lock, "Scan again" releases it (`:app:testDebugUnitTest`)
- [ ] 2.2 Picture written on a background dispatcher; stall logging (`scan.stall where=camera|ui ms=…`, new Evt); verify build and that a capture still logs the picture name in the Compose test
- [ ] 2.3 Keep the latest picture per face and hand them to `LastScanPictures` with the result; verify by a unit test of the holder and a build

## 3. Manual input / check

- [ ] 3.1 One-screen layout (top map + 3D, title + hint, grid filling the middle, bottom bar with palette, ‹ › and Tarkista, result line); existing manual-input tests pass; screenshot `manual-input` shows the palette and the check button
- [ ] 3.2 Check mode: one-line instruction, camera picture beside the grid, "Skannaa uudelleen" in the top bar (navigates to the scan); strings fi/en; screenshot `scan-check` with a picture; Compose test: scan again calls back (`:app:testDebugUnitTest`, lint)

## 4. Docs

- [ ] 4.1 `docs/architecture.md` (lock timing, stall logging, check page), `docs/operations.md` (`scan.stall`), roadmap row 17 `scan-polish` done; verify by reading
