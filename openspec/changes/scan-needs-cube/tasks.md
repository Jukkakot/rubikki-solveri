# Tasks

## 1. Sticker check (`cube`)

- [x] 1.1 `FrameSampler.stickerCells(picture)` and `looksLikeCube` = all nine cells + the gap check (design §1)
- [x] 1.2 Tests: cells built from the logged readings (2026-10-04) — real faces (daylight and evening whites, dark reds) pass; room greys/beiges, black, yellow-middle-with-patterned-neighbours, a cell straddling a gap fail; a 120×120 picture is checked in under 2 ms. Existing scan tests stay green

## 2. Scan screen (`shared`)

- [x] 2.1 Keep `stickerCells` with the latest picture; no picture yet = not a cube; green cell outlines; reworded `scan_status_no_cube` in both languages (design §2)
- [x] 2.2 One Compose test: a picture with one failing cell shows the bring-the-cube text and no hold progress

## 3. Camera follow

- [x] 3.1 Pictures to the follow panel; frames without a face skip `FollowTracker`; `follow_bring_cube` in both languages (design §3)
- [x] 3.2 Test: a frame without a face does not advance or learn (FollowTracker untouched), a face frame advances as before (`FollowTest` stays green)

## 4. Check and docs

- [x] 4.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution` and the smoke test; one screenshot of the scan grid with mixed green/white outlines
- [x] 4.2 `docs/architecture.md` Camera scan step 3 (the sticker check) and Camera follow (ignored frames)
