## MODIFIED Requirements

### Requirement: Live reading
While a face is in the grid, each cell SHALL show a dot of the colour the camera currently sees
there, as seen, without deciding which cube colour it is. If the centre reads closer to another
cube colour than the asked face's, the screen SHALL say which colour the centre looks like and that
the user can hold still if this is the right side anyway; this SHALL NOT stop the capture. The
reading of the centre SHALL use the colours this cube has already shown (the accepted centres) and
a default palette for colours not seen yet.

#### Scenario: Wrong face
- **WHEN** the front face is asked for and the camera sees a red centre
- **THEN** the screen says the centre looks red and the user can hold still if this is the right side, and the face can still be captured as the front

#### Scenario: Warm red
- **WHEN** the cube's red reads closer to the default orange and the right face (red centre) is asked for
- **THEN** the face is captured, and once accepted, this cube's red reads as red

#### Scenario: Raw colours
- **WHEN** a cell of the grid sees a pinkish red
- **THEN** its dot shows that pinkish red, not a palette colour

### Requirement: Capture
A face SHALL be captured automatically when every cell's reading has stayed close to the same
colour for about 1.5 seconds, with a short vibration, or at once with the capture button. While the
face is held steady, a progress indicator SHALL show how much of the hold time has passed; a clear
change in any cell's reading SHALL restart it. While the camera still sees the face accepted last,
the screen SHALL ask the user to turn the cube and SHALL NOT capture. The previous face SHALL be
re-scannable.

#### Scenario: Held still
- **WHEN** the asked face is held still in the grid for the hold time
- **THEN** it is captured and shown for confirmation

#### Scenario: Still turning
- **WHEN** a face is seen but the readings change clearly within the hold time
- **THEN** nothing is captured and the progress starts again

#### Scenario: Previous face still in view
- **WHEN** the front face has been accepted and the camera still sees it
- **THEN** the screen asks to turn the cube, the right face stays asked for and nothing is captured

#### Scenario: Redo
- **WHEN** the user taps redo after confirming the right face
- **THEN** the right face is asked for again

### Requirement: Confirm each face
After a capture the screen SHALL stop reading the camera and show the nine colours as the camera
saw them, with a note that the colours are worked out at the end from the whole cube, and a choice
to go on to the next face or scan the same face again. If the captured centre read as another
colour than the asked face's, the review SHALL say so and that the user can go on if this is the
right side. Stickers in the review SHALL NOT be tappable. Only an accepted face SHALL count as done.

#### Scenario: Accept
- **WHEN** the front face has been captured and the user taps "Good, next"
- **THEN** the right face is asked for

#### Scenario: Scan again
- **WHEN** the front face has been captured and the user taps "Scan again"
- **THEN** the front face is asked for again and nothing is stored for it

#### Scenario: Centre note in the review
- **WHEN** the face captured as the front has a centre that read as red
- **THEN** the review says the centre read as red and that the user can go on if this is the right side

#### Scenario: Tap to fix
- **WHEN** the user taps a sticker in the review
- **THEN** nothing changes; doubtful stickers are checked in the manual editor after the scan
