# Proposal

## Why

`share-log-fix` made the share button download the log in Samsung Internet, but the scan pictures
were lost: the log said `NotAllowedError: Must be handling a user gesture` for the retry with the
log alone (the first share used up the tap), and the first refusal was not logged. Likely root
cause of the first refusal: Chromium (Chrome, Samsung Internet) shares at most 10 files at once, and
the app sent 13 (the log and 12 pictures). The user wants the pictures with the log for debugging.

## What Changes

- The share sheet gets the log and the newest 9 pictures (10 files, Chromium's limit).
- When the share sheet is not available or the browser refuses the share (not the user cancelling),
  one zip file is downloaded: the log and the stored scan pictures. The log-only share retry is
  dropped (it cannot work without a new tap).
- The first refusal is logged (`log.shared outcome=downloaded error=…`).
- The zip is made with `fflate` (npm, small, established); stored, not compressed (PNGs are
  already compressed).

Modules: `web` only.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `web-app`: the fallback download is a zip with the log and the pictures.

## Impact

`web/src/wasmJsMain/resources/platform.mjs`, `web/build.gradle.kts` (npm `fflate`).
