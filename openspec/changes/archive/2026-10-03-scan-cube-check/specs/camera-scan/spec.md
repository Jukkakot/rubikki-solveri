## ADDED Requirements

### Requirement: No cube in the grid
A face SHALL only be captured automatically when the grid looks like cube stickers: at most one of
the nine cells may read as neither a clear colour nor a bright white. Otherwise the screen SHALL
say that no cube is seen in the grid and the hold progress SHALL not run. The capture button SHALL
still capture what is in the grid.

#### Scenario: Desk in view
- **WHEN** the camera shows a dark mouse pad or a grey desk in the grid and is held still
- **THEN** the screen says no cube is seen and nothing is captured

#### Scenario: Cube in view
- **WHEN** a cube face (including a solved white face) is held still in the grid
- **THEN** it is captured as before

#### Scenario: Capture anyway
- **WHEN** no cube is seen and the user taps the capture button
- **THEN** the grid is captured and shown for review
