# cube-view Specification

## Purpose
The on-screen 3D cube used across the app: it must look like the real cube, show every move
clearly as an animation, and let the user turn it to look at any side.

## Requirements

### Requirement: 3D cube display
The app SHALL draw the cube in 3D with perspective, showing three faces at rest (top, front and
right in the default view), with the real sticker colours separated by dark gaps. Stickers not yet
known SHALL be drawn grey.

#### Scenario: Default view
- **WHEN** a solved cube is shown in the default view
- **THEN** the white top, green front and red right faces are visible and nothing else

#### Scenario: Unknown sticker
- **WHEN** a sticker has no colour yet
- **THEN** it is drawn grey

### Requirement: Move animation
Every move SHALL be shown as a smooth turn of the moving layers (or the whole cube for a
rotation) about the right axis in the right direction. A quarter turn SHALL take about 0.3 s. A
half turn SHALL play as two quarter steps in the same direction, each like a quarter turn, with a
pause of about 0.25 s between them, so it is plain that the layer turns twice. Moves requested
during an animation SHALL queue and play in order. When the phone's animations are switched off,
moves SHALL apply at once.

#### Scenario: Animated turn
- **WHEN** the move R is played
- **THEN** only the right layer turns, away from the viewer at the top, and the cube ends in the state after R

#### Scenario: Half turn in two steps
- **WHEN** the move R2 is played
- **THEN** the right layer turns a quarter, stops briefly, turns another quarter the same way, and the cube ends in the state after R2

#### Scenario: Queued moves
- **WHEN** three moves are requested quickly
- **THEN** they play one after another and the cube ends in the state after all three

### Requirement: Turning the view
Dragging on the cube SHALL rotate the view freely in the drag's direction without changing the
cube's state.

#### Scenario: Drag to look at the back
- **WHEN** the user drags horizontally across the cube by about its width
- **THEN** the view turns roughly half way round and the back face comes into view

### Requirement: Sticker picking and highlighting
A tap on the cube SHALL identify the sticker under the finger on the visible side. The display
SHALL be able to mark a set of stickers so that they stand out.

#### Scenario: Tap a sticker
- **WHEN** the user taps the middle of the front face
- **THEN** the front centre sticker is identified

#### Scenario: Marked stickers
- **WHEN** stickers are marked
- **THEN** they are drawn with a strong outline
