# Proposal

## Why

Every feature (3D view, manual input, solvers, camera scan, move guide) works on the same cube:
a state, moves in standard notation, and a check that a scanned or painted cube can exist. It has
to be right and heavily tested before any screen depends on it.

## What Changes

- Cube state as 54 stickers with the standard colour scheme (white/yellow, green/blue,
  red/orange) and the app's holding convention: white on top, green facing you.
- Moves: face turns (U D R L F B), slices (M E S), wide turns (Uw/u …) and whole-cube rotations
  (x y z), each clockwise, counter-clockwise (') or half (2).
- Notation: parse and print move sequences; inverse of a sequence; simplification (merging and
  cancelling consecutive turns of the same layer).
- Piece view (corners and edges with orientation) for solvers and for highlighting.
- Validity check with a specific reason: sticker counts, centres, impossible or duplicate pieces,
  twisted corner, flipped edge, swapped pieces (parity).
- Random scramble generator for testing and practice.

## Capabilities

### New Capabilities
- `cube-model`: the cube's state, moves and notation, and whether a given colouring is a solvable
  cube.

### Modified Capabilities
(none)

## Impact

- Module: `cube` only (pure Kotlin, JVM tests). No app changes yet; `cube-view` is the first user.
