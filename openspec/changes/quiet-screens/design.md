# Design

## Context

Screens touched (`shared/src/commonMain/.../ui`):
- `scan/ScanScreen.kt` (606 lines): a top bar, the face marks, the camera, status texts and a bottom
  bar.
- `manual/ManualInputScreen.kt` (529 lines).
- `free/FreeCubeScreen.kt` (130 lines): text buttons per layer.
- `progress/TimerScreen.kt`: hint texts per timer phase.

The video scan's overlay controls (`scan-paint`: `BackButton`, `RoundIconToggle`, a ⋮
`DropdownMenu`, a status pill) are the model to follow.

## Goals / Non-Goals

**Goals:**
- The same look on both scans.
- Text that appears only while it teaches something.
- Icon buttons with full names as descriptions.

**Non-Goals:**
- Changing how the guided scan reads or captures.
- Changing the colour check's logic.
- Changing the timer's statistics.

## Decisions

1. **Shared scan overlay.** The video scan's top row (back, a slot in the middle, torch, menu) moves
   into a small composable (`ScanOverlayBar`) used by both scans:
   - The video scan puts its ring in the middle slot.
   - The guided scan puts its six face marks there.
   - *Alternative:* copying the row. Rejected, because the two scans should not drift apart.
2. **"Until first" state.** The hints shown only at first use the state that already exists, so no
   new setting is needed:
   - The guided scan's request: `faces done == 0`.
   - The check's line: a `rememberSaveable` flag set by the first paint, "Näyttää oikealta" or
     "Kuvaa uudelleen".
   - The timer's instruction: the timed-solve count from the progress repository, the same flow the
     history uses.
   - The free cube's drag hint: a flag set by the first drag on the cube (`CubeViewState` changes).
3. **Layer pictures.** Each free-cube layer button draws a 28 dp `Cube3D` of a solved cube with that
   layer highlighted and the move's arrow (`highlight` and `arrow` already exist for the guide),
   with dragging off. Its name stays the content description ("Oikea puoli", …).
   - *Alternative:* six vector icons. Rejected, because they would have to be drawn by hand and would
     not match the guide's arrows.
4. **Manual input buttons.** ‹ and › are `RoundIconButton`s (56 dp), and ✓ is a `BigButton` with a
   check icon and no text.
   - The existing content descriptions keep the old words, so tests and screen readers find them by
     name.

## Risks / Trade-offs

- [Six 3D cubes in buttons draw slowly on a weak phone.] → They are static, so each composes once
  and only redraws when the direction toggle changes.
- [A hint that disappears may be missed by someone who needs it again.] → The guided scan and the
  check are rarely used now (the video scan is the default). The timer's instruction comes back if
  the history is cleared.
