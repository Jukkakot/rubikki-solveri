## MODIFIED Requirements

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Marks SHALL be drawn only on the faces found in the picture, never on sides of the cube the camera
has not found, so no mark lands beside or above the cube. On a face found, every sticker not yet
read SHALL be veiled in grey; read stickers SHALL show only their small mark. A face read steadily
(over a few pictures) SHALL get a thin outline; a face found in one picture only gets none. A face
SHALL get a white outline and a small tick at its centre only when the rest of the cube confirms
all its stickers, not on its own readings alone. Which sides are still to show is told by the
progress ring and the turn demo, not by marks on the cube. A small vibration SHALL tell when new
stickers become known and when a new side is read (a segment of the progress ring lights).

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers are read
- **THEN** their grey veils give way to small marks, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show hollow rings and no veil, but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side gets no marks, and its ring segment stays faint until it is read

#### Scenario: Tilt not sure
- **WHEN** one face is seen at a slant and no other face is in the picture
- **THEN** only that face is marked, and nothing is drawn beside it

#### Scenario: Open face not marked twice
- **WHEN** a face is found whose side the scan cannot tell yet
- **THEN** that face shows only its own marks

#### Scenario: A stray lattice
- **WHEN** for one picture a lattice is found across the edge of the cube or beside it
- **THEN** it gets no outline, and its marks go with the next picture

#### Scenario: New side read
- **WHEN** a face with the blue centre is read for the first time in the scan
- **THEN** the phone gives a short vibration and the blue segment of the ring lights

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and both are found in the picture
- **THEN** both have an outline and a tick, and no row of side colours is shown under the picture
