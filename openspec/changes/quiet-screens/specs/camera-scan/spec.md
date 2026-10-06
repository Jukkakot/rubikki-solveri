# Spec Delta

## MODIFIED Requirements

### Requirement: Guided scan
The scan screen SHALL show the camera preview with a 3×3 grid and which faces are done. Any face not
yet scanned SHALL be accepted, in any order and turned any way. The screen SHALL NOT suggest an
order or a way to hold the cube; until the first face is captured it SHALL ask for any face, held
any way round, and after that only the status line SHALL remain.

#### Scenario: First face
- **WHEN** the scan starts
- **THEN** it asks for any face of the cube, held any way round, and names no particular face

#### Scenario: Another face first
- **WHEN** the scan starts and the user shows the top face turned a quarter
- **THEN** it is captured, and at the end the top face's colours are in their place

#### Scenario: Hint gone after the first face
- **WHEN** the first face has been captured and accepted
- **THEN** the "any face, any way" request is no longer shown

### Requirement: One screen
In portrait the scan screen SHALL fit the display without scrolling. The camera SHALL fill the
screen; back, the torch (where the device has one) and a menu (the video scan, entering colours by
hand) SHALL be round icons on the picture, with the six face marks along its top edge and no title.
The shutter and a redo icon SHALL stay at the bottom, and the status line, the hold progress and the
review texts SHALL be shown on the camera view.

#### Scenario: Review on a phone
- **WHEN** a face has been captured on a phone in portrait
- **THEN** "Good, next" and "Scan again" are visible without scrolling, and the review texts are on the dimmed camera view

#### Scenario: Scanning on a phone
- **WHEN** a face is being held in the grid
- **THEN** the status and the progress are shown on the camera view and the capture button is visible without scrolling

#### Scenario: Controls on the picture
- **WHEN** the guided scan opens
- **THEN** the camera fills the screen with back, torch and menu icons on it and the six face marks along its top, and no title is shown

### Requirement: Scan look
The scan screens (camera, camera permission and colour check) SHALL always be dark, also when the
app is light, so the camera picture and the sticker colours stand out. While scanning, the
capture action SHALL be a large round shutter button. Which faces are done SHALL be shown as six
marks: a done face filled with its centre as the camera saw it, the face being scanned marked as
current, the rest empty. The marks SHALL be the only count of faces done; no "n/6" text SHALL be
shown.

#### Scenario: Light app, dark scan
- **WHEN** the app is in light mode and the user opens the scan
- **THEN** the scan screen is dark, and the app is light again after leaving the scan

#### Scenario: Faces done
- **WHEN** two faces have been accepted
- **THEN** two marks are filled with those faces' centres as seen, the next mark shows it is current, three are empty, and no "2/6" text is shown

#### Scenario: Shutter
- **WHEN** a face is in the grid while scanning
- **THEN** a large round shutter button captures it at once
