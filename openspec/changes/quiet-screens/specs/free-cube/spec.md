# Spec Delta

## Purpose

The free cube: a 3D cube the user turns freely by layer, to try moves, scramble it, and have it
solved from any state.

## ADDED Requirements

### Requirement: Turn layers with pictures
The free cube SHALL offer one button per face layer (up, down, right, left, front, back). Each SHALL
show a small cube picture with that layer highlighted and an arrow in the direction it will turn,
with the layer's name as its description. A ↻/↺ toggle SHALL choose clockwise or counter-clockwise
for all of them, and the pictures' arrows SHALL follow it. A tap SHALL animate the turn on the cube.

#### Scenario: Turn the right layer
- **WHEN** the user taps the right-layer button with clockwise chosen
- **THEN** the cube animates a clockwise turn of the right layer

#### Scenario: Other way round
- **WHEN** the user switches the toggle to counter-clockwise
- **THEN** every button's arrow points the other way and the next tap turns its layer counter-clockwise

### Requirement: Scramble, undo, reset and solve
The free cube SHALL offer scramble, undo and reset as icon buttons with their names as
descriptions, and "solve" as the one main button that opens the solution of the current cube.
Undo SHALL take back the last turn, and reset SHALL return to the cube the screen was opened with.

#### Scenario: Undo
- **WHEN** the user turns the right layer and taps undo
- **THEN** the cube is as it was before the turn

#### Scenario: Solve from here
- **WHEN** the user scrambles the cube and taps solve
- **THEN** the solution of that cube opens

### Requirement: Drag hint until the first drag
A hint that dragging turns the view SHALL be shown until the user first drags the cube on this
screen.

#### Scenario: Hint goes
- **WHEN** the user drags the cube once
- **THEN** the drag hint is no longer shown
