# Tasks

## 1. Recording format in cube

- [x] 1.1 `ScanRecording`: header, picture lines (`<ms> <FaceCodec>`), `reset` lines, end line; writer with a 90 s ring buffer (`cut=` in the header) and reader
- [x] 1.2 Tests: write/read round trip, cut after 90 s, reset and end lines, malformed line rejected with its line number
- [x] 1.3 Harness: `VideoFixtures.loadRecording`, `RecordingReplay` (`RECORDING=`), timeline printing shared with `RulesTimeline`
- [x] 1.4 Test: an existing fixture replayed through the writer and read back reaches the same known stickers and end as the fixture itself

## 2. Recording in the app

- [x] 2.1 `ScanRecordingStore` (keep 3, newest first; list, clear) with `NoScanRecordings`; phone file store; browser key-value store with a logged, swallowed write failure
- [x] 2.2 Phone: record beside `scan.onFrame` in `VideoScanScreen`; browser: record where the finder's text passes the page (or have the scan worker return it, design 4); resets recorded
- [x] 2.3 Write the recording when the scan finishes, is left, goes to the background or the tab is hidden; log line names the recording
- [x] 2.4 Share and clear: recordings join the log share (phone intent, browser share sheet / zip) and are cleared with the log
- [x] 2.5 Tests: store keeps three and drops the oldest; clear; `VideoScanScreen` writes one recording ending in `left` when the screen is left

## 3. Hide marks setting

- [x] 3.1 `hideScanMarks` setting (DataStore, `StoredSettings`), Settings row in the scan section (fi, en)
- [x] 3.2 `VideoScanScreen` skips the paint layer when on; ring, status line, turn demo and haptics stay
- [x] 3.3 Tests: setting persists; with it on no paint layer is drawn while the ring still shows

## 4. Check, docs

- [x] 4.1 Build both apps and run all unit tests
- [x] 4.2 `docs/development.md`: recording-first flow in "Scan test recordings" (share log → `recordings/` → `RECORDING=`), video path for finder work; `docs/architecture.md` diagnostics map
- [x] 4.3 Roadmap: row 72 `scan-recording` done
