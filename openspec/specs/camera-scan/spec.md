# camera-scan Specification

## Purpose
Reads the colours of the user's real cube with the phone's camera, face by face, and hands a
complete, checked cube to the solver or, when unsure, to the manual editor.

## Requirements

### Requirement: Guided scan
The scan screen SHALL show the camera preview with a 3×3 grid, the face to show (front, right,
back, left, top, bottom) with how to hold the cube, and which faces are done.

#### Scenario: First face
- **WHEN** the scan starts
- **THEN** it asks for the front face: green centre towards the user, white on top

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

### Requirement: Classification by the centres
After all faces, each sticker SHALL get the colour of the centre it is closest to, such that every
colour is used exactly nine times, and a confidence. The six centres SHALL keep their colours.

#### Scenario: Different lighting
- **WHEN** a scrambled cube is scanned under warm indoor light that shifts all colours
- **THEN** every sticker gets its right colour

#### Scenario: Doubtful sticker
- **WHEN** a sticker's reading is about equally close to two colours
- **THEN** it is marked uncertain

### Requirement: Result
A valid scan where no sticker is uncertain SHALL open the solution directly. Otherwise the manual
editor SHALL open with the scanned colours, the uncertain or problem stickers marked, and a note
asking the user to check them.

#### Scenario: Confident scan
- **WHEN** the scan is valid and confident
- **THEN** the solution opens

#### Scenario: Unsure scan
- **WHEN** the scan has uncertain stickers or is invalid
- **THEN** the manual editor opens with those stickers marked

### Requirement: Camera permission
Without camera permission the screen SHALL explain why the camera is needed, offer to ask for it
again and offer manual input instead.

#### Scenario: Permission denied
- **WHEN** the user denies the camera permission
- **THEN** the screen explains it and offers manual input

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

### Requirement: Steady camera settings
After the first face is accepted, the camera's exposure and white balance SHALL stay fixed for the
rest of the scan, so that every face is read under the same settings.

#### Scenario: Lock after the first face
- **WHEN** the front face is accepted
- **THEN** exposure and white balance are locked until the scan ends or returns to the front face

### Requirement: One screen
In portrait the scan screen SHALL fit the display without scrolling. The actions (capture and redo
while scanning; scan again and go on during the review) SHALL stay visible at the bottom, and the
status line, the hold progress and the review texts SHALL be shown on the camera view, not between
it and the actions.

#### Scenario: Review on a phone
- **WHEN** a face has been captured on a phone in portrait
- **THEN** "Good, next" and "Scan again" are visible without scrolling, and the review texts are on the dimmed camera view

#### Scenario: Scanning on a phone
- **WHEN** a face is being held in the grid
- **THEN** the status and the progress are shown on the camera view and the capture button is visible without scrolling

### Requirement: No cube in the grid
A face SHALL only be captured automatically when the grid looks like cube stickers: at most one of
the nine cells may read as neither a clear colour nor a bright white. Otherwise the screen SHALL
say that no cube is seen in the grid and the hold progress SHALL not run. The capture button SHALL
still capture what is in the grid.

#### Scenario: Desk in view
- **WHEN** the camera shows a dark mouse pad or a grey desk in the grid and is held still
- **THEN** the screen says no cube is seen and nothing is captured

#### Scenario: Cube in view
- **WHEN** a cube face (including a solved white face) is held still in the grid
- **THEN** it is captured as before

#### Scenario: Capture anyway
- **WHEN** no cube is seen and the user taps the capture button
- **THEN** the grid is captured and shown for review
