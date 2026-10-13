# Proposal

## Why

The user (2026-10-13) wants a way to learn the move combinations a first-time solver needs: solve their own cube
in the learning mode's seven stages, but with each step shown as "this piece goes there with this combination",
not as single moves to copy. The learning mode shows one move at a time and never says which combination it is
or what it does; the lessons teach the combinations, but on example cubes, not the user's own. This replaces the
earlier `solve-challenge` proposal (user 2026-10-13).

## What Changes

- **A third way on the start screen: "Opettele liikesarjat" / "Learn the combinations"**, beside the shortest
  solution and learning step by step. The cube is solved in the beginner method's seven stages, as in learning.
- **A step is a card:** the piece (its colours) highlighted on the 3D cube and its target place outlined, a
  sentence ("Valko-puna-sininen kulma tähän paikkaan"), and the combination with its count (`R' D' R D` × 3).
  Before the combination, the turns of the top or bottom that bring the piece over its place are said and shown
  as their own short part.
- **"Näytä"** animates the combination on the cube; **siirto kerrallaan** steps through it move by move with the
  move guide; **"Tein sen"** goes to the next step.
- **White cross:** no combination there; its card shows the edge and the few turns that bring it up into place.
- **No whole-cube turn inside a combination's animation** (user 2026-10-13): where the method changes how the cube
  is held (white on top → yellow on top, a side to the front), that is its own instruction card before the
  combination; the combination's animation turns layers only and the view never turns by itself.
- A link from a combination to its lesson page ("Lue lisää").

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `beginner-solver`: a new requirement *Learn the combinations*; each step tells its parts (hold change, setup
  turns, combination and count, free turns).

## Impact

`cube` (`BeginnerSolver`: steps carry their parts instead of a flat move list; the moves stay the same), `shared`
(start screen option, the combination card screen reusing the 3D cube, move guide and lesson links), texts in
Finnish and English. `app` and `web` unchanged beyond the shared screens.
