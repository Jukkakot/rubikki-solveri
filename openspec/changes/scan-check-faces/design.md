# Design

## Context

See proposal.md. Today `ScanSession.outcome()` classifies all 54 raw readings at once
(`ColorClassifier.classify`, balanced so each colour gets nine) and returns `ScanOutcome(editor,
uncertain, validity)`; the raw readings are dropped. A confident, valid outcome goes to the solution;
otherwise `RubikkiNavHost` opens `ManualInputRoute(fromScan = true)` with the encoded cube and the
marks, and the pictures come through the in-memory `LastScanPictures.byFace`. The check is
`ManualInputScreen` with a picture beside the face, prev/next/check buttons and a top-bar "scan
again" that restarts the whole scan. `CubeCheck.validity` returns the first problem; only
`BadCentres`, `ImpossiblePiece` and `DuplicatePiece` carry stickers.

## Goals / Non-Goals

**Goals:** a check the user walks face by face; fixing one face by rescanning only it; a plain
verdict that points at faces (and, where it can, the two stickers) instead of cube theory.

**Non-Goals:** changes to plain manual input; searching for fixes beyond one swap of two stickers;
keeping the scan's readings across an app restart; a new look (that is `look-refresh`).

## Decisions

- **Keep the raw readings.** `ScanOutcome` gets `samples: List<Rgb>` (54, URFDLB). The app keeps the
  last scan in memory: `LastScanPictures` becomes `LastScan` with `pictures` and `samples`. Gone
  after a restart → "Scan this face again" is hidden; the whole-cube rescan stays.
- **Likely misreads (cube module).** New `MisreadSearch.swaps(cube, cost)`: for every pair of
  non-centre stickers with different colours, swap them and run `CubeCheck.validity`; keep the
  swaps that give `Valid`. Rank by the extra reading distance the swap costs:
  `d(i, ref[cj]) + d(j, ref[ci]) − d(i, ref[ci]) − d(j, ref[cj])`, where `ref` is the mean reading of
  each colour's stickers in the current colouring. Without readings (`cost = null`) the order is the
  sticker order. 48·47/2 = 1128 validity checks, well under 50 ms on a phone; run on
  `Dispatchers.Default` anyway. Alternative: three-cycles or single-sticker repaints — rejected: a
  balanced classification keeps nine of each colour, so the typical misread is exactly a swap, and
  a repaint alone can never fix a count.
- **Suspect faces.** Best swap → its two faces (the walk opens the first in face order). No valid
  swap → faces of `validity.markedStickers`; if that is empty (twist, flip, parity) → the two faces
  with the lowest mean confidence; plain manual input keeps today's detailed messages. The technical
  validity still goes to the log (`scan.check` line).
- **One face against this cube's colours.** `ColorClassifier.classifyFace(samples, labelled,
  ownCentre)`: references are the mean reading per colour over the other 45 stickers, labelled by
  the check's current colours (so the user's fixes count). Because the one-face scan starts the
  camera afresh with its own exposure, the rescan's readings are scaled by one gain: the face's
  centre brightness (r+g+b) in the original scan over now. (A Lab offset was tried first and failed
  the test: exposure acts as a gain, and a darker red then read as orange.) Each sticker takes the nearest reference
  (no balancing: the other faces are fixed, the counts show any surplus); confidence as in
  `classify`. Phone check item: whether the offset is enough under a different exposure.
- **Check state.** A small pure `ScanCheck` in the cube module holds the editor, the checked faces,
  the marks per face and the readings; operations `lookRight(face)`, `paint(index, color)`,
  `replaceFace(face, samples)`, `verdict()`. Unit-testable without Compose. The screen saves it with
  `rememberSaveable` (encoded string: colours + checked bitmask + marks); readings stay in `LastScan`.
- **Screen.** Same `ManualInputScreen` in check mode (`check: ScanCheck?`). Bottom bar in check mode:
  verdict/hint line, palette, then `[Scan this face again] [Looks right]` (outlined / filled);
  prev/next and Check are not shown — the walk and the face map replace them, and the verdict comes
  on the last "Looks right". The mini net shows a small check mark on checked faces; the note line
  says "N faces left to check". "Scan whole cube again" moves into the overflow menu (it is rarely
  wanted now and the top bar gets crowded). Last face with "Looks right" and a valid cube → solution.
- **One-face scan.** `ScanRoute(face: String? = null)`. `ScanSession(only = FaceView)`: index starts
  at that face, `accept()` ends the session (`isDone`), no previous-face check (the face before it
  was not just shown), redo disabled, progress shows one dot. Exposure lock as before (at capture).
  The result goes back through `LastScan.rescanned` (snapshot state, next to the pictures and
  readings it belongs with) and the check consumes it once. Changed from the planned
  `savedStateHandle`: the readings and pictures already live in `LastScan`, so one place is
  simpler. Alternative: a shared view model — rejected: one value back is all that is needed.
- **A scan invalid without any mark** (twist, flip, swapped pieces, nothing uncertain) starts with
  every face checked; the check gives its verdict at once on opening instead of waiting for a tap.
- **Texts (fi).** "Näyttää oikealta", "Kuvaa uudelleen" (the review's word for the same act; the longer text did not fit the half-width button), "Tarkistettavana vielä %d
  puolta", verdict "Tällaista kuutiota ei voi olla – jokin tarra on luettu väärin. Katso vielä:
  %s." (faces joined with "ja"), "Skannaa koko kuutio uudelleen" in the menu. English to match.

## Risks / Trade-offs

- Several swaps can be valid; the top one is a guess. → Name its faces, mark both stickers; the user
  still compares with the picture. Ties are rare with real readings.
- Exposure in the one-face scan differs from the original. → Centre offset; uncertain stickers get
  marked; if it misreads on the phone, a later change can lock exposure to stored values.
- The verdict appears only after all faces are checked, so a user who fixes a sticker early does not
  see it at once. → Intended: one clear moment of truth instead of messages while editing.
