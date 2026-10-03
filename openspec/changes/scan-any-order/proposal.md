# Proposal

## Why

The scan asks for the faces in a fixed order and each face held one exact way ("green centre
towards you, white on top"). A face shown in another order or turned a quarter is stored as the
asked face as it is, so the cube comes out wrong — likely why the user's first phone try did not
give a right solution. The user wants to show the faces in any order and any rotation and let the
app work it out, on the assumption that the real cube is always a valid one, so anything that does
not fit is a reading error.

## What Changes

- Any not-yet-scanned face can be shown, turned any way. The centre tells which face it is: the
  review says "Recognised: Right face" and the user can change it with a tap on another face's
  colour before going on. The fixed order stays as a suggestion ("Next, for example: right face").
- Showing a face that is already scanned (in any rotation) asks to turn to another face, as the
  "previous face still in view" check does now.
- After the six faces, the app finds how each face was turned: it tries the 4⁶ = 4096 rotations
  and keeps the one that makes a solvable cube. If none does (a misread sticker), it keeps the one
  with the most real pieces and the check takes over. If the centres' colours were mixed up
  between two opposite faces (red/orange, white/yellow), swapping them is tried too. If two
  different rotations both give a solvable cube, the faces in doubt are marked for the check.
- The check's camera pictures are turned the same way as the colours, so they match.
- A one-face rescan from the check also accepts the face in any rotation.

Modules: **cube** (face recognition by centre, rotation search, session without fixed order) and
**app** (scan screen texts and review, picture turning).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `camera-scan`: guided scan, live reading, capture, confirm each face, one-face scan and result
  change from a fixed order and hold to any order and rotation.
- `manual-input`: the check's camera pictures are shown turned to match the colours.

## Impact

- cube: `ScanSession` (faces by centre, captured per face, recognition, already-scanned check),
  new `RotationSearch`, `ScanOutcome` gains the found rotations; `ScanCheck.replaceFace` tries the
  four rotations; tests incl. the phone regression with turned faces.
- app: `ScanContent` (title/hint, recognised face and the face picker in the review, progress by
  face), picture turning in `LastScan`, strings fi/en, Compose and screenshot tests, gallery.
- Roadmap: inserted before `about-log-polish` (user decision 2026-10-03).
