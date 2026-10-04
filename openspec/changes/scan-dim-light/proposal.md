# Proposal

## Why

In dim, warm evening light the scan fails although every colour is plain to the eye (user's log,
2026-10-04 21:30): the first face's dark blue centre (`072641`) read closer to the daylight white
than to the daylight blue, so the blue face was taken for the top (white); from then on white and
blue were swapped and the result was an impossible cube. The user wants the reading to be
forgiving: it does not have to see exactly the expected blue, only six different colours, nine of
each.

## What Changes

- **Bigger reading area per cell:** each cell is read from about 60 % of the cell (40 % today) as a
  robust average (the middle half of the values per channel), so a highlight or a dark corner
  weighs less.
- **Brightness-independent face recognition:** while scanning, the centre is compared with the
  references after scaling out its brightness, so a dark colour is no longer taken for white.
- **Centres named together at the end:** after the sixth face the six centres are matched to the
  six colours as one best assignment, and faces are renamed silently when that differs from what
  was recognised live. The 54 readings are then grouped nine per colour by the cube's own centres,
  as today. If the cube is still impossible, the next-best namings of the centres are tried and the
  first solvable one is used.
- **Check after every scan:** the colour check (the camera's pictures next to the colours as read)
  opens after every scan. A confident one continues to the solution by itself after a few seconds,
  with a button at hand to scan again if the colours do not match the pictures; an unsure one waits
  with the problem stickers marked, as today.
- **No face names while scanning:** the live "centre looks like X", the review's "Recognised: X"
  and its choice of another face go away; the names are decided at the end.
- **Real data in the tests:** the scans of the user's log of 2026-10-04 (daylight and the failed
  evening scan) are kept as test data (`evidence/`).
- Not in this change: the browser crash after the tab was in the background for minutes (goes to
  the backlog).

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `camera-scan`: Live reading and Confirm each face (no face names while scanning), Classification
  (bigger averaged cell area), Result (centres named jointly, next-best namings, the check always
  opens and continues by itself when confident), Guided scan and Scan look (done marks show the
  centre as seen).

## Impact

- `cube` module: `scan/FrameSampler` (cell area, trimmed mean), `scan/ColorClassifier` (normalised
  comparison, joint centre naming), `scan/ScanSession` (live recognition, `outcome` renames faces
  and tries next-best namings), `scan/RotationSearch` (called per naming), tests with the log's
  readings.
- `shared` module: the scan flow's navigation after `scan.done` (always to the check), the check's
  note for a confident scan (fi/en string).
- `app` module: none beyond tests (screens are in `shared`).
