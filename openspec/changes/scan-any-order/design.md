# Design

## Context

See proposal.md. `ScanSession` walks `FaceView.entries` by `index`, stores `captured[index]` and
compares the frame with the previous face only. `outcome()` classifies the 54 readings (balanced,
seeded by the centres) and checks validity. The readings of a face are in net order as seen when
the face is held the way `FaceView` describes. The check (`ScanCheck`) and the one-face rescan
(`classifyFace`) assume that order too. Pictures are 120×120 ARGB as seen.

## Goals / Non-Goals

**Goals:** any order, any rotation, the right cube out; the user confirms which face each capture
is; no suggested order (user decision 2026-10-03: just show the six faces).

**Non-Goals:** video / continuous tracking (noted as a later idea); mirrored views (a camera never
mirrors a real cube); deducing the sixth face from five (later idea).

## Decisions

- **A face by its centre, confirmed by the user.** Live: the centre is read against the references
  (accepted centres + default palette), restricted to the colours of faces not yet scanned; the
  event carries `recognised: FaceView`. At capture the review shows "Tunnistettu: Oikea puoli" and
  a row of the not-yet-scanned faces' centre colours (44 dp circles, the recognised one selected);
  a tap changes it. `accept(face)` stores the readings under that face. Alternative: no
  confirmation, deduce all labels at the end — rejected: the user asked for the confirmation, and
  it keeps the live references (the cube's own red) right from the second face on.
- **Session state.** `captured: Map<FaceView, List<Rgb>>` plus an accept order (stack) for redo;
  `index` = number done; `isDone` = six done (one-face
  mode: that face done). "Already scanned" = the frame is alike (ΔE 12 per cell) to any accepted
  face in any of its four rotations → `ScanEvent.AlreadyScanned` (replaces `PreviousFace`; text
  "Tämä puoli on jo kuvattu – käännä kuutiota toiseen puoleen.").
- **Rotation search (cube module, `RotationSearch`).** Readings stay as seen; a rotation k (quarter
  turns clockwise as seen) maps net position n to its place. `outcome()`: classify the 54 as now
  (rotation does not matter to the balanced classification), then for the 4⁶ combinations rotate
  the colours of each face and run `CubeCheck.validity`; collect the valid ones. One distinct valid
  cube → use it. Several distinct valid cubes → use the one with the fewest quarter turns (most
  likely as held, since people tend to hold a face upright) and mark every non-centre sticker of the faces whose rotation differs
  between them. None → score = number of the 20 piece places whose colours form a real piece
  (`CubeCheck.pieces`-style reading per place); best score, ties by fewest turns. Then, only when
  none was valid, the same with the colours of one opposite pair of centres renamed (red↔orange,
  white↔yellow, green↔blue); a valid result there wins. *Changed in implementation:* only one pair
  at a time, not the 7 combinations. Naming one pair the wrong way round mirrors the cube; renaming
  any one pair undoes a mirror (two mirrors are a whole-cube turn), so all three single pairs give
  a solvable but only one the right cube, and two pairs never fix anything. The pair is chosen by
  the readings: the one whose two colours' readings fit the default palette better with the names
  swapped (without readings: red/orange, white/yellow, green/blue). Cost: up to 4 × 4096 checks,
  measured ≈ 0.35 s on the desktop JVM, on `Dispatchers.Default`; the common case stops after the
  first 4096 (≈ 0.1 s).
- **What the outcome carries.** `ScanOutcome.samples` become the readings turned into net order (so
  the check and `classifyFace` keep working unchanged) and `rotations: Map<Face, Int>`; the app
  turns each face's picture by the same k before handing it to `LastScan`.
- **One-face rescan.** `ScanCheck.replaceFace` classifies the face in each of its four rotations and
  keeps the one that gives a valid cube with the rest, else the one with the most real pieces,
  ties by the lowest classification distance; it returns the rotation so the picture can turn too.
- **Exposure lock.** `locked = index > 0 || review != null` keeps its meaning (index = faces done).
- **Texts.** Title "Kuvattu %d/6" while scanning; the hint line "Näytä mikä tahansa kuvaamaton
  puoli, missä asennossa tahansa."; live status "Keskiö näyttää: %s" (face name). The hold hints
  ("vihreä keskiö sinua kohti…") are no longer shown in the scan. One-face mode: title the face
  name, hint "Näytä %1$s (%2$s keskiö) missä asennossa tahansa." (face, centre colour).
- **Picture turning.** `rotatePicture(argb, size, k)` in the app (plain index math on the IntArray).

## Risks / Trade-offs

- A user who mislabels a face in the review and two faces get swapped labels that are not opposite
  → no valid rotation; the check names faces. Acceptable: the review shows the colours big.
- Symmetric patterns can have several valid rotations → marked faces, the user compares with the
  turned pictures.
- Warm red reads orange until red is known → the review shows "Vasen puoli"; the user taps red.
  The opposite-pair rename at the end also catches a wrong confirmation of exactly this kind.
