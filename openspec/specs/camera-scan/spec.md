# camera-scan Specification

## Purpose
Reads the colours of the user's real cube with the phone's camera, face by face, and hands a
complete, checked cube to the solver or, when unsure, to the manual editor.

## Requirements

### Requirement: Guided scan
The scan screen SHALL show the camera preview with a 3×3 grid and which faces are done. Any face not
yet scanned SHALL be accepted, in any order and turned any way. The screen SHALL NOT suggest an
order or a way to hold the cube; it SHALL ask for any face not yet scanned, held any way round.

#### Scenario: First face
- **WHEN** the scan starts
- **THEN** it asks for any face of the cube, held any way round, and names no particular face

#### Scenario: Another face first
- **WHEN** the scan starts and the user shows the top face turned a quarter
- **THEN** it is captured and recognised as the top face

### Requirement: Live reading
While a face is in the grid, each cell SHALL show a dot of the colour the camera currently sees
there, as seen, without deciding which cube colour it is. The screen SHALL say which face the centre
looks like, among the faces not yet scanned; this SHALL NOT stop the capture. The reading of the
centre SHALL use the colours this cube has already shown (the accepted centres) and a default
palette for colours not seen yet.

#### Scenario: Wrong face
- **WHEN** no face is done yet and the camera sees a red centre
- **THEN** the screen says the centre looks like the right face, and the face can be captured

#### Scenario: Warm red
- **WHEN** the cube's red reads closer to the default orange and the right face (red centre) is shown
- **THEN** the face can be captured and its recognised face changed to the right face in the review, and once accepted, this cube's red reads as red

#### Scenario: Raw colours
- **WHEN** a cell of the grid sees a pinkish red
- **THEN** its dot shows that pinkish red, not a palette colour

### Requirement: Capture
A face SHALL be captured automatically when every cell's reading has stayed close to the same
colour for about 1.5 seconds, with a short vibration, or at once with the capture button. While the
face is held steady, a progress indicator SHALL show how much of the hold time has passed; a clear
change in any cell's reading SHALL restart it. While the camera sees a face already accepted, in any
rotation, the screen SHALL ask the user to turn to a face not yet scanned and SHALL NOT capture. The
face accepted last SHALL be re-scannable.

#### Scenario: Held still
- **WHEN** a face not yet scanned is held still in the grid for the hold time
- **THEN** it is captured and shown for confirmation

#### Scenario: Still turning
- **WHEN** a face is seen but the readings change clearly within the hold time
- **THEN** nothing is captured and the progress starts again

#### Scenario: Previous face still in view
- **WHEN** the front face has been accepted and the camera sees it again, turned a quarter
- **THEN** the screen asks to turn to another face and nothing is captured

#### Scenario: Redo
- **WHEN** the user taps redo after accepting the right face
- **THEN** the right face is no longer done and can be scanned again

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
After the six faces, the scan SHALL find how each face was turned, on the assumption that the real
cube is solvable: of all rotations of the six faces, the one that gives a solvable cube SHALL be
used. When none does, the one with the most stickers forming real pieces SHALL be used, and if the
colours of two opposite centres were read the wrong way round, swapping them SHALL be tried too.
When rotations giving different solvable cubes exist, the faces whose rotation differs SHALL be
marked. A valid scan where no sticker is uncertain or marked SHALL open the solution directly.
Otherwise the check SHALL open with the scanned colours, the uncertain or problem stickers marked,
and a note asking the user to check them.

#### Scenario: Confident scan
- **WHEN** the scan is valid and confident
- **THEN** the solution opens

#### Scenario: Faces turned
- **WHEN** a scrambled cube is scanned with the top face turned a quarter and the back face upside down
- **THEN** the solution opens for the real cube

#### Scenario: Unsure scan
- **WHEN** the scan has uncertain stickers or is invalid
- **THEN** the check opens with those stickers marked

### Requirement: Camera permission
Without camera permission the screen SHALL explain why the camera is needed, offer to ask for it
again and offer manual input instead.

#### Scenario: Permission denied
- **WHEN** the user denies the camera permission
- **THEN** the screen explains it and offers manual input

### Requirement: Confirm each face
After a capture the screen SHALL stop reading the camera and show the nine colours as the camera
saw them, which face it was recognised as, with a note that the colours are worked out at the end
from the whole cube, and a choice to go on or scan again. The user SHALL be able to change the
recognised face to any face not yet scanned by tapping its centre colour. Stickers in the review
SHALL NOT be tappable. Only an accepted face SHALL count as done.

#### Scenario: Accept
- **WHEN** a face recognised as the front has been captured and the user taps "Good, next"
- **THEN** the front face counts as done and any face not yet scanned is asked for

#### Scenario: Centre note in the review
- **WHEN** a capture was recognised as the left face (orange) and the user taps the red centre colour
- **THEN** the review says right face, and on "Good, next" the right face counts as done

