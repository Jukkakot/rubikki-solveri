## MODIFIED Requirements

### Requirement: Paint follows the cube
The marks SHALL move smoothly with the real cube between readings, without jumps, and SHALL stay on
the cube when a reading misses its pose for a moment. While the cube is held in the hand, with the
small movements that holding brings, the marks SHALL stay. Only while the cube moves clearly fast
(about a side width a second or more) SHALL the marks fade out, and come back once it has slowed for
a moment, so that no mark floats beside a moving cube. When no face has been found for about a
second, the marks SHALL fade out. They SHALL come back as soon as the cube is seen again.

In the phone app, and in the browser where the scan reads its pictures beside the page, the screen
SHALL instead show the very picture the scan read with that picture's marks, so the marks lie
exactly on the cube also while it moves: the picture then changes at the scan's rate and a little
behind the live camera, and the marks neither glide nor fade for movement. A picture in which no
face was found SHALL not replace the one shown for up to about 0.3 seconds, so the marks do not
blink out for a few pictures. Until the first picture is read, and where the browser cannot read
beside the page, the live picture and the rules above stay.

#### Scenario: Turning the cube
- **WHEN** the user turns the cube slowly in front of the camera
- **THEN** the marks glide with the stickers and do not flicker or jump between positions

#### Scenario: Held in the hand
- **WHEN** the user holds the cube in front of the camera with the small movements of a hand
- **THEN** the marks stay shown

#### Scenario: Moving quickly
- **WHEN** the user moves or turns the cube quickly, about a side width a second or faster, where the live picture is shown
- **THEN** the marks fade out while it moves and appear again on the cube soon after it slows

#### Scenario: Pose missed for a moment
- **WHEN** a few pictures in a row give no pose while the cube stays in view
- **THEN** the marks stay on the cube

#### Scenario: Cube out of view
- **WHEN** the cube is taken out of the picture
- **THEN** the marks fade out within about a second, and appear again when the cube is back

#### Scenario: Marks on the moving cube
- **WHEN** the user turns the cube quickly in front of the camera, in the phone app or in the browser
- **THEN** each picture shown has its marks on the cube's stickers, without lagging beside it

#### Scenario: Picture without a face
- **WHEN** two or three pictures in a row find no face while the cube is moved
- **THEN** the last picture with its marks stays shown instead of a picture without marks, and the next picture with a face replaces it
