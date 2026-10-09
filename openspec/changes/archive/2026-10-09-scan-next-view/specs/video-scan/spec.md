## ADDED Requirements

### Requirement: Corner row
During the whole video scan a row of eight small corner pictures SHALL lie above the status line,
one for each corner of the cube, each drawn as a cube corner in the colours of the three centres
that meet there. A corner SHALL count as read once its three stickers are known. A read corner SHALL
dim and get a tick, keeping its place, so the row shows both what is left and what is done. The
corner worth showing next SHALL pulse. When the scan finishes, every corner SHALL be read.

#### Scenario: From the start
- **WHEN** the video scan opens and nothing has been read yet
- **THEN** eight corner pictures are shown in their colours, none dimmed

#### Scenario: Corner read
- **WHEN** the three stickers of the white, red and green corner become known
- **THEN** that corner picture dims and gets a tick, and the others keep their places

#### Scenario: Next corner pulses
- **WHEN** three corners are still unread and the white, red and blue one would settle the most
- **THEN** the white, red and blue corner picture pulses

#### Scenario: Finished
- **WHEN** the scan finishes, also with some stickers never seen
- **THEN** every corner picture is dimmed with a tick

### Requirement: Next corner chosen
The scan SHALL keep a choice of the corner worth showing next among the unread corners: the corner
whose view (its three sides) would settle the most, counting the stickers not yet known, those in
doubt and the sides whose turn is still open. The choice SHALL change only when another corner would
settle clearly more, so it does not jump while the user turns the cube. There SHALL be no choice
once every corner is read.

#### Scenario: Missing corner
- **WHEN** the only unknown stickers are those of the white, red and blue corner and the edges next to it
- **THEN** the next corner is the white, red and blue one

#### Scenario: Steady choice
- **WHEN** two corners would settle about the same and the user turns the cube a little
- **THEN** the chosen corner stays the same

## REMOVED Requirements

### Requirement: Progress ring
**Reason**: Replaced by the corner row (user, mockups 2026-10-09: one measure is enough, and the corners say what to show and what is done).
**Migration**: The corner row takes the ring's place in the overlay; side progress now shows as corners read.

## MODIFIED Requirements

### Requirement: One status line
One short status line SHALL lie at the bottom of the picture. It SHALL ask to show the cube when
none is found, ask to show the grey parts while sides are still unread, and once every side has been
read but the cube is not yet clear, say how many corners are left ("Vielä 3 kulmaa"). At the end it
SHALL say the scan is ready. A stall notice SHALL take its place while shown.

#### Scenario: Status line
- **WHEN** a cube is in view and some sides have not been read yet
- **THEN** the line asks to show the grey parts

#### Scenario: Every side read
- **WHEN** all six sides have been read, the scan cannot yet tell the whole cube and three corners are unread
- **THEN** the line says three corners are left

#### Scenario: No cube
- **WHEN** no face is found in the picture
- **THEN** the line asks to show the cube to the camera

#### Scenario: Turn the cube
- **WHEN** the readings fit two cubes about equally because two faces could be told apart either way
- **THEN** a corner touching those faces stays unread and pulses as the next corner until a view settles it

### Requirement: Turn shown on a small cube
When nothing new has been read for about two seconds while the scan is not finished, a small 3D
cube SHALL appear by the status line and show, as a short repeating movement, how to turn the real
cube. When a side is still unread and the scan knows how the cube is held, the small cube SHALL have
grey stickers except its six centres in their colours, start as the real cube is held, and turn so
that the unread side faces the camera. Once every side has been read, the small cube SHALL show the
known stickers in their colours and the unknown ones grey, start as the real cube is held (or, when
the scan does not know that, with one side facing the camera), and turn so that the next corner
faces the camera, with the stickers still needed there blinking. The movement SHALL stay the same
while the corner or side to show stays the same, and the small cube SHALL go away as soon as
something new is read. No arrow SHALL be drawn on the real cube.

#### Scenario: Unread side shown
- **WHEN** the orange side has not been read, the scan knows how the cube is held, and nothing new has been read for two seconds
- **THEN** a small cube with grey stickers and coloured centres appears by the status line, starting as the cube is held and turning its orange centre towards the camera, again and again

#### Scenario: Corners shown
- **WHEN** every side has been read, the next corner is the white, red and blue one and nothing new has been read for two seconds
- **THEN** the small cube shows the known stickers in colour, turns the white, red and blue corner towards the camera, and the stickers still needed there blink

#### Scenario: Gone on progress
- **WHEN** the small cube is shown and a new sticker or side is read
- **THEN** the small cube goes away

### Requirement: Recognised by agreement
The scan SHALL keep track of the cubes that are still possible and narrow them with every
observation, using only facts that hold for every real cube: each colour on nine stickers, each
centre's colour fixed with its opposite (white–yellow, green–blue, red–orange), only real corner and
edge pieces, each piece once, the corners' twist, the edges' flip and the parity of a real cube.
Which face and turn a reading shows SHALL be among the possibilities, not decided before them by
how its centre looks: faces found in one picture SHALL be different faces that are neighbours on
the cube (never opposite colours), touching along the edges the picture shows them touching, which
also fixes their turns and the order of the colours round a corner; a face followed from picture to
picture SHALL stay the same face; how a centre looks SHALL count only as evidence, never as a rule.
A sticker SHALL count as known only when every cube still clearly possible has the same colour there,
whether from several agreeing frames or from the rest of the cube. A single wrong frame SHALL NOT
change a known sticker or finish the scan; readings that disagree with each other SHALL weigh
against each other and recent clear readings SHALL count over old ones, so that a face read wrong at
first is put right by later clear views. Red and orange readings SHALL count as weaker evidence
against each other than other colours. Renaming or re-turning a face SHALL NOT forget what its
stickers were read as. The scan's view of which face and turn each reading shows SHALL change
only when something new speaks for it (a new reading, or a reading growing old): while nothing new
is read it SHALL stay the same, and it SHALL NOT go back and forth between two views picture after
picture.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

