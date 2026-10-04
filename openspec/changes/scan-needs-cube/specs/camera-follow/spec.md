## ADDED Requirements

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
