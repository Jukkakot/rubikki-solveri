## MODIFIED Requirements

### Requirement: Recognised by agreement
A sticker SHALL count as known only when several frames agree on its colour, or when the rest of
the cube leaves only one possible colour for it. A single wrong frame SHALL NOT change a known
sticker or finish the scan. Red and orange readings SHALL count as weaker evidence against each
other than other colours. Faces SHALL be told apart by how their centres look on this cube in this
light, compared with each other, not only against fixed reference colours: two faces whose centres
look clearly different SHALL never be taken for the same face, even when both are nearer the same
reference colour and are never in view together. The faces seen so far SHALL be named together,
each colour once, and when five have been seen the sixth SHALL follow. When five faces are named
surely, a sixth face seen several times SHALL be named with the colour left, even when its centre
looks more like one of the five; it SHALL NOT wait as unnamed. While a face's centre fits two
colours about equally and the naming cannot tell them apart yet, its stickers SHALL NOT be shown as
known. Two faces found in the same picture SHALL never be taken for the same face: when their
centres name the same colour, the one that fits it better keeps it and the other takes its
next-best colour if it fits nearly as well. Renaming a face SHALL NOT forget what its stickers were
read as: stickers whose readings still agree stay known. Recent readings SHALL count over old ones:
when the readings of a face seen for a while agree with each other and outnumber what was read
before, they SHALL replace known stickers, so that a face read wrong at first is put right by later
clear views.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

#### Scenario: Dark centre taken for another colour
- **WHEN** the white face and the blue face are in view together and the blue face's centre, in shadow, reads closer to white
- **THEN** the white face is read from the white face only, and the blue face's readings count for the blue face

#### Scenario: Blue face first, white face later
- **WHEN** the scan starts with the blue face on top, its centre reading nearer white, and the white face is shown only later
- **THEN** the two faces' readings are never mixed, and once the white face is seen the blue face is named blue

#### Scenario: Doubtful centre
- **WHEN** a face is seen whose centre fits white and blue about equally, and no other face settles which it is
- **THEN** its stickers are not shown as known until the naming is clear

#### Scenario: Orange face first
- **WHEN** the scan starts with the orange face in view, its centre fitting red and orange about equally, and the red face is shown only later
- **THEN** no sticker of the red side is shown wrong at any time, and once the red face is seen both are named right

#### Scenario: Red centre looks orange
- **WHEN** the white, yellow, green, blue and orange faces are named surely and the red face, whose centre looks more orange than red, is shown several times
- **THEN** the red face is named red, its stickers become known and the scan can finish

#### Scenario: Rename keeps the stickers
- **WHEN** a face whose stickers are known is renamed
- **THEN** the stickers whose readings still agree stay known, and the number of known stickers does not drop

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

### Requirement: Colours on the camera picture
The camera picture SHALL show both which stickers are still needed and what each known sticker was
read as, without covering the real cube. A sticker still needed SHALL be covered by a grey veil
smaller than the sticker. A known sticker SHALL show a small dot in its read colour at its centre,
small enough that the real sticker shows around it, so a misread can be seen at a glance.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture with some of its stickers still needed
- **THEN** those stickers show a grey veil and the known ones show a small dot in their read colour

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** its dot shows the read colour, which differs from the real sticker around it

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker still needed on a side of the real cube turned towards the camera SHALL be veiled in
grey, so the grey parts show what is left to show; known stickers SHALL show only their small dot.
A side SHALL get a white outline and a small tick at its centre only when the rest of the cube
confirms all its stickers, not on its own readings alone. When the cube's pose cannot be told, at
least the faces found SHALL be marked. A small vibration SHALL tell when new stickers become known.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers become known
- **THEN** their grey veils give way to small dots, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show dots and no veil, but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's needed stickers are still veiled

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

#### Scenario: Turning the cube
- **WHEN** the user turns the cube slowly in front of the camera
- **THEN** the marks glide with the stickers and do not flicker or jump between positions

#### Scenario: Held in the hand
- **WHEN** the user holds the cube in front of the camera with the small movements of a hand
- **THEN** the marks stay shown

#### Scenario: Moving quickly
- **WHEN** the user moves or turns the cube quickly, about a side width a second or faster
- **THEN** the marks fade out while it moves and appear again on the cube soon after it slows

#### Scenario: Pose missed for a moment
- **WHEN** a few pictures in a row give no pose while the cube stays in view
- **THEN** the marks stay on the cube

#### Scenario: Cube out of view
- **WHEN** the cube is taken out of the picture
- **THEN** the marks fade out within about a second, and appear again when the cube is back
