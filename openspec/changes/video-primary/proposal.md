# Proposal

## Why

The video scan now works well on the phone (user, 2026-10-06), so it should be the way scanning
starts. The extra home tile "Skannaa videolta (kokeilu)" also takes so much room that the browser's
home screen has hardly any space left for the cube.

## What Changes

- Home: the big "Skannaa kuutio" button opens the video scan and shows the video camera icon. The video tile goes away, so the
  home screen is back to a 2×2 grid (manual input, learn, timer, free cube). "(kokeilu)" disappears.
- Video scan screen: a small "Kuva kerrallaan" text button in the top bar beside the torch opens the guided scan (one face at a time) in its
  place, so going back from it returns home, not to the video scan.
- Guided scan screen: a "Videolla" button switches back to the video scan the same way.
- Every "scan" entry that starts a whole new scan (home, the manual input screen's scan link) opens
  the video scan. Rescanning one face from the colour check stays in the guided scan (it scans just
  that face).
- The video scan's title becomes "Skannaa" / "Scan" (was "Videoskannaus"), so it fits the top bar
  beside "Kuva kerrallaan" and the torch (decided while implementing).
- No remembered choice: scanning always starts as video (user, 2026-10-06).
- Modules: shared (home, both scan screens, navigation, texts). No change to cube or app.

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: "Guided scan stays" now says the video scan is the default and the guided scan is
  one tap away from it.
- `app-shell`: "Home screen layout" scan opens the video scan; the video tile is gone.

## Impact

`shared`: `ui/nav/RubikkiNavHost.kt` (home entries, scan routes), `ui/scan/VideoScanScreen.kt` and
`ui/scan/ScanScreen.kt` (switch buttons), strings (fi/en), home and scan screenshot/Compose tests.
