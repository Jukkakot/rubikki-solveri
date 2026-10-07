## MODIFIED Requirements

### Requirement: Colours on the camera picture
The camera picture SHALL show which stickers are still needed, not what each sticker was read as.
A sticker still needed SHALL be covered by a grey veil smaller than the sticker; a known sticker
SHALL be left bare, so the real cube shows. No read colour SHALL be painted over the picture.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture with some of its stickers still needed
- **THEN** those stickers show a grey veil and the known ones show nothing over them

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** no tile in the read colour is shown over it

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker still needed on a side of the real cube turned towards the camera SHALL be veiled in
grey, so the grey parts show what is left to show; known stickers SHALL be left bare. A side SHALL
get a white outline and a small tick at its centre only when the rest of the cube confirms all its
stickers, not on its own readings alone. When the cube's pose cannot be told, at least the faces
found SHALL be marked. A small vibration SHALL tell when new stickers become known.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers become known
- **THEN** their grey veils disappear, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show no veil but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's needed stickers are still veiled

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and both are turned towards the camera
- **THEN** both have an outline and a tick, and no row of side colours is shown under the picture

### Requirement: Paint follows the cube
The marks SHALL move smoothly with the real cube between readings, without jumps, and SHALL stay on
the cube when a reading misses its pose for a moment. While the cube moves quickly the marks SHALL
fade out, and come back once it has rested for a moment, so that no mark floats beside a moving
cube. When no face has been found for about a second, the marks SHALL fade out. They SHALL come back
as soon as the cube is seen again.

#### Scenario: Turning the cube
- **WHEN** the user turns the cube slowly in front of the camera
- **THEN** the marks glide with the stickers and do not flicker or jump between positions

#### Scenario: Moving quickly
- **WHEN** the user moves or turns the cube quickly
- **THEN** the marks fade out while it moves and appear again on the cube soon after it rests

#### Scenario: Pose missed for a moment
- **WHEN** a few pictures in a row give no pose while the cube stays in view
- **THEN** the marks stay on the cube

#### Scenario: Cube out of view
- **WHEN** the cube is taken out of the picture
- **THEN** the marks fade out within about a second, and appear again when the cube is back
