# camera-follow Specification

## Purpose
Lets the user follow the solution with their eyes on the real cube: the phone's camera watches
the front face, shows the move on it and moves on when the move is done.

## Requirements

### Requirement: Camera mode
The solution screen SHALL offer a camera mode showing the live camera with the 3×3 grid, the
current move in words, a small 3D guide cube showing the move, and the usual previous, show and
done buttons. The user SHALL be told to keep the green centre towards the camera and white on top.

#### Scenario: Switch to camera mode
- **WHEN** the user turns on camera mode during a solution
- **THEN** the camera with the grid appears, with the current move and the small guide cube

### Requirement: Arrow on the real cube
The current move SHALL be drawn over the grid as seen on the front face: a top or bottom turn as
an arrow along that row, a left or right turn as an arrow along that column, a front turn as a
round arrow, each in the turning direction; a half turn SHALL be marked "2×". A move that cannot
be seen from the front SHALL have no arrow on the grid.

#### Scenario: Right turn
- **WHEN** the move is "turn the right side clockwise"
- **THEN** an upward arrow is drawn along the right column

#### Scenario: Top turn
- **WHEN** the move is "turn the top clockwise"
- **THEN** a leftward arrow is drawn along the top row

### Requirement: Detecting the move
When the front face's colours match the cube after the current move for a short stable moment,
the app SHALL advance to the next move with a vibration. When they match the result of a
different turn, it SHALL say what was done and which turn undoes it.

#### Scenario: Move done
- **WHEN** the user makes the shown move with the cube in the grid
- **THEN** the app advances to the next move by itself

#### Scenario: Wrong direction
- **WHEN** the user turns the right side the opposite way
- **THEN** the app says the turn went the wrong way and shows how to undo it

### Requirement: Moves not visible from the front
When the current move does not change the front face, the screen SHALL say that this move is
confirmed with the done button.

#### Scenario: Back turn
- **WHEN** the move is a turn of the back
- **THEN** the screen asks the user to tap done after turning

### Requirement: Self-calibration
While following, the colour reading SHALL adapt to the light using the stickers whose colours are
known from the cube.

#### Scenario: Warm light
- **WHEN** the light makes white read as yellowish
- **THEN** after a few frames of the known front face, white is read as white

### Requirement: Cube in the grid
Camera follow SHALL use the scan's sticker check: a frame in which not every grid cell looks like a
sticker SHALL be ignored (no advance, no wrong-move notice, no learning of colours), and the
screen SHALL ask the user to bring the cube into the grid.

#### Scenario: Cube out of the grid
- **WHEN** the user lowers the cube so that the grid shows the table
- **THEN** the app asks to bring the cube into the grid and does not advance or report a wrong move

#### Scenario: Back in the grid
- **WHEN** the cube's front face fills the grid again
- **THEN** following continues as before
