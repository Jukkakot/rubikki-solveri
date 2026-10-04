# Design

## Context

- A cell's reading today is the per-channel median of the middle 40 % of the cell, sampled every
  second pixel (`FrameSampler.sample`, `CELL_MIDDLE`).
- During the scan each captured face is named by its centre: `ScanSession.recognise` ranks the
  colours by Lab distance (lightness weighted 0.5) to references that are the accepted centres plus
  the daylight `DEFAULT_PALETTE`, and takes the best colour among the faces still left. One wrong
  early name pushes the others: in the evening scan the dark blue centre `072641` (L 14) was 43 from
  default white and 50 from default blue, so blue became U; the white face later got the only name
  left (B). `scan.done` then reported `ImpossiblePiece(8, 9, 20)`.
- `ScanSession.outcome` classifies the 54 readings nine per colour, seeded by the centres as named
  (`ColorClassifier.classify`, Hungarian), then `RotationSearch.search` tries the 4⁶ face rotations
  and, if none is solvable, renames one opposite pair. A swap of two non-opposite centres (white ↔
  blue here) is not covered.
- A confident scan goes straight to the solution (`RubikkiNavHost`, `outcome.isConfident`);
  otherwise the colour check (`ManualInputScreen` with `fromScan`, pictures from `LastScan`) opens.
- Test data: `evidence/scan-log-2026-10-04.txt` holds the user's five scans of that day (two valid
  in daylight, three invalid: 10:47, 11:07 and the evening 18:30), with each face's nine readings.

## Goals / Non-Goals

**Goals:** the evening scan of the log comes out valid without the user's help; no daylight scan
of the log gets worse; the user always sees the result next to the pictures before solving.

**Non-Goals:** camera exposure or white-balance control; changing the live dots, the hold or the
capture rules; the browser crash after a long background (backlog).

## Decisions

### 1. Cell reading: trimmed mean of a bigger middle

`CELL_MIDDLE` 0.4 → 0.6, and the reading per channel is the mean of the middle half of the sorted
values (25th–75th percentile) instead of the median. Why: a bigger area averages out sensor noise in
dim light; trimming keeps a highlight or the edge of a gap from deciding it. 0.6 still keeps clear
of the gaps when the cube fills the grid (the gaps are about 10 % of a cell on each side). The
sticker check (`FrameSampler.check`, gap contrast) is unchanged.

### 2. Brightness-independent comparison for naming centres

A new comparison scales a reading so its brightest channel is 255 (gain capped at 6×, so noise in
near-black is not blown up) before Lab, and the references are scaled the same way. It is used for
naming centres (live recognition and decision 3), not for the nine-per-colour grouping, which keeps
today's Lab distance against the cube's own centres (it already works within one lighting). With
this, `072641` scales to `1c98ff` and is plainly blue; the evening white `7f7459` scales to `fff2b2`
and stays nearer white than yellow (`ffd900`). Alternative considered: hue angle only. Rejected:
white has no reliable hue, and red/orange differ mainly in it, so a pure hue test is brittle.

### 3. Centres named together at the end

`outcome()` takes the six captured faces' centre readings and solves the 6 × 6 assignment of colours
to centres (cost = decision 2 distance to the scaled default palette; Hungarian, as in classify). The
captured faces are renamed by this naming (their readings move with them; `RotationSearch` finds the
turns as before, so where a face sat on the cube need not be known). The live name is kept when it
ties.

### 4. Next-best namings when the cube is impossible

When the naming of decision 3 gives no solvable cube (after `RotationSearch`, including its
opposite-pair rename), the namings are tried in order of total cost, at most the 12 best of the
720, and the first solvable one is used. If none is, the decision-3 result is kept (today's
behaviour: most real pieces, marked in the check). Why 12: each try is one classification and one
rotation search; the bound keeps the worst case well under a second on the phone's browser (to be
measured in a test). Silent: the check shows the cube as read; nothing says a face was renamed. The
log's `scan.done` gets the naming used (`renamed=U>B,B>U`) for diagnosis.

