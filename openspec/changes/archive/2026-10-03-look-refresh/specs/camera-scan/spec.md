## ADDED Requirements

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