#### Scenario: Impossible piece
- **WHEN** a sticker reads yellow next to a white sticker of the same piece
- **THEN** the reading counts only for the colours a real piece allows there, and yellow is never shown for it

#### Scenario: Dark centre taken for another colour
- **WHEN** the white face and the blue face are in view together and the blue face's centre, in shadow, reads closer to white
- **THEN** the white face is read from the white face only, and the blue face's readings count for the blue face

#### Scenario: Doubtful centre
- **WHEN** a face is seen whose centre fits white and blue about equally, and no other view settles which it is
- **THEN** its stickers are not shown as known until a view settles it

#### Scenario: Orange face first
- **WHEN** the scan starts with the orange face in view, its centre fitting red and orange about equally, and the red face is shown only later
- **THEN** no sticker of the red side is shown wrong at any time, and once the red face is seen both are named right

#### Scenario: Red centre looks orange
- **WHEN** the white, yellow, green, blue and orange faces are known and the red face, whose centre looks more orange than red, is shown several times
- **THEN** the red face is told red, its stickers become known and the scan can finish

#### Scenario: Look-alike centres
- **WHEN** the red centre looks orange in this light, and the red and orange faces are each shown, never together
- **THEN** the two faces are told apart by the faces seen around them, and the scan finishes with the true cube

#### Scenario: Corner decides red or orange
- **WHEN** a corner with the white and green faces is in view and its third centre could be red or orange by its look
- **THEN** the third face is named by which way round the three faces run, and the reading's stickers count for that face

#### Scenario: Neighbours are never opposite
- **WHEN** two faces are found side by side whose centres look white and pale yellow
- **THEN** they are never taken as the white and the yellow face together

#### Scenario: Patterned cube
- **WHEN** a pattern makes one face, turned, look like another face (a striped cube)
- **THEN** the faces are still told apart and the scan finishes with the true cube

#### Scenario: Blue face first, white face later
- **WHEN** the scan starts with the blue face on top, its centre reading nearer white, and the white face is shown only later
- **THEN** no sticker is shown wrong at any time, and once the white face is seen the blue face is named blue

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

#### Scenario: Rename keeps the stickers
- **WHEN** a face's name or turn changes as more is seen
- **THEN** the stickers whose readings still agree stay known, and the number of known stickers does not drop

#### Scenario: Nothing new read
- **WHEN** the cube is out of view, or a face has left the picture, and no new reading arrives
- **THEN** the shown stickers, the corner row and the faces' names and turns stay as they are

#### Scenario: Held still
- **WHEN** the cube is held still in view for a few seconds
- **THEN** no sticker switches back and forth between two colours picture after picture

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Marks SHALL be drawn only on the faces found in the picture, never on sides of the cube the camera
has not found, so no mark lands beside or above the cube. A face SHALL get marks only once it has
been followed from an earlier picture; a lattice found in one picture alone (for example in a
blurred picture while the cube moves quickly) SHALL get no marks at all. On a face that gets marks,
every sticker not yet read SHALL be veiled in grey; read stickers SHALL show only their small mark.
A face read steadily (over a few pictures) SHALL get a thin outline; a face found in one picture
only gets none. A face SHALL get a white outline and a small tick at its centre only when the rest
of the cube confirms all its stickers, not on its own readings alone. Which corners are still to show
is told by the corner row and the turn demo, not by marks on the cube. A small vibration SHALL
tell when new stickers become known and when a new side is read.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers are read
- **THEN** their grey veils give way to small marks, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show hollow rings and no veil, but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side gets no marks, and its corners in the corner row stay undone until it is read

#### Scenario: Tilt not sure
- **WHEN** one face is seen at a slant and no other face is in the picture
- **THEN** only that face is marked, and nothing is drawn beside it

#### Scenario: Open face not marked twice
- **WHEN** a face is found whose side the scan cannot tell yet
- **THEN** that face shows only its own marks

#### Scenario: A stray lattice
- **WHEN** for one picture a lattice is found across the edge of the cube or beside it
- **THEN** it gets no veils, marks or outline

#### Scenario: Turned quickly
- **WHEN** the cube is turned quickly so that faces are found in blurred pictures at places no earlier picture had them
- **THEN** no grey veils appear beside or above the cube, and the marks come back on faces followed again

#### Scenario: New side read
- **WHEN** a face with the blue centre is read for the first time in the scan
- **THEN** the phone gives a short vibration

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and both are found in the picture
- **THEN** both have an outline and a tick, and no row of side colours is shown under the picture

### Requirement: Marks can be hidden
Settings SHALL offer to hide the scan's marks on the camera picture, so a screen recording of a scan
shows the cube as the camera saw it. With it on, the video scan SHALL draw no veils, rings, dots,
outlines or ticks on the camera picture; the corner row, the status line, the turn demo and the
vibrations SHALL stay. It SHALL be off by default and remembered across starts. It SHALL change only
what is drawn, never what the scan reads or decides.

#### Scenario: Clean screen recording
- **WHEN** the user turns on hiding the marks and scans the cube
- **THEN** the camera picture shows no marks, while the corner row fills and the scan finishes as usual

#### Scenario: Default
- **WHEN** the app is used without changing the setting
- **THEN** the marks are shown
