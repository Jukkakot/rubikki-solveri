# Proposal

## Why

After an unsure scan the check page shows the whole cube with some stickers marked, and the user is
left to work out where the mistake is. When the cube cannot be right (the check says "a corner is
twisted"), the message names a cube-theory problem, not a place to look, and the only way out is to
scan all six faces again. Gallery feedback 8 asks for a check that goes face by face, lets a face be
fixed or rescanned on its own, and says plainly which faces to suspect.

## What Changes

- The check after a scan walks the faces one at a time. Each face shows the camera picture and the
  colours, with "Looks right" (go on), a sticker fix (palette + tap, as now) and "Scan this face
  again". Faces without any marked sticker start as checked; the walk goes through the rest.
- Rescanning one face opens the scan for just that face and returns to the check with its new
  colours; the other faces and the user's fixes stay as they are.
- When every face is checked the cube is checked automatically. A solvable cube opens the solution.
  One that cannot be right gets a plain message ("Some sticker was read wrong") and the faces to
  look at again; the most likely wrong stickers are marked, and those faces go back to unchecked.
- The cube module learns to find the likely misreads: which single swap of two stickers makes the
  cube solvable, ranked by how close the camera readings were.
- Manual input (not from a scan) is unchanged.

Modules: **cube** (likely-misread search, one-face classification against the cube's own colours)
and **app** (face-by-face check, one-face scan, navigation between them).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `manual-input`: "Check a scan" becomes a face-by-face check with per-face confirm, rescan of one
  face and a plain verdict with the faces to suspect.
- `camera-scan`: a scan of a single face, started from the check, that returns to it.

## Impact

- cube: new misread search next to `CubeCheck`; `ColorClassifier` gets a one-face classification;
  the scan's raw readings are kept in the outcome.
- app: `ManualInputScreen` check mode, `ScanScreen` one-face mode, `RubikkiNavHost` routes; the
  last scan's readings kept in memory next to its pictures; new strings (fi/en); screenshot tests
  for the new check states and the gallery refreshed.
- No new dependencies.
