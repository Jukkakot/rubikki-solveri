## MODIFIED Requirements

### Requirement: No cube in the grid
A face SHALL only be captured automatically when every one of the nine cells looks like a single
sticker: the cell's middle SHALL be one even colour, and that colour SHALL be a cube colour
(clearly coloured, or light and nearly grey for white); in addition the cells SHALL have the dark
gaps between stickers. Dark, grey, beige or brown cells, patterned cells and cells that straddle a
gap SHALL fail. Each cell of the grid SHALL show whether it looks like a sticker (a green outline
when it does). While any cell fails, the screen SHALL ask the user to bring the cube into the grid
and the hold progress SHALL NOT run. The capture button SHALL still capture what is in the grid.

#### Scenario: Room in view
- **WHEN** the camera shows a room, with or without the cube small in a corner, and is held still
- **THEN** the screen asks to bring the cube into the grid and nothing is captured

#### Scenario: Patterned cloth
- **WHEN** the grid shows a blanket whose middle is yellow and whose other cells are patterned or grey
- **THEN** nothing is captured, and the yellow middle cell shows green while the others do not

#### Scenario: Cube off the grid
- **WHEN** a cube face is held so that its bottom row is below the grid, or so close that the grid covers one or two stickers
- **THEN** the cells on gaps or outside the cube show that they are not stickers and nothing is captured

#### Scenario: Cube in view
- **WHEN** a cube face (including a white face) fills the grid, one sticker per cell, and is held still
- **THEN** all cells show green and the face is captured as before

#### Scenario: Capture anyway
- **WHEN** a cell fails and the user taps the capture button
- **THEN** the grid is captured and shown for review
