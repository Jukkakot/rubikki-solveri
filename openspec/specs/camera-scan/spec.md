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
While a face is in the grid, each cell SHALL show the colour it currently reads. If the centre
does not match the face asked for, the screen SHALL say which centre to turn towards the camera.

#### Scenario: Wrong face
- **WHEN** the front face is asked for and the camera sees a red centre
- **THEN** the screen says to turn the green centre towards the camera and does not capture

### Requirement: Capture
A face SHALL be captured automatically when the readings stay the same with the right centre for
about 1.5 seconds, with a short vibration, or at once with the capture button. While the right face
is held steady, a progress indicator SHALL show how much of the hold time has passed; any change
in the readings SHALL restart it. The previous face SHALL be re-scannable.

#### Scenario: Held still
- **WHEN** the right face is held still in the grid for the hold time
- **THEN** it is captured and shown for confirmation

#### Scenario: Still turning
- **WHEN** the right centre is seen but the other readings change within the hold time
- **THEN** nothing is captured and the progress starts again

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
After a capture the screen SHALL stop reading the camera and show the nine colours as read, with a
choice to accept them or scan the same face again. Only an accepted face SHALL count as done.

#### Scenario: Accept
- **WHEN** the front face has been captured and the user taps "Looks right"
- **THEN** the right face is asked for

#### Scenario: Scan again
- **WHEN** the front face has been captured and the user taps "Scan again"
- **THEN** the front face is asked for again and nothing is stored for it
