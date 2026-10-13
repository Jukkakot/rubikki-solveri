# Design

## Context

- `BeginnerSolver.solve` builds each step from macros found by `MacroSearch`: whole-cube turns (`y`, the `z2`
  turn-over, a first `x`/`z` to bring white up), a setup turn of U or D, then an algorithm (`TRIGGER`,
  `MIDDLE_RIGHT/LEFT`, `YELLOW_CROSS`, `SWAP_EDGES`, `PLACE_CORNERS`) repeated n times; up to 2–3 macros chained in
  one step. The white cross uses a free search (`CrossSearch.place`). A `Step` keeps only the flat move list and a
  `StepNote` (which piece).
- The learning mode (`SolveMethod.LEARN`) walks the flat moves with the move guide; the view never turns by itself
  and a whole-cube turn is shown as the cube turning (`move-guide` *Steady view*).
- Lessons have a page per algorithm with a 3D demo (`lessons` *Algorithm demo*).

## Goals / Non-Goals

**Goals:** each step of the user's own solve shown as which piece goes where with which combination and how many
times; the combination animated with layer turns only.

**Non-goals:** a new method or new algorithms; checking the real cube (no scan between steps); changing the
learning mode or the lessons.

## Decisions

1. **Steps carry parts.** `Step` gains `parts: List<StepPart>` with `Hold(moves)` (whole-cube turns),
   `Setup(moves)` (U/D turns before a combination), `Combo(algorithm, times)` and `Free(moves)` (the cross, the final
   turn). `MacroSearch` returns the macros it chose, so the parts are exact; the step's `moves` stay their
   concatenation, so the other modes are unchanged. Tests: parts concatenate to the old moves on many scrambles.
2. **A card per part group.** A step shows one card: piece and target (from `StepNote` and the cube after the
   step), then its parts in order: a hold card ("Käännä kuutiota: sininen eteen", with the turn shown once as the
   cube turning, as the learning mode does), a setup line ("Käännä alakerrosta kerran oikealle"), the combination
   with × n. A step with two chained macros shows both in order.
3. **Combination animation without whole-cube turns.** "Näytä" plays only the combination's layer moves (× n) from
   the cube as it is after the hold and setup; the view stays put. Hold changes are never inside it (decision 2),
   so the user's request holds by construction.
4. **Pieces highlighted.** The step's piece is outlined on the cube where it is now, and its target place where it
   will be (from the cube after the step), as the lessons outline the pieces an algorithm moves.
5. **Lesson link.** Each combination links to its lesson page (the lesson for its stage, the algorithm's page).
6. **Name:** "Opettele liikesarjat" / "Learn the combinations"; the combination itself is "liikesarja" in Finnish
   texts.

## Implementation decisions (autopilot)

7. **Every part has "Näytä"**, not only the combination: hold and setup play their own turns the same way, so the
   user can watch each part; the combination's play still holds no whole-cube turn (decision 3).
8. **Piece and place share one outline:** the piece's stickers where it is now and its slot (between the centres of
   its colours) are both outlined with the existing mark style; when the piece is home they coincide. Yellow-layer
   steps outline nothing (they move a whole layer).
9. **Start screen label "Liikesarjat" / "Combinations"** (a third segment beside Nopein/Opettele); the step counter
   is "Askel n/m" so it does not clash with "Vaihe n/7" of the stages.
10. **No camera follow in this mode** (the ⋮ menu hides it); history records the solve as "Liikesarjat".
11. **"Lue lisää" opens the stage's lesson on the algorithm's page** (`LessonRoute` gained `page`).

## Risks / Trade-offs

- [Two chained macros make a step long] → shown as two combination parts in one card, each with its own "Näytä".
- [Hold changes every few steps (the `y` turns for corners and middle edges)] → kept from the method; each is one
  short card. Fewer turns could come later by preferring macros without `y`.

## Open Questions

None blocking; the name and wording can be changed after the first try on the phone.
