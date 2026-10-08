## MODIFIED Requirements

### Requirement: Colours on the camera picture
The camera picture SHALL show both which stickers the camera has not read yet and what each read
sticker was read as, without covering the real cube. A sticker not read yet SHALL be covered by a
grey veil smaller than the sticker. A read sticker SHALL show a small dot in its read colour at its
centre, small enough that the real sticker shows around it, so a misread can be seen at a glance. A
sticker counts as read as soon as the camera has read its face steadily over a few pictures, before
the scan knows which face of the cube it is; a known sticker counts as read too.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture and some of its stickers have not been read yet
- **THEN** those stickers show a grey veil and the read ones show a small dot in their read colour

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** its dot shows the read colour, which differs from the real sticker around it

#### Scenario: Read before placed
- **WHEN** a face is held to the camera for about half a second while the scan cannot yet tell which face of the cube it is
- **THEN** its grey veils give way to dots in the colours read, without waiting for the scan to place it

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker not yet read on a side of the real cube turned towards the camera SHALL be veiled in
grey, so the grey parts show what is left to show; read stickers SHALL show only their small dot.
A side SHALL get a white outline and a small tick at its centre only when the rest of the cube
confirms all its stickers, not on its own readings alone. When the cube's pose cannot be told, at
least the faces found SHALL be marked. A small vibration SHALL tell when new stickers become known.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers are read
- **THEN** their grey veils give way to small dots, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show dots and no veil, but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's stickers not read yet are still veiled

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
and the marks neither glide nor fade for movement. Where the browser cannot read beside the page,
the live picture and the rules above stay.

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

### Requirement: Progress ring
A small ring at the top of the picture SHALL show the six sides as six segments, each in its
centre's colour, without numbers. A segment SHALL be faint while no face with that centre colour
has been read, lit once one has, and full once the rest of the cube confirms that side. The ring
SHALL be full in every segment when the scan finishes, also when the scan finishes with some
stickers never seen. A faint segment tells which side is still to show; no arrows are shown.

#### Scenario: Half known
- **WHEN** three sides have been read and none is confirmed yet
- **THEN** three segments are lit, not full, and the other three are faint

#### Scenario: One side still unread
- **WHEN** five sides have been read and the orange side has not been shown
- **THEN** five segments are lit and the orange one is faint

#### Scenario: Read but not confirmed
- **WHEN** the white side has been read but the rest of the cube does not confirm it yet
- **THEN** the white segment is lit but not full

#### Scenario: Finish before every sticker is seen
- **WHEN** the scan finishes with 50 stickers known
- **THEN** every segment of the ring is full

### Requirement: One status line
One short status line SHALL lie at the bottom of the picture. It SHALL ask to show the cube when
none is found, ask to show the grey parts while faces are still unread, ask to show the cube's
corners (three sides at once) once every side has been read but the cube is not yet clear, ask to
turn the cube while two faces could still be told apart either way (for about two seconds and
more), and say the scan is ready at the end. A stall notice SHALL take its place while shown.

#### Scenario: Status line
- **WHEN** a cube is in view and some sides have not been read yet
- **THEN** the line asks to show the grey parts

#### Scenario: Every side read
- **WHEN** all six sides have been read and the scan cannot yet tell the whole cube
- **THEN** the line asks to show the cube's corners

#### Scenario: No cube
- **WHEN** no face is found in the picture
- **THEN** the line asks to show the cube to the camera

#### Scenario: Turn the cube
- **WHEN** the readings fit two cubes about equally for about two seconds, because two faces could be told apart either way
- **THEN** the line asks to turn the cube, and it goes back as soon as a view settles it
