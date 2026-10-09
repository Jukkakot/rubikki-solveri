# Proposal

## Why

The user asked how the scan could say more clearly what to show next. Today the scan is well guided
until every side has been read, through the faint ring segment and the small cube turning the unread
side forward. After that the line only says "Käännä kuutiota" and the small cube tilts to some corner.
The scan, though, knows exactly which stickers are still unknown and which faces' turns are in
doubt. In the 2026-10-09 logs every side was read within 2–4 s, and then the user waited a further
1–4 s "hoping it finishes soon". The user reviewed mockups (artifact "Skannausvinkit", options A–D,
and "Kääntönuolet") and decided against arrows for now.

## What Changes

- The scan picks the next view: the corner (three sides) or the single side whose showing would
  settle the most. That means unknown stickers, stickers in doubt, and faces whose turn is still
  open. The pick is held steady and changes only when another view becomes clearly better.
- **Colour corner on the status line:** once every side has been read, the line names that view by
  its colours, as two or three pulsing colour dots ("Näytä kulma" plus dots, or "Näytä sivu" plus one
  dot). The same segments pulse in the ring.
- **The small cube shows the target:** after about 2 s without progress, the small cube starts as
  the cube is held and turns that view to the camera. Its known stickers are in their colours,
  unknown ones grey, and the stickers still needed blink. Before every side has been read it works as
  today (the unread side forward).
- **The ring fills sticker by sticker:** each segment fills by the share of its side's stickers that
  are known, so progress shows all the time. It is full when the side is confirmed, as today.
- No arrows on the real cube.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: "Progress ring" (filling by known stickers, pulsing segments), "One status line"
  (colour corner once every side is read), "Turn shown on a small cube" (target view with blinking
  needed stickers).

## Impact

- `cube`: the next-view choice is a pure function of the scan state (`VideoScan`/`FaceTracks`
  state: known stickers, doubtful ones, open turns, how the cube is held), with tests.
- `shared`: `ScanOverlayBar`/`VideoScanScreen` (ring fill and pulse, status line with colour dots),
  `TurnDemo` (target view, coloured known stickers, blinking needed ones), strings (fi, en).
- In the browser the state comes from the worker, so the next view goes into the state text as well.
