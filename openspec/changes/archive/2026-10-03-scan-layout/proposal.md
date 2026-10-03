## Why

On the phone the scan screen is taller than the display: during the review the user has to scroll
down to find "Good, next" / "Scan again", and the long review text sits between the camera and the
buttons. The scan is done with one hand holding the cube, so the actions must be visible at once.

## What Changes

- The scan screen fits the display without scrolling: the action buttons are pinned at the bottom,
  the camera takes the remaining height.
- The status line (hold still, centre hint, turn the cube) and the hold progress move onto the
  bottom edge of the camera view.
- The review text is shortened to one line on the dimmed camera view, with the "colours are worked
  out at the end" note as a small second line; the centre note stays there too.

Modules: `app` only (scan screen, strings).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `camera-scan`: new requirement that the scan fits one screen with the actions always visible.

## Impact

- `app/ui/scan/ScanScreen`: layout (no vertical scroll, bottom action bar, overlays on the camera).
- Strings fi/en (`scan_review` shorter, new `scan_review_note`), scan tests and the review screenshot.