#### Scenario: Scan again
- **WHEN** a face has been captured and the user taps "Scan again"
- **THEN** nothing is stored for it and the camera is read again

#### Scenario: Tap to fix
- **WHEN** the user taps a sticker in the review
- **THEN** nothing changes; doubtful stickers are checked after the scan

### Requirement: Steady camera settings
When the first face is captured, the camera's exposure and white balance SHALL be locked for the
rest of the scan, so that every face is read under the settings the camera had while the first face
was held still. Going back to no face done (scanning the first face again) SHALL release the lock
until a face is captured again.

#### Scenario: Lock after the first face
- **WHEN** the first face is captured, whichever face it is
- **THEN** exposure and white balance are locked until the scan ends or no face is done again

#### Scenario: Turning after the first face
- **WHEN** the first face has been captured and the user turns the cube towards a darker view before accepting
- **THEN** the next faces are read with the first face's exposure, not a brighter one

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
A face SHALL only be captured automatically when every one of the nine cells looks like a single
sticker: the cell's middle SHALL be one even colour, and that colour SHALL be a cube colour
(clearly coloured, or light and nearly grey for white); in addition the cells SHALL have the dark
gaps between stickers. Dark, grey, beige or brown cells, patterned cells and cells that straddle a
gap SHALL fail. Each cell of the grid SHALL show whether it looks like a sticker (a green outline
when it does). While any cell fails, the screen SHALL ask the user to bring the cube into the grid
and the hold progress SHALL NOT run. The capture button SHALL still capture what is in the grid.

#### Scenario: Desk in view
- **WHEN** the camera shows a dark mouse pad or a grey desk in the grid and is held still
- **THEN** the screen asks to bring the cube into the grid and nothing is captured

#### Scenario: Room in view
- **WHEN** the camera shows a room, with or without the cube small in a corner, and is held still
- **THEN** the screen asks to bring the cube into the grid and nothing is captured

#### Scenario: Patterned cloth
- **WHEN** the grid shows a blanket whose middle is yellow and whose other cells are patterned or grey
- **THEN** nothing is captured, and the yellow middle cell shows green while the others do not

#### Scenario: Cube off the grid
- **WHEN** a cube face is held so that its bottom row is below the grid, or so close that the grid covers one or two stickers
- **THEN** the cells on gaps or outside the cube show that they are not stickers and nothing is captured

#### Scenario: Cube in view
- **WHEN** a cube face (including a white face) fills the grid, one sticker per cell, and is held still
- **THEN** all cells show green and the face is captured as before

#### Scenario: Capture anyway
- **WHEN** a cell fails and the user taps the capture button
- **THEN** the grid is captured and shown for review

### Requirement: Smooth capture
The camera view SHALL keep moving through a capture and the review; saving the capture's picture
SHALL not hold up the screen. A stop of the camera frames or of the screen's drawing long enough to
notice SHALL be written to the log with where it happened and how long it lasted.

#### Scenario: Capture without a freeze
- **WHEN** a face is captured
- **THEN** the camera view does not freeze while the picture is saved

#### Scenario: Stall logged
- **WHEN** no camera frame arrives for a noticeable time during the scan
- **THEN** the log gets a stall line with its length

### Requirement: Scan one face
The scan SHALL be able to ask for a single face, started from the check of a scan. It SHALL name that
face and its centre colour, capture and confirm it as in the full scan, accept it turned any way,
and on "Good, next" return the face's readings and its picture to the check instead of asking for
another face. The face progress SHALL show only that face, and the action to scan the previous face
again SHALL not be offered.

#### Scenario: One face only
- **WHEN** the one-face scan is opened for the top face and the user accepts a capture
- **THEN** the check is shown again with the top face's new reading and no other face is asked for

#### Scenario: Turned in the rescan
- **WHEN** the top face is rescanned turned a quarter
- **THEN** the check shows the top face's colours the right way round

#### Scenario: Scan the same face again
- **WHEN** the top face has been captured in the one-face scan and the user taps "Scan again"
- **THEN** the top face is asked for again

### Requirement: Scan look
The scan screens (camera, camera permission and colour check) SHALL always be dark, also when the
app is light, so the camera picture and the sticker colours stand out. While scanning, the
capture action SHALL be a large round shutter button. Which faces are done SHALL be shown as six
marks: a done face filled with its centre colour, the face being scanned marked as current, the
rest empty.

#### Scenario: Light app, dark scan
- **WHEN** the app is in light mode and the user opens the scan
- **THEN** the scan screen is dark, and the app is light again after leaving the scan

#### Scenario: Faces done
- **WHEN** the white and green faces have been accepted
- **THEN** two marks are filled white and green, the next mark shows it is current, and three are empty

#### Scenario: Shutter
- **WHEN** a face is in the grid while scanning
- **THEN** a large round shutter button captures it at once
