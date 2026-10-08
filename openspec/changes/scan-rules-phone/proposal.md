# Proposal

## Why

The first phone test of the rules scanner (`scan-rules`; web, Samsung Browser, 2026-10-08 08:45,
the striped cube in daylight) went badly (user: "todella huonosti"). The log shows, for the plain
striped cube, orange stickers read as red and shown as known red on the front and back sides, the
orange face never recognised, and the scan stuck with "Värit eivät täsmää" for 40 s; on a turned
cube right after it, the same. "Käännä kuutiota" showed right at the start. One scan in between
(05:39) finished right. These break the spec as it stands ("never show a sticker known that a
possible cube could still have otherwise", finish when one cube is clear, the hint only while two
faces could be either way), so no spec changes: the code is made to meet it.

## What Changes

- A fixture from the user's screen recording (`web_084657`; the scan parts only, not the home
  screen's spinning cube), with the plain striped cube as its truth for the first scan.
- Red and orange told apart as the earlier scanner did on this recording: the stickers are named and
  shown so that no orange sticker is shown red (or the other way) as known; the orange face is
  recognised and the scan finishes.
- The turn-the-cube hint only once the cube is otherwise nearly read and two faces could be either
  way, not while a first face is still being followed.
- Whatever else the fixture shows as the cause of the stall, fixed within the bar: never a wrong
  cube on any fixture, the confirmed eleven as fast as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None (the `video-scan` spec already says what should happen; this change makes the rules scanner
meet it).

## Impact

`cube/scan/FaceTracks.kt`, `VideoScan.rulesFrame` (names, display, hint); a new fixture and the
acceptance harness; possibly `ScanAcceptanceHarness` bar notes. No UI or settings change.