### 5. The check after every scan, continuing by itself when confident

`onResult` always opens the colour check. For a confident scan (user, 2026-10-04) it gets no marks, a
short note (`check_note_ok`: "Vertaa kuviin. Jatketaan ratkaisuun…" / "Compare with the pictures.
Going on to the solution…") and continues to the solution after **5 seconds**; the time left shows
as the "Näyttää oikealta" button filling up. "Skannaa koko kuutio uudelleen" is shown as an
outlined button next to it. Any touch on the check (a sticker, a colour, scrolling, a button) stops
the automatic continue; the button then works as today. An unsure or invalid scan never continues by
itself. The pictures in `LastScan` follow the renamed faces (the outcome's `from` already maps faces
for the opposite-pair rename; decision 3 extends that map).

### 7. No face names while scanning

Since the names are decided at the end (user, 2026-10-04), the full scan shows none: the live
"Keskiö näyttää: X" status, the review's "Tunnistettu: X" and its "Väärä puoli? Napauta…" choice of
another face, and the face name in "X luettu" (becomes "Puoli luettu") go away, as does the grid's
hint colour for the recognised centre. The done-face marks are filled with the centre as seen
(raw reading) instead of the named colour. Live recognition stays inside `ScanSession` (decision 2)
as the provisional slot of each face and for the references; decision 3 corrects it. The
single-face rescan (`only`) keeps naming its face, since that face is known. Strings
`scan_status_centre`, `scan_review_face`, `scan_review_pick` and `scan_captured` are removed or
replaced.

### 6. Tests from the log

A test fixture parses the evidence file's `scan.face` lines (face, nine readings) per scan and runs
`ScanSession`'s outcome on them. Checks: the evening scan is valid; the two daylight scans give the
same cube as logged; the 10:47 and 11:07 scans are reported either way (no expectation on validity
beyond not throwing, since the real cube is unknown), and their result is noted in the summary. The
live recognition test: `072641` with no faces done names the blue face.

### Implementation decisions (autopilot)

- **Namings by enumeration:** all 720 namings are costed and sorted (cheap), instead of Hungarian
  plus a k-best search; the best one is what Hungarian would give. A tie keeps the live name.
- **Pair rename only for the best naming:** renaming an opposite pair is itself another naming, so
  namings 2–12 try rotations only. Worst case on the JVM: 1.4 s → 0.5 s (the log's impossible
  10:47 and 11:07 scans); a valid scan stays under 0.15 s.
- **Confident check's bar:** "Skannaa koko kuutio uudelleen" + "Näyttää oikealta" for the whole
  life of the screen, also after a touch stopped the countdown; no per-face rescan there. Why: the
  touch that stops the countdown must not change the button under the finger. Stickers can still be
  painted, and the menu keeps "scan the whole cube again".
- **Countdown look:** a translucent fill sweeping across "Näyttää oikealta"; it clears when stopped.
- **One-face rescan:** keeps its face name in the review and its centre ring on the grid.
- **`ScanSession.choose`** stays (no UI uses it) so tests can replay the log's live names.
- **Log results:** the evening scan is valid and confident with the new live naming already; with
  the log's wrong live names it is renamed `B>U,U>B` and gives the same cube. 10:47 and 11:07 stay
  impossible: their U and D faces have near-black and brownish cells that are not stickers, not a
  naming problem.

## Risks / Trade-offs

- [A bigger area catches a gap when the cube is held small in the grid] → the trimmed mean drops the
  darkest quarter; `scan-needs-cube` already refuses frames where cells are not one sticker.
- [Scaled comparison confuses red and orange in the dark] → only centre naming uses it, and the joint
  naming plus next-best tries resolve a red/orange mix-up through solvability.
- [An extra screen for confident scans] → one tap more; the user asked for the glance.
