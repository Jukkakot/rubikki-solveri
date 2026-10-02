# Design

## Context

Pure Kotlin `cube` module (see `app-setup`). Consumers: the 3D view (needs sticker positions and
per-move animation info), the solvers (need a piece view), the scanner (validity with reasons).

## Goals / Non-Goals

**Goals:** one obviously-correct model, generated from geometry rather than hand-typed tables;
cheap enough for a UI (a move is a 54-entry permutation); exhaustive tests.

**Non-Goals:** search speed for solvers (the two-phase solver has its own tables; the beginner
solver will add what it needs), bigger cubes.

## Decisions

- **Sticker order is the common URFDLB facelet order** (U1…U9, R1…R9, F1…F9, D1…D9, L1…L9,
  B1…B9, each face read row by row in the standard net). The two-phase solver reads exactly this
  string, so no conversion bugs between model and solver.
- **Moves generated from 3D geometry.** Each sticker has a position (x right, y up, z front, each
  in −1..1) and a normal. A move is an axis, a set of layers and a quarter-turn count; rotating the
  matching stickers by −90° about the axis (clockwise seen from the axis tip) yields a 54-entry
  permutation, computed once per move and cached. Alternative: hand-written cycle tables — rejected,
  error-prone and 27 tables to check. Tests pin the geometry to known facts (turn direction, cycle
  orders, solver-compatible facelet tables).
- **State is immutable** (`Cube` wraps a colour array; `apply` returns a new one). 54 bytes per
  state is nothing for a UI, and immutability fits Compose.
- **Colours vs faces.** A `Cube` holds colours (what the user sees and scans). The piece view and
  the solver string translate colours to faces through the centres, so any holding orientation
  works.
- **Holding convention** white up, green front (product.md, decided by the app) is the colour
  scheme `ColorScheme.STANDARD` and the solved state.
- **Validity order** as in the spec: counts → centres → impossible piece → duplicate → twist →
  flip → parity. Earlier checks make later ones meaningful (the piece view needs real pieces).
  The result is a sealed type with sticker indices for highlighting.
- **Centres check** compares the cube's centre arrangement against the scheme: opposite pairs and
  handedness must match some rotation of the solved cube (24 orientations, tested by rotating the
  solved cube).
- **Scramble** uses a random face-turn sequence (no same face twice, no three on one axis); the
  random-state scramble from the two-phase solver comes in `fast-solve`.
- **Notation** accepts `'` and `’` for prime, `2'` as `2`, and both `Rw` and `r` (printed as
  written). Errors carry the token and its 1-based position.

## Risks / Trade-offs

- [Facelet geometry mistake] → tests cross-check the generated corner/edge positions against the
  published two-phase facelet tables and the solver's own expectations in `fast-solve`.
