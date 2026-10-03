## Context

`ScanContent` is one scrolling column: title, hold hint, a 3:4 camera box, progress bar, status
text, review notes, "X read", face progress, buttons, manual link. On a phone that is taller than
the screen.

## Goals / Non-Goals

**Goals:** no scrolling in portrait; actions where the thumb is; texts on the camera.

**Non-Goals:** a landscape layout of its own (it shrinks the camera; acceptable for now); changes to
the scan logic.

## Decisions

- **Scaffold `bottomBar` for the actions.** Button row plus the manual-input link in a small row
  under it. Always visible, no scroll container at all.
- **Camera takes the remaining height.** The content column holds the title row (face title + face
  progress dots) and the hold hint, then a `weight(1f)` area with the 3:4 camera box centred in it
  (aspect ratio matched to height first), so the grid geometry the sampler uses stays the same.
- **Overlays.** At the bottom of the camera box: a translucent dark panel with the status line and
  the hold progress bar while scanning. During the review the dimmed overlay already covers the
  camera; the short review line, the small "worked out at the end" note and the centre note go
  under the tiles inside it. "Front read." moves to the top-left of the camera box as a small chip.
- **Review text split.** `scan_review` = "This is how the camera saw this side." and
  `scan_review_note` = "The colours are worked out at the end from the whole cube." The "check
  doubtful ones then" part is dropped: the manual editor already says so when it opens.
- **Overlay text** is white on a black scrim in both themes (camera content is unpredictable).

## Risks / Trade-offs

- [Short screens or landscape: title + hint + bar leave little for the camera] → the camera box
  shrinks but everything stays reachable; tune if seen on the phone.
- [Text on the camera is harder to read in bright scenes] → ~60 % black scrim under the text.
