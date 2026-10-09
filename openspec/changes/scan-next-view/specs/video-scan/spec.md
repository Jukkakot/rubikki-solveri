## ADDED Requirements

### Requirement: Next view chosen
The scan SHALL keep a choice of the next view to show: one of the cube's eight corners (three sides)
or one of its six sides, whichever would settle the most at that moment. That counts the stickers
not yet known, the stickers in doubt, and the sides whose turn is still open. The choice SHALL change
only when another view would settle clearly more, so it does not jump while the user turns the cube.
There SHALL be no choice once the cube is clear.

#### Scenario: Missing corner
- **WHEN** every side has been read and the only unknown stickers are those of the white, red and blue corner and the edges next to it
- **THEN** the next view is the white, red and blue corner

#### Scenario: Steady choice
- **WHEN** two corners would settle about the same and the user turns the cube a little
- **THEN** the chosen view stays the same

## MODIFIED Requirements

### Requirement: Progress ring
A small ring at the top of the picture SHALL show the six sides as six segments, each in its
centre's colour, without numbers. A segment SHALL be faint while no face with that centre colour
has been read. Once one has, it SHALL fill by the share of that side's nine stickers that are known,
and it SHALL be full once the rest of the cube confirms that side. Once every side has been read and
a next view is chosen, the segments of that view's sides SHALL pulse. The ring SHALL be full in every
segment when the scan finishes, also when the scan finishes with some stickers never seen. A faint
segment tells which side is still to show.

#### Scenario: Half known
- **WHEN** three sides have been read and none is confirmed yet
- **THEN** three segments are partly filled and the other three are faint

#### Scenario: Filling by stickers
- **WHEN** seven of the white side's stickers are known and the side is not confirmed
- **THEN** the white segment is filled about seven ninths

#### Scenario: One side still unread
- **WHEN** five sides have been read and the orange side has not been shown
- **THEN** five segments are filled at least in part and the orange one is faint

#### Scenario: Read but not confirmed
- **WHEN** the white side has been read but the rest of the cube does not confirm it yet
- **THEN** the white segment is not full

#### Scenario: Next view pulses
- **WHEN** every side has been read and the next view is the white, red and blue corner
- **THEN** the white, red and blue segments pulse

#### Scenario: Finish before every sticker is seen
- **WHEN** the scan finishes with 50 stickers known
- **THEN** every segment of the ring is full

### Requirement: One status line
One short status line SHALL lie at the bottom of the picture. It SHALL ask to show the cube when
none is found and ask to show the grey parts while sides are still unread. Once every side has been
read but the cube is not yet clear, it SHALL name the next view by its colours, as pulsing colour
dots after "Näytä kulma" (three dots) or "Näytä sivu" (one dot). At the end it SHALL say the scan is
ready. A stall notice SHALL take its place while shown.

#### Scenario: Status line
- **WHEN** a cube is in view and some sides have not been read yet
- **THEN** the line asks to show the grey parts

#### Scenario: Every side read
- **WHEN** all six sides have been read, the scan cannot yet tell the whole cube, and the next view is the white, red and blue corner
- **THEN** the line says "Näytä kulma" with a white, a red and a blue dot

#### Scenario: One side needed
- **WHEN** all six sides have been read and the next view is the yellow side
- **THEN** the line says "Näytä sivu" with a yellow dot

#### Scenario: No cube
- **WHEN** no face is found in the picture
- **THEN** the line asks to show the cube to the camera

#### Scenario: Turn the cube
- **WHEN** the readings fit two cubes about equally because two faces could be told apart either way
- **THEN** the line names the view that settles it with its colour dots, and goes back as soon as a view settles it

### Requirement: Turn shown on a small cube
When nothing new has been read for about two seconds while the scan is not finished, a small 3D
cube SHALL appear by the status line and show, as a short repeating movement, how to turn the real
cube. When a side is still unread and the scan knows how the cube is held, the small cube SHALL have
grey stickers except its six centres in their colours, start as the real cube is held, and turn so
that the unread side faces the camera. Once every side has been read, the small cube SHALL show the
known stickers in their colours and the unknown ones grey, start as the real cube is held (or, when
the scan does not know that, with one side facing the camera), and turn so that the next view faces
the camera, with the stickers still needed in that view blinking. The movement SHALL stay the same
while the view to show stays the same, and the small cube SHALL go away as soon as something new is
read. No arrow SHALL be drawn on the real cube.

#### Scenario: Unread side shown
- **WHEN** the orange side has not been read, the scan knows how the cube is held, and nothing new has been read for two seconds
- **THEN** a small cube with grey stickers and coloured centres appears by the status line, starting as the cube is held and turning its orange centre towards the camera, again and again

#### Scenario: Corners shown
- **WHEN** every side has been read, the next view is the white, red and blue corner and nothing new has been read for two seconds
- **THEN** the small cube shows the known stickers in colour, turns the white, red and blue corner towards the camera, and the stickers still needed there blink

#### Scenario: Gone on progress
- **WHEN** the small cube is shown and a new sticker or side is read
- **THEN** the small cube goes away
