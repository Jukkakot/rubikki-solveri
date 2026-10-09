# Design

## Context

The video scan's logic (`VideoScan.onFrame(faces, nowMillis)`) sees only the faces found in each
picture (`FaceReading`: centre, u, v, nine colours) and the time. `reset()` starts it over. The
camera, exposure and finder sit before that point. Their effect is already in the colours.

- Phone: `VideoScanScreen` calls `scan.onFrame(f.faces, now)` on a background thread.
- Browser: the finder worker sends faces as `FaceCodec` text, and the scan worker decodes it
  (`ScanWorker`, `FaceCodec.decode`).

Fixtures (`cube/src/jvmTest/resources/video/*.txt`) store the same face data per still, with times
assumed at 100 ms per frame.

Shared material today: the log, plus up to 12 scan pictures (`ScanPictureStore`; phone files,
browser key-value store). The browser shares at most ten files, otherwise it downloads a zip.

## Goals / Non-Goals

**Goals:**
- A failing phone or browser scan replays exactly on the JVM from what the user already shares
  (the log share).
- A clean screen recording is possible when a video is still wanted.

**Non-Goals:**
- Recording camera pictures or video in the app (size, privacy, and not needed for the logic).
- Replaying the finder or the exposure control. Their output is what gets recorded.
- Automatic upload anywhere.

## Decisions

1. **Record the scan's input, not pictures.** One line per picture: the time in ms since the scan
   started, then the `FaceCodec` text of the faces. The format already exists and is what the
   browser workers exchange. About 200 characters a picture with two faces, so about 5 KB a second
   at 25 pictures a second and about 450 KB for 90 s. Pictures would be 100× larger and would
   still need the finder to agree.
2. **File format** (text, `cube` module, `ScanRecording`):
   - Line 1: `# scan-recording 1 platform=<android|web> ver=<app version> started=<ISO time> [cut=<ms>]`
   - Then one line per picture: `<ms> <FaceCodec>`, plus `<ms> reset` where the scan restarted.
   - Last line: `# end finished <54-letter cube>` | `# end left` | `# end restart`.

   One writer and one reader in `cube`, used by the app and by the harness.
3. **Buffered in memory, written once.** A ring buffer of the last 90 s (by time) per scan is
   written when the scan ends: finished, left, the screen goes to the background, or the browser
   tab is hidden. There is no per-picture I/O, so the scan is not slowed. The cost: a crash or a
   killed tab loses that scan's recording. The crash log still tells what happened.
4. **Where it is recorded.** On the phone, beside `scan.onFrame` in `VideoScanScreen`. In the
   browser, where the page relays the finder's text to the scan worker. If the pipeline sends
   worker to worker, the scan worker gives the text back with its answer, and the page records
   it. Checked while implementing (task 2.2).
5. **Keep three, newest first.** A `ScanRecordingStore` interface sits beside `ScanPictureStore`.
   - Phone: files `scan-recordings/<started>.txt` in the app's files directory.
   - Browser: one key holding the list (at most about 1.4 MB).

   A store write that fails, such as the browser quota, logs one line and drops the recording.
   It never stops the scan.
6. **Sharing.** Recordings are added to the existing share: phone share intent, browser share
   sheet, or zip download (more than ten files go to the zip already). Clearing the log clears them.
7. **Replay.** `VideoFixtures.loadRecording(name)` reads a recording from
   `src/jvmTest/resources/recordings/` with its real times and resets. `RecordingReplay`
   (env `RECORDING=<name>`, optional `TIMELINE_TRUTH`) prints the same timeline as `RulesTimeline`
   and the end the phone logged.

   `RulesTimeline`'s printing becomes a shared helper. A round-trip test records a replay of an
   existing fixture through the writer, reads it back, and replays it to the same known stickers
   and end, which proves replay is exact. A bug fix later adds the shared recording with a test of
   the right outcome.
8. **Hide marks setting.** `hideScanMarks` (default false) sits in Settings under the scan section,
   stored like the other settings (Android DataStore, browser `StoredSettings`). `VideoScanScreen`
   skips the paint layer (veils, marks, outlines, ticks). The ring, status line, turn demo and
   haptics stay, and the scan runs the same.
9. **Docs.** `docs/development.md` "Scan test recordings" gets the new first path: the user shares
   the log, the recording goes into `recordings/`, and replay runs with `RECORDING=`. The video
   path stays for finder work.

## Risks / Trade-offs

- **Exactness.** The replay matches only if the scan logic is deterministic given faces and times.
  The round-trip test guards that. A log snapshot compared with a replay of the same moment checks
  it on a real recording.
- **Browser storage.** About 1.4 MB more in the key-value store, beside a log capped at 512 KB.
  Should the quota bite on some browser, the failure is logged and the newest recording is dropped.
- **Privacy.** Recordings hold only numbers about the cube, no picture. They leave the device only
  on share, like the log.

## Implementation notes

- **`t0` in the header.** The scan's decisions use some absolute times (timers starting at 0), so a
  recording keeps the scan clock of its first picture (`t0=`); lines stay ms from that picture and a
  replay gives the scan `t0 + ms`.
- **The scan gets the recorded faces.** Coordinates are kept to 1/1000 px (`FaceCodec`). The phone
  and the browser's finder worker now give the scan the faces read back from that text, so the
  replay sees exactly the same numbers (a change below a thousandth of a pixel).
- **A restart ends a recording** (`# end restart`) and the next pictures start a new one, so a failed
  scan is not pushed out of its 90 s by the retry. `reset` lines mark a fresh scan the page did not
  start: in the browser the scan worker's answer now also carries the time it gave the scan and how
  many pictures its scan has had; a 1 after earlier pictures (the pipeline taking over) is a reset.
- **Background / hidden tab:** `ON_STOP` of the screen's lifecycle (Compose Multiplatform maps the
  tab's visibility to it) writes the recording ending `left` but keeps it open; a later end writes it
  again under the same name, which replaces it.
- **Where the files go:** phone `files/logs/recordings/scan-<time>.txt` (under `logs/` so the log's
  FileProvider shares them), written on a background thread. Browser: straight in localStorage, not
  through the memory fallback, so a full quota drops the recording without switching the whole app
  to memory.
- **Browser share:** the log, the recordings, then the newest pictures that still fit in ten files;
  the zip has everything.
- **Hidden marks:** the paint layer is left out; in the browser an empty canvas still puts the read
  picture on screen. The spinner shown before the first sticker stays (a status sign, not a mark).
- **Settings:** a new section "Skannaus" / "Scanning" holds the switch.
