## MODIFIED Requirements

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker not yet read on a side of the real cube turned towards the camera SHALL be veiled in
grey, so the grey parts show what is left to show; read stickers SHALL show only their small mark.
A side SHALL get a white outline and a small tick at its centre only when the rest of the cube
confirms all its stickers, not on its own readings alone. When the cube's pose cannot be told, at
least the faces found SHALL be marked. Sides not found in the picture SHALL be marked only when the
way the cube is tilted is sure (the face is seen straight on, another known face in the same
picture shows the tilt, or it follows a sure tilt of the pictures just before); otherwise only the
faces found are marked, so no mark lands beside the cube. A side not found SHALL not be marked where
a face found in the picture lies, so a face never gets two layers of marks. A small vibration SHALL
tell when new stickers become known and when a new side is read (a segment of the progress ring
lights).

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers are read
- **THEN** their grey veils give way to small marks, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show hollow rings and no veil, but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's stickers not read yet are still veiled

#### Scenario: Tilt not sure
- **WHEN** one face is seen at a slant, no other known face is in the picture and no sure tilt came just before
- **THEN** only that face is marked, and no veils are drawn beside it on the table

#### Scenario: Open face not marked twice
- **WHEN** a face is found whose side the scan cannot tell yet, where the cube's projection has a side
- **THEN** that face shows only its own marks, not the projection's veils as well

#### Scenario: New side read
- **WHEN** a face with the blue centre is read for the first time in the scan
- **THEN** the phone gives a short vibration and the blue segment of the ring lights

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and both are turned towards the camera
- **THEN** both have an outline and a tick, and no row of side colours is shown under the picture

### Requirement: Paint follows the cube
The marks SHALL move smoothly with the real cube between readings, without jumps, and SHALL stay on
the cube when a reading misses its pose for a moment. While the cube is held in the hand, with the
small movements that holding brings, the marks SHALL stay. Only while the cube moves clearly fast
(about a side width a second or more) SHALL the marks fade out, and come back once it has slowed for
a moment, so that no mark floats beside a moving cube. When no face has been found for about a
second, the marks SHALL fade out. They SHALL come back as soon as the cube is seen again.

In the browser, where the scan reads its pictures beside the page, the screen SHALL instead show
the very picture the scan read with that picture's marks, so the marks lie exactly on the cube also
while it moves: the picture then changes at the scan's rate and a little behind the live camera,
and the marks neither glide nor fade for movement. A picture in which no face was found SHALL not
replace the one shown for up to about 0.3 seconds, so the marks do not blink out for a few
pictures. Where the browser cannot read beside the page, the live picture and the rules above stay.

#### Scenario: Turning the cube
- **WHEN** the user turns the cube slowly in front of the camera
- **THEN** the marks glide with the stickers and do not flicker or jump between positions

#### Scenario: Held in the hand
- **WHEN** the user holds the cube in front of the camera with the small movements of a hand
- **THEN** the marks stay shown

#### Scenario: Moving quickly
- **WHEN** the user moves or turns the cube quickly, about a side width a second or faster, in the phone app
- **THEN** the marks fade out while it moves and appear again on the cube soon after it slows

#### Scenario: Pose missed for a moment
- **WHEN** a few pictures in a row give no pose while the cube stays in view
- **THEN** the marks stay on the cube

#### Scenario: Cube out of view
- **WHEN** the cube is taken out of the picture
- **THEN** the marks fade out within about a second, and appear again when the cube is back

#### Scenario: Browser marks on the moving cube
- **WHEN** the user turns the cube quickly in front of the camera in the browser
- **THEN** each picture shown has its marks on the cube's stickers, without lagging beside it

#### Scenario: Browser picture without a face
- **WHEN** in the browser two or three pictures in a row find no face while the cube is moved
- **THEN** the last picture with its marks stays shown instead of a picture without marks, and the next picture with a face replaces it
